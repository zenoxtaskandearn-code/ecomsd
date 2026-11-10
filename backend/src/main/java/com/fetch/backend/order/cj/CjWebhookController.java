package com.fetch.backend.order.cj;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fetch.backend.catalog.cj.CjDropshippingClient;
import com.fetch.backend.order.entity.Order;
import com.fetch.backend.order.repo.OrderRepository;
import com.fetch.backend.payment.entity.WebhookEvent;
import com.fetch.backend.payment.repo.WebhookEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/webhooks/cj")
public class CjWebhookController {
    private final WebhookEventRepository webhookEventRepository;
    private final OrderRepository orderRepository;
    private final CjFulfillmentService cjFulfillmentService;
    private final ObjectMapper mapper;
    private final CjDropshippingClient cjClient;
    private final String webhookSecret;

    public CjWebhookController(WebhookEventRepository webhookEventRepository, OrderRepository orderRepository,
                               CjFulfillmentService cjFulfillmentService, ObjectMapper mapper,
                               CjDropshippingClient cjClient,
                               @Value("${CJ_WEBHOOK_SECRET:}") String webhookSecret) {
        this.webhookEventRepository = webhookEventRepository;
        this.orderRepository = orderRepository;
        this.cjFulfillmentService = cjFulfillmentService;
        this.mapper = mapper;
        this.cjClient = cjClient;
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
    }

    @PostMapping
    public ResponseEntity<Object> handle(@RequestHeader Map<String, String> headers,
                                         @RequestBody(required = false) String rawBody) {
        // CJ requires 200 OK within 3 seconds. Process best-effort/idempotently. No CJ retry on failure.
        String body = rawBody == null ? "{}" : rawBody;
        String sign = header(headers, "sign");
        if (sign != null && !sign.isBlank() && !verifySignature(sign, body)) {
            return ResponseEntity.status(401).body(Map.of("code", 401, "result", "fail", "message", "invalid signature"));
        }
        try {
            JsonNode root = mapper.readTree(body);
            JsonNode params = root.path("params");
            if (params.isMissingNode() || params.isNull()) params = root;

            String type = firstText(root, "type", "topic");
            String messageType = firstText(root, "messageType");
            String eventType = (type == null ? "" : type) + (messageType == null ? "" : (":" + messageType));

            // CJ payload fields vary by topic (ORDER/LOGISTIC). Extract merchant + CJ order ids and tracking.
            String merchantOrder = firstText(params, "orderNumber", "orderNum", "merchantOrderNumber", "shopOrderNumber");
            String cjOrderId = firstText(params, "cjOrderId");
            if (cjOrderId == null) {
                // LOGISTIC uses params.orderId for CJ order id
                String oid = firstText(params, "orderId");
                if (oid != null && looksLikeCjOrderId(oid, merchantOrder)) cjOrderId = oid;
            }
            // Some topics may include storeOrderNumbers array (LOGISTIC)
            if (merchantOrder == null) merchantOrder = firstStoreOrderNumber(params);
            if (merchantOrder == null) merchantOrder = firstText(root, "orderNumber", "orderNum", "merchantOrderNumber", "shopOrderNumber");
            if (cjOrderId == null) cjOrderId = firstText(root, "cjOrderId", "orderId");

            String tracking = firstText(params, "trackingNumber", "trackNumber", "logisticNo");
            if (tracking == null) tracking = firstText(root, "trackingNumber", "trackNumber", "logisticNo");

            String eventId = firstText(root, "messageId", "eventId", "id", "logId");
            if (eventId == null || eventId.isBlank()) eventId = sha256((eventType == null ? "" : eventType) + "|" + (cjOrderId == null ? "" : cjOrderId) + "|" + (merchantOrder == null ? "" : merchantOrder) + "|" + (tracking == null ? "" : tracking) + "|" + body);
            if (webhookEventRepository.findByProviderAndEventId("CJ", eventId).isPresent()) {
                return ResponseEntity.ok(Map.of("code", 200, "result", "success", "message", "duplicate"));
            }
            WebhookEvent ev = new WebhookEvent();
            ev.setProvider("CJ");
            ev.setEventId(eventId.length() > 128 ? eventId.substring(0, 128) : eventId);
            ev.setEventType(eventType);
            ev.setPayloadHash(sha256(body));
            ev.setProcessStatus("RECEIVED");
            Optional<Order> orderOpt = findOrder(cjOrderId, merchantOrder, params, root);
            orderOpt.ifPresent(o -> ev.setOrderId(o.getId()));
            try {
                webhookEventRepository.saveAndFlush(ev);
            } catch (DataIntegrityViolationException dup) {
                return ResponseEntity.ok(Map.of("code", 200, "result", "success", "message", "duplicate"));
            }
            try {
                if (orderOpt.isPresent()) {
                    Order order = orderOpt.get();
                    if (cjOrderId != null && !cjOrderId.isBlank() && (order.getCjOrderId() == null || order.getCjOrderId().isBlank())) {
                        order.setCjOrderId(cjOrderId.trim());
                        orderRepository.save(order);
                    }
                    cjFulfillmentService.syncOrderFromCj(order.getOrderNumber());
                    ev.setProcessStatus("PROCESSED");
                } else {
                    ev.setProcessStatus("IGNORED");
                    ev.setErrorMessage("No matching local order found for CJ webhook (" + (type == null ? "unknown" : type) + ").");
                }
                ev.setProcessedAt(Instant.now());
                webhookEventRepository.save(ev);
            } catch (Exception e) {
                ev.setProcessStatus("FAILED");
                ev.setErrorMessage(safe(e.getMessage()));
                ev.setProcessedAt(Instant.now());
                try { webhookEventRepository.save(ev); } catch (Exception ignored) {}
            }
        } catch (Exception e) {
            // always acknowledge to avoid retries; nothing to persist safely
        }
        return ResponseEntity.ok(Map.of("code", 200, "result", "success", "message", "ok"));
    }

    private boolean verifySignature(String signHeader, String rawBody) {
        // Official CJ: HMAC-SHA256(secret, rawBody) Base64 == sign header. Secret is openId (or explicit override).
        String secret = !webhookSecret.isBlank() ? webhookSecret : cjClient.getOpenId();
        if (secret == null || secret.isBlank()) return true; // cannot verify without openId/secret; accept (idempotent processing)
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = Base64.getEncoder().encodeToString(mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8)));
            return constantTimeEquals(expected, signHeader.trim());
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] x = a.getBytes(StandardCharsets.UTF_8);
        byte[] y = b.getBytes(StandardCharsets.UTF_8);
        if (x.length != y.length) return false;
        int r = 0; for (int i = 0; i < x.length; i++) r |= x[i] ^ y[i];
        return r == 0;
    }

    private Optional<Order> findOrder(String cjOrderId, String merchantOrder, JsonNode params, JsonNode root) {
        if (merchantOrder != null && !merchantOrder.isBlank()) {
            Optional<Order> o = orderRepository.findByOrderNumber(merchantOrder.trim());
            if (o.isPresent()) return o;
        }
        if (cjOrderId != null && !cjOrderId.isBlank()) {
            Optional<Order> o = orderRepository.findByCjOrderId(cjOrderId.trim());
            if (o.isPresent()) return o;
        }
        // storeOrderNumbers array (LOGISTIC)
        JsonNode arr = params == null ? null : params.path("storeOrderNumbers");
        if (arr != null && arr.isArray()) {
            for (JsonNode n : arr) { if (n.isTextual()) { Optional<Order> o = orderRepository.findByOrderNumber(n.asText().trim()); if (o.isPresent()) return o; } }
        }
        // sometimes nested in data
        JsonNode data = root.path("data");
        if (data != null && !data.isMissingNode() && !data.isNull()) {
            String c = firstText(data, "cjOrderId", "orderId");
            String m = firstText(data, "orderNumber", "orderNum", "merchantOrderNumber");
            if (m != null) { Optional<Order> o = orderRepository.findByOrderNumber(m.trim()); if (o.isPresent()) return o; }
            if (c != null) { Optional<Order> o = orderRepository.findByCjOrderId(c.trim()); if (o.isPresent()) return o; }
        }
        return Optional.empty();
    }

    private static String firstStoreOrderNumber(JsonNode params) {
        if (params == null) return null;
        JsonNode arr = params.path("storeOrderNumbers");
        if (arr != null && arr.isArray()) {
            for (JsonNode n : arr) if (n.isTextual() && !n.asText().isBlank()) return n.asText().trim();
        }
        return null;
    }
    private static boolean looksLikeCjOrderId(String id, String merchantOrder) {
        if (id == null || id.isBlank()) return false;
        if (merchantOrder != null && id.equalsIgnoreCase(merchantOrder)) return false;
        // CJ order ids are typically long numeric strings or specific prefixes; merchant order in our app is like FT-xxxxxx
        return !id.toUpperCase().startsWith("FT-");
    }

    private static String header(Map<String, String> headers, String... names) {
        if (headers == null) return null;
        for (String n : names) {
            for (var e : headers.entrySet()) if (e.getKey() != null && e.getKey().equalsIgnoreCase(n)) return e.getValue();
        }
        return null;
    }
    private static String firstText(JsonNode n, String... fields) {
        if (n == null) return null;
        for (String f : fields) { JsonNode v = n.path(f); if (!v.isMissingNode() && !v.isNull()) { String s = v.asText(); if (s != null && !s.isBlank() && !"null".equalsIgnoreCase(s)) return s; } }
        return null;
    }
    private static String sha256(String s) {
        try { MessageDigest md = MessageDigest.getInstance("SHA-256"); return HexFormat.of().formatHex(md.digest((s == null ? "" : s).getBytes(StandardCharsets.UTF_8))); }
        catch (Exception e) { return String.valueOf((s == null ? 0 : s.hashCode())); }
    }
    private static String safe(String s) { if (s == null) return null; return s.length() > 2000 ? s.substring(0, 2000) : s; }
}
