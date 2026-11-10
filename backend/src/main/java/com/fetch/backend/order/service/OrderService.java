package com.fetch.backend.order.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fetch.backend.cart.entity.CartItem;
import com.fetch.backend.cart.repo.CartItemRepository;
import com.fetch.backend.cart.repo.CartRepository;
import com.fetch.backend.catalog.entity.ProductVariant;
import com.fetch.backend.catalog.repo.ProductVariantRepository;
import com.fetch.backend.common.error.BadRequestException;
import com.fetch.backend.common.error.NotFoundException;
import com.fetch.backend.order.dto.OrderDtos;
import com.fetch.backend.order.entity.Order;
import com.fetch.backend.order.entity.OrderItem;
import com.fetch.backend.order.repo.OrderItemRepository;
import com.fetch.backend.order.repo.OrderRepository;
import com.fetch.backend.payment.repo.ShipmentRepository;
import com.fetch.backend.order.cj.CjAutoSubmitter;
import com.fetch.backend.common.service.AuditLogService;
import com.fetch.backend.common.service.NotificationService;
import com.fetch.backend.user.service.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.*;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final CjAutoSubmitter cjAutoSubmitter;
    private final OrderStatusService orderStatusService;
    private final OrderCostService orderCostService;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private static final SecureRandom RNG = new SecureRandom();

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository, CartRepository cartRepository,
                        CartItemRepository cartItemRepository, ProductVariantRepository variantRepository, ShipmentRepository shipmentRepository, UserService userService, ObjectMapper objectMapper,
                        CjAutoSubmitter cjAutoSubmitter, OrderStatusService orderStatusService, OrderCostService orderCostService,
                        AuditLogService auditLogService, NotificationService notificationService) {
        this.orderRepository = orderRepository; this.orderItemRepository = orderItemRepository; this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository; this.variantRepository = variantRepository; this.shipmentRepository = shipmentRepository; this.userService = userService; this.objectMapper = objectMapper;
        this.cjAutoSubmitter = cjAutoSubmitter; this.orderStatusService = orderStatusService; this.orderCostService = orderCostService;
        this.auditLogService = auditLogService; this.notificationService = notificationService;
    }

    @Transactional(readOnly = true)
    public OrderDtos.CheckoutPreviewResponse preview(OrderDtos.CreateOrderRequest request) {
        var user = userService.currentUser();
        var cart = cartRepository.findByUserIdAndStatus(user.getId(), "ACTIVE").orElseThrow(() -> new BadRequestException("Cart is empty"));
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        if (items.isEmpty()) throw new BadRequestException("Cart is empty");
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderDtos.OrderItemResponse> itemDtos = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (CartItem ci : items) {
            ProductVariant v = variantRepository.findById(ci.getProductVariantId()).orElse(null);
            if (v == null || !v.isActive() || !"ACTIVE".equals(v.getProduct().getStatus())) {
                warnings.add("An item in your cart is no longer available.");
                continue;
            }
            BigDecimal line = v.getPriceAmount().multiply(BigDecimal.valueOf(ci.getQuantity()));
            subtotal = subtotal.add(line);
            itemDtos.add(new OrderDtos.OrderItemResponse(v.getProduct().getName(), v.getTitle(), v.getSku(), v.getPriceAmount(), ci.getQuantity(), line));
        }
        if (itemDtos.isEmpty()) throw new BadRequestException("No available items in cart");
        BigDecimal shipping = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO; // MVP: no automated US tax
        BigDecimal total = subtotal.add(shipping).add(tax);
        return new OrderDtos.CheckoutPreviewResponse("USD", subtotal, shipping, tax, BigDecimal.ZERO, total, itemDtos, warnings,
                "Shipping and any applicable taxes/duties are confirmed at finalization. International customers are responsible for duties/taxes where applicable.");
    }

    @Transactional
    public OrderDtos.OrderResponse createFromCart(OrderDtos.CreateOrderRequest request) {
        var user = userService.currentUser();
        String idem = request.idempotencyKey() == null ? null : request.idempotencyKey().trim();
        if (idem != null && !idem.isBlank()) {
            var existing = orderRepository.findByUserIdAndCheckoutIdempotencyKey(user.getId(), idem);
            if (existing.isPresent()) {
                Order o = existing.get();
                return toResponse(o, orderItemRepository.findByOrderId(o.getId()));
            }
        }
        var cart = cartRepository.findByUserIdAndStatus(user.getId(), "ACTIVE").orElseThrow(() -> new BadRequestException("Cart is empty"));
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        if (items.isEmpty()) throw new BadRequestException("Cart is empty");

        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                Order order = new Order();
                order.setOrderNumber(generateOrderNumber());
                order.setUserId(user.getId());
                order.setStatus("PENDING_PAYMENT");
                order.setPaymentStatus("PENDING");
                order.setFulfillmentStatus("PENDING");
                order.setCurrency("USD");
                order.setCustomerEmail(request.customerEmail() != null && !request.customerEmail().isBlank() ? request.customerEmail().trim().toLowerCase() : user.getEmail());
                order.setShippingAddressSnapshot(writeJson(request.shippingAddress()));
                order.setBillingAddressSnapshot(request.billingAddress() == null ? null : writeJson(request.billingAddress()));
                order.setPlacedAt(Instant.now());
                if (idem != null && !idem.isBlank()) order.setCheckoutIdempotencyKey(idem.length() > 80 ? idem.substring(0, 80) : idem);
                order.setTrackingAccessToken(generateTrackingToken());

                BigDecimal subtotal = BigDecimal.ZERO;
                List<OrderItem> orderItems = new ArrayList<>();
                for (CartItem ci : items) {
                    ProductVariant v = variantRepository.findById(ci.getProductVariantId()).orElseThrow(() -> new BadRequestException("A cart item is no longer available"));
                    if (!v.isActive() || !"ACTIVE".equals(v.getProduct().getStatus())) throw new BadRequestException("A cart item is unavailable");
                    BigDecimal line = v.getPriceAmount().multiply(BigDecimal.valueOf(ci.getQuantity()));
                    subtotal = subtotal.add(line);
                    OrderItem oi = new OrderItem();
                    oi.setProductVariantId(v.getId());
                    oi.setProductNameSnapshot(v.getProduct().getName());
                    oi.setVariantTitleSnapshot(v.getTitle());
                    oi.setSkuSnapshot(v.getSku());
                    oi.setUnitPriceAmount(v.getPriceAmount());
                    oi.setQuantity(ci.getQuantity());
                    oi.setLineTotalAmount(line);
                    orderItems.add(oi);
                }
                order.setSubtotalAmount(subtotal);
                order.setShippingAmount(BigDecimal.ZERO);
                order.setTaxAmount(BigDecimal.ZERO); // MVP: no automated tax
                order.setDiscountAmount(BigDecimal.ZERO);
                order.setTotalAmount(subtotal);
                orderRepository.saveAndFlush(order);
                for (OrderItem oi : orderItems) { oi.setOrderId(order.getId()); orderItemRepository.save(oi); }
                orderStatusService.record(order.getId(), "ORDER", null, "PENDING_PAYMENT", "SYSTEM", "Order created, awaiting payment");
                orderStatusService.record(order.getId(), "PAYMENT", null, "PENDING", "SYSTEM", "Payment pending");
                orderStatusService.record(order.getId(), "FULFILLMENT", null, "PENDING", "SYSTEM", "Fulfillment pending payment");
                try { if (orderCostService != null) orderCostService.upsertForOrder(order); } catch (Exception ignored) {}
                try { if (auditLogService != null) auditLogService.record("CUSTOMER", String.valueOf(user.getId()), "ORDER_CREATED", "Order", String.valueOf(order.getId()), order.getId(), "INFO", Map.of("orderNumber", order.getOrderNumber()), null); } catch (Exception ignored) {}
                try { if (notificationService != null) notificationService.enqueue("ORDER_CONFIRMATION", "EMAIL", order.getCustomerEmail(), "Order confirmation " + order.getOrderNumber(), order.getId(), user.getId(), Map.of("orderNumber", order.getOrderNumber(), "total", order.getTotalAmount()), "order:" + order.getId() + ":confirm"); } catch (Exception ignored) {}

                cart.setStatus("CONVERTED");
                cartRepository.save(cart);
                cjAutoSubmitter.onOrderCreated(order.getOrderNumber());
                return toResponse(order, orderItems);
            } catch (DataIntegrityViolationException e) {
                if (attempt == 4) throw new BadRequestException("Could not create order. Please try again.");
            }
        }
        throw new BadRequestException("Could not create order. Please try again.");
    }

    @Transactional(readOnly = true)
    public List<OrderDtos.OrderResponse> myOrders() {
        var user = userService.currentUser();
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(o -> toResponse(o, orderItemRepository.findByOrderId(o.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderResponse myOrder(String orderNumber) {
        var user = userService.currentUser();
        Order order = orderRepository.findByOrderNumberAndUserId(orderNumber, user.getId()).orElseThrow(() -> new NotFoundException("Order not found"));
        return toResponse(order, orderItemRepository.findByOrderId(order.getId()));
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderResponse trackOrder(String orderNumber, String trackingToken) {
        if (trackingToken == null || trackingToken.isBlank()) throw new NotFoundException("Order not found");
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new NotFoundException("Order not found"));
        if (order.getTrackingAccessToken() == null || !order.getTrackingAccessToken().equals(trackingToken.trim())) {
            throw new NotFoundException("Order not found");
        }
        return toResponse(order, orderItemRepository.findByOrderId(order.getId()));
    }

    private OrderDtos.OrderResponse toResponse(Order order, List<OrderItem> items) {
        Map<String, Object> addr = readJson(order.getShippingAddressSnapshot());
        List<OrderDtos.OrderItemResponse> itemDtos = items.stream().map(i -> new OrderDtos.OrderItemResponse(
                i.getProductNameSnapshot(), i.getVariantTitleSnapshot(), i.getSkuSnapshot(), i.getUnitPriceAmount(), i.getQuantity(), i.getLineTotalAmount())).toList();
        List<OrderDtos.ShipmentResponse> shipments = shipmentRepository.findByOrderId(order.getId()).stream()
                .map(s -> new OrderDtos.ShipmentResponse(
                        s.getCarrier(), s.getTrackingNumber(), s.getTrackingUrl(), s.getStatus(),
                        fmt(s.getShippedAt()), fmt(s.getDeliveredAt()), fmt(s.getCjTrackSyncedAt()), s.getSource()))
                .toList();

        Instant shippedAt = shipments.stream().map(s -> parseInstantSafe(s.shippedAt())).filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
        // prefer entity shipment timestamps for timeline accuracy
        var shipEntities = shipmentRepository.findByOrderId(order.getId());
        Instant shippedEntityAt = shipEntities.stream().map(com.fetch.backend.payment.entity.Shipment::getShippedAt).filter(Objects::nonNull).min(Comparator.naturalOrder()).orElse(null);
        Instant deliveredEntityAt = shipEntities.stream().map(com.fetch.backend.payment.entity.Shipment::getDeliveredAt).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null);
        if (shippedAt == null) shippedAt = shippedEntityAt;

        OrderDtos.FulfillmentInfo fulfillment = buildFulfillment(order, shipEntities, deliveredEntityAt);
        List<OrderDtos.TimelineEvent> timeline = buildTimeline(order, shippedAt, deliveredEntityAt, fulfillment);

        return new OrderDtos.OrderResponse(order.getOrderNumber(), order.getStatus(), order.getCurrency(), order.getSubtotalAmount(),
                order.getShippingAmount(), order.getTaxAmount(), order.getDiscountAmount(), order.getTotalAmount(),
                order.getCustomerEmail(), fmt(order.getPlacedAt()), fmt(order.getPaidAt()), addr, itemDtos, shipments, timeline, fulfillment);
    }

    private OrderDtos.FulfillmentInfo buildFulfillment(Order order, List<com.fetch.backend.payment.entity.Shipment> shipEntities, Instant deliveredEntityAt) {
        boolean hasCj = (order.getCjOrderId() != null && !order.getCjOrderId().isBlank())
                || (order.getCjFulfillmentStatus() != null && !order.getCjFulfillmentStatus().isBlank())
                || (order.getCjSubmittedAt() != null);
        String supplierName = hasCj ? "CJdropshipping" : "Fulfillment partner";
        String supplierOrderId = order.getCjOrderNumber() != null && !order.getCjOrderNumber().isBlank() ? order.getCjOrderNumber() : order.getCjOrderId();
        String fulfillmentStatus = deriveCustomerFulfillmentStatus(order, hasCj);
        Instant lastUpdated = order.getCjLastSyncAt() != null ? order.getCjLastSyncAt() : (order.getUpdatedAt() != null ? order.getUpdatedAt() : order.getPlacedAt());
        String expected = estimateDelivery(order, shipEntities, deliveredEntityAt);
        String msg = fulfillmentMessage(order, shipEntities, hasCj);
        // Do not expose internal supplier/system error details to customers.
        String err = null;
        if (order.getCjLastError() != null && !order.getCjLastError().isBlank() && !"DELIVERED".equalsIgnoreCase(nz(order.getStatus()))) {
            // Keep customer note generic; admin views expose raw CJ error.
            err = "We're preparing your order for fulfillment. If there is a delay, our team will review and update your order shortly.";
        }
        return new OrderDtos.FulfillmentInfo(supplierName, fulfillmentStatus, supplierOrderId,
                fmt(order.getCjSubmittedAt()), fmt(lastUpdated), expected, msg, err);
    }

    private String deriveCustomerFulfillmentStatus(Order order, boolean hasCj) {
        String st = nz(order.getStatus()).toUpperCase(Locale.ROOT);
        String fs = nz(order.getFulfillmentStatus()).toUpperCase(Locale.ROOT);
        if ("DELIVERED".equals(st) || "DELIVERED".equals(fs)) return "DELIVERED";
        if ("SHIPPED".equals(st) || "SHIPPED".equals(fs) || "IN_TRANSIT".equals(fs)) return "SHIPPED";
        if (hasCj || "FULFILLMENT_SUBMITTED".equals(st) || "SUBMITTED".equals(fs)) return "PROCESSING";
        if (Set.of("PAID","PROCESSING","AWAITING_FULFILMENT").contains(st) || "CAPTURED".equals(nz(order.getPaymentStatus()).toUpperCase(Locale.ROOT))) return "PROCESSING";
        if ("PENDING_PAYMENT".equals(st) || "PAYMENT_FAILED".equals(st)) return "PENDING";
        if ("CANCELLED".equals(st) || "REFUNDED".equals(st)) return st;
        return fs.isBlank() ? "PROCESSING" : fs;
    }

    private String fulfillmentMessage(Order order, List<com.fetch.backend.payment.entity.Shipment> shipEntities, boolean hasCj) {
        String st = nz(order.getStatus()).toUpperCase(Locale.ROOT);
        if ("DELIVERED".equals(st)) return "Your order was delivered. Thank you for shopping with Lupine & Leash.";
        if ("SHIPPED".equals(st)) return "Your order is on the way. Tracking updates are shown below when available.";
        if ("FULFILLMENT_SUBMITTED".equals(st) || (hasCj && "PROCESSING".equals(st))) return "Your order has been submitted to our fulfillment partner and is being prepared.";
        if ("PAID".equals(st) || "PROCESSING".equals(st)) return "Payment confirmed. We’re preparing your order for fulfillment.";
        if ("PENDING_PAYMENT".equals(st)) return "Waiting for payment confirmation.";
        if ("PAYMENT_FAILED".equals(st)) return "Payment was not completed. You can try checkout again.";
        if ("CANCELLED".equals(st) || "REFUNDED".equals(st)) return "This order is " + st.toLowerCase(Locale.ROOT).replace('_', ' ') + ".";
        if (!shipEntities.isEmpty()) return "Fulfillment is in progress. See shipment details below.";
        return "We’ll update this page as your order progresses.";
    }

    private String estimateDelivery(Order order, List<com.fetch.backend.payment.entity.Shipment> shipEntities, Instant deliveredEntityAt) {
        if (deliveredEntityAt != null || "DELIVERED".equalsIgnoreCase(nz(order.getStatus()))) return "Delivered";
        String st = nz(order.getStatus()).toUpperCase(Locale.ROOT);
        if ("CANCELLED".equals(st) || "REFUNDED".equals(st)) return "Not applicable";
        Instant base = order.getPaidAt() != null ? order.getPaidAt() : order.getPlacedAt();
        if (base == null) return "To be confirmed after processing";
        // typical dropshipping window shown as range; refined when shipped
        java.time.ZoneId zone = java.time.ZoneId.of("UTC");
        java.time.LocalDate b = base.atZone(zone).toLocalDate();
        if ("SHIPPED".equals(st) || shipEntities.stream().anyMatch(s -> s.getShippedAt() != null)) {
            java.time.LocalDate from = b.plusDays(5);
            java.time.LocalDate to = b.plusDays(12);
            return "Estimated " + from + " to " + to + " (UTC)";
        }
        if ("PAID".equals(st) || "PROCESSING".equals(st) || "FULFILLMENT_SUBMITTED".equals(st)) {
            java.time.LocalDate from = b.plusDays(7);
            java.time.LocalDate to = b.plusDays(15);
            return "Estimated " + from + " to " + to + " (UTC)";
        }
        if ("PENDING_PAYMENT".equals(st) || "PAYMENT_FAILED".equals(st)) return "Starts after payment confirmation";
        return "Estimated 7–15 business days after payment";
    }

    private List<OrderDtos.TimelineEvent> buildTimeline(Order order, Instant shippedAt, Instant deliveredAt, OrderDtos.FulfillmentInfo fulfillment) {
        String st = nz(order.getStatus()).toUpperCase(Locale.ROOT);
        boolean cancelled = "CANCELLED".equals(st) || "REFUNDED".equals(st);
        boolean paid = order.getPaidAt() != null || Set.of("PAID","PROCESSING","FULFILLMENT_SUBMITTED","SHIPPED","DELIVERED").contains(st);
        boolean processing = paid && (Set.of("PROCESSING","FULFILLMENT_SUBMITTED","SHIPPED","DELIVERED").contains(st) || order.getCjSubmittedAt() != null);
        boolean submitted = order.getCjSubmittedAt() != null || "FULFILLMENT_SUBMITTED".equals(st) || "SHIPPED".equals(st) || "DELIVERED".equals(st);
        boolean shipped = shippedAt != null || "SHIPPED".equals(st) || "DELIVERED".equals(st);
        boolean delivered = deliveredAt != null || "DELIVERED".equals(st);

        LinkedHashMap<String, OrderDtos.TimelineEvent> steps = new LinkedHashMap<>();
        steps.put("placed", ev("placed", "Order placed", order.getPlacedAt(), true, "PENDING_PAYMENT".equals(st) && !paid, null));
        if ("PAYMENT_FAILED".equals(st)) {
            steps.put("payment_failed", ev("payment_failed", "Payment failed", order.getUpdatedAt(), true, true, "Payment was not completed."));
        } else {
            steps.put("paid", ev("paid", "Payment confirmed", order.getPaidAt(), paid, paid && !processing && !cancelled, paid ? null : "Awaiting payment confirmation"));
        }
        if (!cancelled) {
            steps.put("processing", ev("processing", "Processing order", submitted ? order.getCjSubmittedAt() : (processing ? order.getPaidAt() : null), processing, processing && !submitted && !shipped, processing ? "Preparing items for fulfillment" : "Pending"));
            if (submitted || order.getCjOrderId() != null) {
                String note = fulfillment != null && fulfillment.supplierName() != null ? "Submitted to " + fulfillment.supplierName() : "Submitted to fulfillment partner";
                steps.put("submitted", ev("submitted", "Submitted for fulfillment", order.getCjSubmittedAt(), submitted, submitted && !shipped, submitted ? note : "Pending"));
            }
            steps.put("shipped", ev("shipped", "Shipped", shippedAt, shipped, shipped && !delivered, shipped ? "Package handed to carrier" : "Pending"));
            steps.put("delivered", ev("delivered", "Delivered", deliveredAt, delivered, delivered, delivered ? "Order delivered" : "Pending"));
        } else {
            steps.put("closed", ev("closed", st.charAt(0) + st.substring(1).toLowerCase(Locale.ROOT).replace('_', ' '), order.getUpdatedAt(), true, true, "Order closed"));
        }
        return new ArrayList<>(steps.values());
    }

    private OrderDtos.TimelineEvent ev(String key, String label, Instant at, boolean done, boolean current, String note) {
        String when = done ? fmt(at) : null;
        String noteOut = note;
        if (when == null && done && at == null) when = current ? "In progress" : "Completed";
        if (when == null && !done) when = "Pending";
        return new OrderDtos.TimelineEvent(key, label, when, done, current, noteOut);
    }

    private static String fmt(Instant t) { return t == null ? null : t.toString(); }
    private static Instant parseInstantSafe(String s) { try { return s == null ? null : Instant.parse(s); } catch (Exception e) { return null; } }
    private static String nz(String s) { return s == null ? "" : s; }

    private String generateOrderNumber() {
        return "FT-" + (100000 + RNG.nextInt(900000));
    }

    private String generateTrackingToken() {
        byte[] b = new byte[24];
        RNG.nextBytes(b);
        StringBuilder sb = new StringBuilder(b.length * 2);
        for (byte x : b) sb.append(String.format("%02x", x));
        return sb.toString();
    }

    public com.fetch.backend.order.entity.Order getOrderEntity(String orderNumber) {
        return orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new NotFoundException("Order not found"));
    }
    private String writeJson(Object value) { try { return objectMapper.writeValueAsString(value); } catch (Exception e) { throw new BadRequestException("Invalid address"); } }
    private Map<String, Object> readJson(String json) { try { return objectMapper.readValue(json, new TypeReference<>() {}); } catch (Exception e) { return Map.of(); } }
}
