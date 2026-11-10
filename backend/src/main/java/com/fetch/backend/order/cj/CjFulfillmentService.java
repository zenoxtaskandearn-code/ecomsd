package com.fetch.backend.order.cj;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fetch.backend.catalog.cj.CjDropshippingClient;
import com.fetch.backend.catalog.entity.ProductVariant;
import com.fetch.backend.catalog.repo.ProductVariantRepository;
import com.fetch.backend.common.error.BadRequestException;
import com.fetch.backend.common.error.NotFoundException;
import com.fetch.backend.order.entity.Order;
import com.fetch.backend.order.entity.OrderItem;
import com.fetch.backend.order.repo.OrderItemRepository;
import com.fetch.backend.order.repo.OrderRepository;
import com.fetch.backend.payment.entity.Shipment;
import com.fetch.backend.payment.repo.ShipmentRepository;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;

@Service
public class CjFulfillmentService {
    private final CjDropshippingClient cj;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository variantRepository;
    private final ShipmentRepository shipmentRepository;
    private final ObjectMapper mapper;
    private final ApplicationContext applicationContext;

    public CjFulfillmentService(CjDropshippingClient cj, OrderRepository orderRepository, OrderItemRepository orderItemRepository,
                                ProductVariantRepository variantRepository, ShipmentRepository shipmentRepository, ObjectMapper mapper,
                                ApplicationContext applicationContext) {
        this.cj = cj; this.orderRepository = orderRepository; this.orderItemRepository = orderItemRepository;
        this.variantRepository = variantRepository; this.shipmentRepository = shipmentRepository; this.mapper = mapper;
        this.applicationContext = applicationContext;
    }

    public record CjSubmitOptions(String logisticName, String fromCountryCode, String remark) {
        public static CjSubmitOptions none() { return new CjSubmitOptions(null, null, null); }
    }

    @Transactional
    public Map<String, Object> submitOrderToCj(String orderNumber) {
        return submitOrderToCj(orderNumber, false, CjSubmitOptions.none());
    }

    @Transactional
    public Map<String, Object> submitOrderToCj(String orderNumber, boolean allowPendingPayment) {
        return submitOrderToCj(orderNumber, allowPendingPayment, CjSubmitOptions.none());
    }

    @Transactional
    public Map<String, Object> submitOrderToCj(String orderNumber, boolean allowPendingPayment, CjSubmitOptions options) {
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new NotFoundException("Order not found"));
        if (!cj.isConfigured()) throw new BadRequestException("CJdropshipping API is not configured. Set CJ_API_ENABLED=true and CJ_API_KEY.");
        // Idempotency/race guard: if already submitted, never submit again.
        if (order.getCjOrderId() != null && !order.getCjOrderId().isBlank()) {
            try { syncOrderFromCj(orderNumber); } catch (Exception ignored) {}
            return detailMap(orderRepository.findByOrderNumber(orderNumber).orElse(order));
        }
        String status = nz(order.getStatus()).toUpperCase(Locale.ROOT);
        // Safety: never fulfill unpaid/cancelled/refunded orders via standard flow.
        if (Set.of("CANCELLED", "REFUNDED").contains(status)) {
            throw new BadRequestException("Cannot submit a " + status.toLowerCase(Locale.ROOT) + " order to CJ.");
        }
        Set<String> allowed = allowPendingPayment
                ? Set.of("PENDING_PAYMENT", "PAID", "PROCESSING", "FULFILLMENT_SUBMITTED", "PAYMENT_FAILED")
                : Set.of("PAID", "PROCESSING", "FULFILLMENT_SUBMITTED");
        if (!allowed.contains(status)) {
            throw new BadRequestException("Order cannot be submitted to CJ in status " + order.getStatus() + (allowPendingPayment ? "." : ". Wait for server-confirmed payment (PAID) or submit manually after review."));
        }
        List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
        if (items.isEmpty()) throw new BadRequestException("Order has no items.");
        List<CjDropshippingClient.CjOrderProduct> products = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (OrderItem item : items) {
            ProductVariant v = item.getProductVariantId() == null ? null : variantRepository.findById(item.getProductVariantId()).orElse(null);
            String vid = v == null ? null : v.getCjVariantId();
            if (vid == null || vid.isBlank()) missing.add((item.getSkuSnapshot() == null ? "item" : item.getSkuSnapshot()) + " (missing CJ variant id)");
            else products.add(new CjDropshippingClient.CjOrderProduct(vid.trim(), Math.max(1, item.getQuantity())));
        }
        if (!missing.isEmpty()) {
            throw new BadRequestException("Cannot submit to CJ: " + String.join("; ", missing) + ". Re-import/sync variants from CJ so cjVariantId is stored.");
        }
        Map<String, Object> addr = readJson(order.getShippingAddressSnapshot());
        String countryCode = first(addr, "countryCode", "country_code", "country").toUpperCase(Locale.ROOT);
        if (countryCode.length() > 2) countryCode = countryCode.substring(0, 2);
        if (countryCode.isBlank()) countryCode = "US";
        String fullName = nz(first(addr, "fullName", "name", "recipient"));
        String phone = nz(first(addr, "phone", "phoneNumber"));
        CjSubmitOptions opt = options == null ? CjSubmitOptions.none() : options;
        CjDropshippingClient.CjCreateOrderRequest req = new CjDropshippingClient.CjCreateOrderRequest(
                order.getOrderNumber(),
                countryCode,
                nz(first(addr, "countryName", "country")).isBlank() ? countryCode : nz(first(addr, "countryName", "country")),
                nz(first(addr, "state", "province", "region")),
                nz(first(addr, "city")),
                nz(first(addr, "line1", "address1", "address")),
                nz(first(addr, "line2", "address2")),
                fullName.isBlank() ? "Customer" : fullName,
                phone.isBlank() ? "0000000000" : phone,
                nz(first(addr, "postalCode", "zip", "postcode")),
                order.getCustomerEmail(),
                blankToNull(opt.remark()) != null ? opt.remark().trim() : ("Lupine & Leash order " + order.getOrderNumber()),
                blankToNull(opt.logisticName()),
                blankToNull(opt.fromCountryCode()),
                products
        );
        try {
            CjDropshippingClient.CjOrderResult res = cj.createOrder(req);
            order.setCjOrderId(blankToNull(res.cjOrderId() != null ? res.cjOrderId() : res.cjOrderNumber()));
            order.setCjOrderNumber(blankToNull(res.cjOrderNumber()));
            order.setCjFulfillmentStatus(blankToNull(res.status()));
            order.setCjSubmittedAt(Instant.now());
            order.setCjLastSyncAt(Instant.now());
            order.setCjLastError(null);
            order.setCjRawSnapshot(writeJsonSafe(res.raw()));
            if (Set.of("PAID", "PROCESSING").contains(status)) order.setStatus("FULFILLMENT_SUBMITTED");
            orderRepository.save(order);
            // try to sync details/tracking immediately (best effort)
            try { syncInternal(order); } catch (Exception ignored) {}
            return detailMap(orderRepository.findByOrderNumber(orderNumber).orElse(order));
        } catch (BadRequestException e) {
            self().recordFailure(order.getId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            String msg = "CJ order submission failed: " + (e.getMessage() == null ? "unknown error" : e.getMessage());
            self().recordFailure(order.getId(), msg.length() > 2000 ? msg.substring(0, 2000) : msg);
            throw new BadRequestException(msg);
        }
    }

    @Transactional
    public Map<String, Object> syncOrderFromCj(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new NotFoundException("Order not found"));
        if (!cj.isConfigured()) throw new BadRequestException("CJdropshipping API is not configured.");
        if (order.getCjOrderId() == null || order.getCjOrderId().isBlank()) throw new BadRequestException("Order has not been submitted to CJ.");
        try {
            syncInternal(order);
            order.setCjLastError(null);
        } catch (BadRequestException e) {
            self().recordFailure(order.getId(), e.getMessage());
            throw e;
        } catch (Exception e) {
            String msg = "CJ sync failed: " + safeMsg(e.getMessage());
            self().recordFailure(order.getId(), msg);
            throw new BadRequestException(msg);
        }
        return detailMap(orderRepository.findByOrderNumber(orderNumber).orElse(order));
    }

    public Map<String, Object> syncPendingCjOrders(int limit) {
        if (!cj.isConfigured()) throw new BadRequestException("CJdropshipping API is not configured.");
        List<Order> orders = orderRepository.findByCjOrderIdIsNotNullAndStatusNotIn(List.of("DELIVERED", "CANCELLED", "REFUNDED"));
        int synced = 0, failed = 0;
        List<String> errors = new ArrayList<>();
        int max = Math.min(100, Math.max(1, limit));
        CjFulfillmentService self = applicationContext.getBean(CjFulfillmentService.class);
        for (Order o : orders) {
            if (synced + failed >= max) break;
            try {
                self.syncOrderById(o.getId());
                synced++;
                try { Thread.sleep(350); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); break; }
            } catch (Exception e) {
                failed++;
                errors.add(o.getOrderNumber() + ": " + safeMsg(e.getMessage()));
            }
        }
        return Map.of("synced", synced, "failed", failed, "errors", errors);
    }

    @Transactional
    public void syncOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new NotFoundException("Order not found"));
        if (order.getCjOrderId() == null || order.getCjOrderId().isBlank()) return;
        try {
            syncInternal(order);
            order.setCjLastError(null);
        } catch (Exception e) {
            String msg = "CJ sync failed: " + safeMsg(e.getMessage());
            self().recordFailure(order.getId(), msg);
            throw e instanceof RuntimeException re ? re : new BadRequestException(msg);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailure(Long orderId, String error) {
        if (orderId == null) return;
        orderRepository.findById(orderId).ifPresent(o -> {
            o.setCjLastError(error == null ? null : (error.length() > 2000 ? error.substring(0, 2000) : error));
            o.setCjLastSyncAt(Instant.now());
            orderRepository.save(o);
        });
    }

    private CjFulfillmentService self() { return applicationContext.getBean(CjFulfillmentService.class); }

    public Map<String, Object> detailMap(Order order) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("cjConfigured", cj.isConfigured());
        m.put("cjOrderId", order.getCjOrderId());
        m.put("cjOrderNumber", order.getCjOrderNumber());
        m.put("cjFulfillmentStatus", order.getCjFulfillmentStatus());
        m.put("cjSubmittedAt", order.getCjSubmittedAt() == null ? null : order.getCjSubmittedAt().toString());
        m.put("cjLastSyncAt", order.getCjLastSyncAt() == null ? null : order.getCjLastSyncAt().toString());
        m.put("cjLastError", order.getCjLastError());
        return m;
    }

    private void syncInternal(Order order) {
        JsonNode detail = cj.getOrderDetail(order.getCjOrderId());
        String cjStatus = firstText(detail, "orderStatus", "status", "cjOrderStatus", "trackStatus");
        order.setCjFulfillmentStatus(blankToNull(cjStatus));
        order.setCjRawSnapshot(writeJsonSafe(detail));
        order.setCjLastSyncAt(Instant.now());

        String tracking = firstText(detail, "trackNumber", "trackingNumber", "logisticNo", "trackingNo");
        String logistic = firstText(detail, "logisticName", "logisticsName", "shippingMethod");
        String trackingUrl = firstText(detail, "trackingUrl", "trackUrl", "logisticsUrl");
        if (tracking != null && !tracking.isBlank()) {
            upsertCjShipment(order, tracking.trim(), logistic, trackingUrl, detail);
        }
        // If CJ order detail lacks tracking URL but has tracking, try logistic API (best effort)
        if (tracking != null && !tracking.isBlank() && (trackingUrl == null || trackingUrl.isBlank())) {
            try {
                CjDropshippingClient.CjTrackingInfo ti = cj.getTrackingInfo(tracking);
                if (ti != null) upsertCjShipment(order, tracking.trim(), nz(logistic).isBlank() ? ti.logisticName() : logistic, ti.trackingUrl(), ti.raw());
            } catch (Exception ignored) {}
        }

        applyStatusMapping(order, cjStatus, tracking);
        orderRepository.save(order);
    }

    private void upsertCjShipment(Order order, String tracking, String logistic, String trackingUrl, JsonNode raw) {
        Shipment s = shipmentRepository.findFirstByOrderIdAndTrackingNumber(order.getId(), tracking).orElseGet(Shipment::new);
        boolean isNew = s.getId() == null;
        s.setOrderId(order.getId());
        s.setSource("CJ");
        if (logistic != null && !logistic.isBlank()) s.setCarrier(logistic.trim());
        s.setTrackingNumber(tracking);
        if (trackingUrl != null && !trackingUrl.isBlank()) s.setTrackingUrl(trackingUrl.trim());
        String mapped = mapShipmentStatus(order.getCjFulfillmentStatus(), raw);
        if (s.getStatus() == null || isNew || !"DELIVERED".equalsIgnoreCase(s.getStatus())) s.setStatus(mapped);
        if ("SHIPPED".equalsIgnoreCase(mapped) && s.getShippedAt() == null) s.setShippedAt(Instant.now());
        if ("DELIVERED".equalsIgnoreCase(mapped)) { s.setStatus("DELIVERED"); if (s.getDeliveredAt() == null) s.setDeliveredAt(Instant.now()); }
        s.setCjTrackSyncedAt(Instant.now());
        s.setCjRawSnapshot(writeJsonSafe(raw));
        shipmentRepository.save(s);
    }

    private void applyStatusMapping(Order order, String cjStatus, String tracking) {
        String cur = nz(order.getStatus()).toUpperCase(Locale.ROOT);
        if (Set.of("CANCELLED", "REFUNDED").contains(cur)) return;
        String cj = nz(cjStatus).toUpperCase(Locale.ROOT);
        boolean hasTracking = tracking != null && !tracking.isBlank();
        boolean delivered = cj.contains("DELIVER") || cj.contains("COMPLETED") && hasTracking || cj.equals("FINISH") || cj.equals("SIGNED");
        boolean shipped = hasTracking || cj.contains("SHIP") || cj.contains("DISPATCH") || cj.contains("IN_TRANSIT") || cj.contains("TRANSIT") || cj.contains("OUT_FOR_DELIVERY");
        boolean cancelled = cj.contains("CANCEL") || cj.contains("CLOSED") || cj.contains("REJECT");
        if (delivered) {
            order.setStatus("DELIVERED");
        } else if (cancelled && !"DELIVERED".equals(cur) && !"SHIPPED".equals(cur)) {
            // don't auto-cancel paid local orders aggressively; record CJ status but keep manual control
            order.setCjFulfillmentStatus(cjStatus == null ? "CJ_CANCELLED" : cjStatus);
        } else if (shipped && Set.of("PAID", "PROCESSING", "FULFILLMENT_SUBMITTED", "PENDING_PAYMENT").contains(cur)) {
            order.setStatus("SHIPPED");
        } else if (order.getCjOrderId() != null && "PAID".equals(cur)) {
            order.setStatus("FULFILLMENT_SUBMITTED");
        }
    }

    private String mapShipmentStatus(String cjStatus, JsonNode raw) {
        String cj = nz(cjStatus).toUpperCase(Locale.ROOT);
        String t = firstText(raw, "trackingStatus", "deliveryStatus", "status");
        String s = (nz(t) + " " + cj).toUpperCase(Locale.ROOT);
        if (s.contains("DELIVER") || s.contains("SIGNED") || s.contains("FINISH")) return "DELIVERED";
        if (s.contains("TRANSIT") || s.contains("SHIP") || s.contains("DISPATCH") || s.contains("PICK")) return "SHIPPED";
        return "PENDING";
    }

    private Map<String, Object> readJson(String json) {
        try { return mapper.readValue(json == null ? "{}" : json, new TypeReference<>() {}); }
        catch (Exception e) { return Map.of(); }
    }
    private String writeJsonSafe(Object o) {
        try { String s = mapper.writeValueAsString(o); return s != null && s.length() > 60000 ? s.substring(0, 60000) : s; }
        catch (Exception e) { return null; }
    }
    private static String nz(String s) { return s == null ? "" : s.trim(); }
    private static String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
    private static String safeMsg(String s) { if (s == null) return "unknown error"; return s.length() > 500 ? s.substring(0, 500) : s; }
    private static String first(Map<String, Object> m, String... keys) {
        if (m == null) return "";
        for (String k : keys) { Object v = m.get(k); if (v != null && !String.valueOf(v).isBlank() && !"null".equalsIgnoreCase(String.valueOf(v))) return String.valueOf(v).trim(); }
        // try case-insensitive
        for (String k : keys) {
            for (var e : m.entrySet()) if (e.getKey() != null && e.getKey().equalsIgnoreCase(k) && e.getValue() != null) return String.valueOf(e.getValue()).trim();
        }
        return "";
    }
    private static String firstText(JsonNode n, String... fields) {
        if (n == null) return null;
        for (String f : fields) {
            JsonNode v = n.path(f);
            if (v.isMissingNode() || v.isNull()) continue;
            String s = v.asText();
            if (s != null && !s.isBlank() && !"null".equalsIgnoreCase(s)) return s;
        }
        return null;
    }
}
