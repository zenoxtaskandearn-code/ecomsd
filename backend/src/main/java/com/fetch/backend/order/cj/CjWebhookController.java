package com.fetch.backend.order.cj;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fetch.backend.order.entity.Order;
import com.fetch.backend.order.repo.OrderRepository;
import com.fetch.backend.payment.entity.WebhookEvent;
import com.fetch.backend.payment.repo.WebhookEventRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
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
    private final String webhookSecret;

    public CjWebhookController(WebhookEventRepository webhookEventRepository, OrderRepository orderRepository,
                               CjFulfillmentService cjFulfillmentService, ObjectMapper mapper,
                               @Value("${CJ_WEBHOOK_SECRET:}") String webhookSecret) {
        this.webhookEventRepository = webhookEventRepository;
        this.orderRepository = orderRepository;
        this.cjFulfillmentService = cjFulfillmentService;
        this.mapper = mapper;
        this.webhookSecret = webhookSecret == null ? "" : webhookSecret.trim();
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> handle(@RequestHeader Map<String, String> headers,
                                                      @RequestBody(required = false) String rawBody) {
        // Acknowledge quickly; process best-effort/idempotently (CJ retries on non-2xx).
        String body = rawBody == null ? "{}" : rawBody;
        if (!webhookSecret.isBlank()) {
            String provided = header(headers, "x-cj-sign", "cj-sign", "x-cj-token", "x-webhook-secret", "authorization");
            if (provided == null || !webhookSecret.equals(provided.replace("Bearer ", "").trim())) {
                // don't leak details; still 200? no, reject unauthorized config attempts but keep generic
                return ResponseEntity.status(401).body(Map.of("status", "unauthorized"));
            }
        }
        try {
            JsonNode root = mapper.readTree(body);
            String eventType = firstText(root, "messageType", "type", "eventType", "event", "topic");
            String cjOrderId = firstText(root, "orderId", "cjOrderId", "order_id");
            String merchantOrder = firstText(root, "orderNumber", "orderNum", "merchantOrderNumber", "shopOrderNumber");
            String tracking = firstText(root, "trackingNumber", "trackNumber", "logisticNo");
            String eventId = firstText(root, "eventId", "messageId", "id", "logId");
            if (eventId == null || eventId.isBlank()) eventId = sha256((eventType == null ? "" : eventType) + "|" + (cjOrderId == null ? "" : cjOrderId) + "|" + (merchantOrder == null ? "" : merchantOrder) + "|" + (tracking == null ? "" : tracking) + "|" + body);
            if (webhookEventRepository.findByProviderAndEventId("CJ", eventId).isPresent()) {
                return ResponseEntity.ok(Map.of("status", "duplicate"));
            }
            WebhookEvent ev = new WebhookEvent();
            ev.setProvider("CJ");
            ev.setEventId(eventId.length() > 128 ? eventId.substring(0, 128) : eventId);
            ev.setEventType(eventType);
            ev.setPayloadHash(sha256(body));
            ev.setProcessStatus("RECEIVED");
            Optional<Order> orderOpt = findOrder(cjOrderId, merchantOrder, root);
            orderOpt.ifPresent(o -> ev.setOrderId(o.getId()));
            try {
                webhookEventRepository.saveAndFlush(ev);
            } catch (DataIntegrityViolationException dup) {
                return ResponseEntity.ok(Map.of("status", "duplicate"));
            }
            try {
                if (orderOpt.isPresent()) {
                    Order order = orderOpt.get();
                    // update CJ id if we learned it
                    if (cjOrderId != null && !cjOrderId.isBlank() && (order.getCjOrderId() == null || order.getCjOrderId().isBlank())) {
                        order.setCjOrderId(cjOrderId.trim());
                        orderRepository.save(order);
                    }
                    cjFulfillmentService.syncOrderFromCj(order.getOrderNumber());
                    ev.setProcessStatus("PROCESSED");
                } else {
                    ev.setProcessStatus("IGNORED");
                    ev.setErrorMessage("No matching local order found for CJ webhook.");
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
            // always acknowledge syntactically bad payloads to avoid infinite retries; logged via webhook record not possible
        }
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    private Optional<Order> findOrder(String cjOrderId, String merchantOrder, JsonNode root) {
        if (merchantOrder != null && !merchantOrder.isBlank()) {
            Optional<Order> o = orderRepository.findByOrderNumber(merchantOrder.trim());
            if (o.isPresent()) return o;
        }
        if (cjOrderId != null && !cjOrderId.isBlank()) {
            Optional<Order> o = orderRepository.findByCjOrderId(cjOrderId.trim());
            if (o.isPresent()) return o;
        }
        // sometimes nested in data
        JsonNode data = root.path("data");
        if (!data.isMissingNode()) {
            String c = firstText(data, "orderId", "cjOrderId");
            String m = firstText(data, "orderNumber", "orderNum", "merchantOrderNumber");
            if (m != null) { Optional<Order> o = orderRepository.findByOrderNumber(m.trim()); if (o.isPresent()) return o; }
            if (c != null) { Optional<Order> o = orderRepository.findByCjOrderId(c.trim()); if (o.isPresent()) return o; }
        }
        return Optional.empty();
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
