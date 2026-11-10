package com.fetch.backend.order.admin;

import com.fetch.backend.common.error.BadRequestException;
import com.fetch.backend.common.error.NotFoundException;
import com.fetch.backend.order.cj.CjFulfillmentService;
import com.fetch.backend.order.entity.Order;
import com.fetch.backend.order.repo.OrderItemRepository;
import com.fetch.backend.order.repo.OrderRepository;
import com.fetch.backend.order.service.OrderStatusService;
import com.fetch.backend.payment.entity.Shipment;
import com.fetch.backend.payment.repo.ShipmentRepository;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/admin/orders")
public class AdminOrderController {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ShipmentRepository shipmentRepository;
    private final CjFulfillmentService cjFulfillmentService;
    private final OrderStatusService orderStatusService;

    public AdminOrderController(OrderRepository orderRepository, OrderItemRepository orderItemRepository, ShipmentRepository shipmentRepository,
                                CjFulfillmentService cjFulfillmentService, OrderStatusService orderStatusService) {
        this.orderRepository = orderRepository; this.orderItemRepository = orderItemRepository; this.shipmentRepository = shipmentRepository;
        this.cjFulfillmentService = cjFulfillmentService; this.orderStatusService = orderStatusService;
    }

    @GetMapping
    public List<Order> list(@RequestParam(required = false) String fulfillment) {
        List<Order> all = orderRepository.findAll().stream()
                .sorted(Comparator.comparing(Order::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
        if (fulfillment == null || fulfillment.isBlank()) return all;
        String f = fulfillment.trim().toUpperCase();
        return all.stream().filter(o -> switch (f) {
            case "CJ_SUBMITTED" -> o.getCjOrderId() != null && !o.getCjOrderId().isBlank();
            case "CJ_NOT_SUBMITTED" -> (o.getCjOrderId() == null || o.getCjOrderId().isBlank()) && "PAID".equalsIgnoreCase(o.getStatus());
            case "CJ_ERROR" -> o.getCjLastError() != null && !o.getCjLastError().isBlank();
            case "CJ_TRACKING" -> o.getCjOrderId() != null && !"DELIVERED".equalsIgnoreCase(o.getStatus()) && !"CANCELLED".equalsIgnoreCase(o.getStatus());
            default -> true;
        }).toList();
    }

    @GetMapping("/{orderNumber}")
    public Map<String, Object> detail(@PathVariable String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new NotFoundException("Order not found"));
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("order", order);
        res.put("items", orderItemRepository.findByOrderId(order.getId()));
        res.put("shipments", shipmentRepository.findByOrderId(order.getId()));
        res.put("cjFulfillment", cjFulfillmentService.detailMap(order));
        return res;
    }

    @PatchMapping("/{orderNumber}/status")
    public Order updateStatus(@PathVariable String orderNumber, @RequestBody Map<String, String> body) {
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new NotFoundException("Order not found"));
        String next = body.get("status");
        if (next == null || next.isBlank()) throw new BadRequestException("status is required");
        String target = next.trim().toUpperCase();
        String from = order.getStatus();
        // Basic financial/order safety: don't mark PAID without payment capture; don't regress delivered/refunded arbitrarily
        if ("PAID".equals(target) && !"CAPTURED".equalsIgnoreCase(nz(order.getPaymentStatus()))) {
            throw new BadRequestException("Cannot set order to PAID without server-confirmed captured payment.");
        }
        if (Set.of("REFUNDED", "CANCELLED").contains(from) && !Set.of("REFUNDED", "CANCELLED").contains(target)) {
            throw new BadRequestException("Cannot move a " + from + " order to " + target + " without an explicit refund/reopen workflow.");
        }
        orderStatusService.applyOrderStatus(order, target, "ADMIN", body.getOrDefault("note", "Status updated by admin"));
        // keep fulfillment/payment aligned for common admin actions
        if ("DELIVERED".equals(target)) orderStatusService.markFulfillment(order, "DELIVERED", "ADMIN", "Marked delivered by admin");
        if ("SHIPPED".equals(target)) orderStatusService.markFulfillment(order, "SHIPPED", "ADMIN", "Marked shipped by admin");
        if ("CANCELLED".equals(target)) orderStatusService.markFulfillment(order, "CANCELLED", "ADMIN", "Cancelled by admin");
        return orderRepository.save(order);
    }

    @PostMapping("/{orderNumber}/shipments")
    public Shipment addShipment(@PathVariable String orderNumber, @RequestBody Map<String, String> body) {
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new NotFoundException("Order not found"));
        Shipment s = new Shipment();
        s.setOrderId(order.getId());
        s.setSource("MANUAL");
        s.setCarrier(body.get("carrier"));
        s.setTrackingNumber(body.get("trackingNumber"));
        s.setTrackingUrl(body.get("trackingUrl"));
        s.setStatus(body.getOrDefault("status", "SHIPPED"));
        if ("SHIPPED".equalsIgnoreCase(s.getStatus()) && s.getShippedAt() == null) s.setShippedAt(Instant.now());
        if ("DELIVERED".equalsIgnoreCase(s.getStatus()) && s.getDeliveredAt() == null) s.setDeliveredAt(Instant.now());
        shipmentRepository.save(s);
        if ("DELIVERED".equalsIgnoreCase(s.getStatus()) && !"CANCELLED".equalsIgnoreCase(order.getStatus()) && !"REFUNDED".equalsIgnoreCase(order.getStatus())) {
            orderStatusService.applyOrderStatus(order, "DELIVERED", "ADMIN", "Manual shipment marked delivered");
            orderStatusService.markFulfillment(order, "DELIVERED", "ADMIN", "Manual shipment delivered");
        } else if ("SHIPPED".equalsIgnoreCase(s.getStatus()) && !"CANCELLED".equalsIgnoreCase(order.getStatus()) && !"REFUNDED".equalsIgnoreCase(order.getStatus()) && !"DELIVERED".equalsIgnoreCase(order.getStatus())) {
            orderStatusService.applyOrderStatus(order, "SHIPPED", "ADMIN", "Manual shipment added");
            orderStatusService.markFulfillment(order, "SHIPPED", "ADMIN", "Manual shipment in transit/shipped");
        }
        orderRepository.save(order);
        return s;
    }

    private static String nz(String s) { return s == null ? "" : s; }

    @PostMapping("/{orderNumber}/cj/submit")
    public Map<String, Object> submitToCj(@PathVariable String orderNumber, @RequestBody(required = false) Map<String, String> body) {
        String logistic = body == null ? null : body.get("logisticName");
        String from = body == null ? null : body.get("fromCountryCode");
        String remark = body == null ? null : body.get("remark");
        return cjFulfillmentService.submitOrderToCj(orderNumber, false, new CjFulfillmentService.CjSubmitOptions(logistic, from, remark));
    }

    @PostMapping("/{orderNumber}/cj/sync")
    public Map<String, Object> syncFromCj(@PathVariable String orderNumber) {
        return cjFulfillmentService.syncOrderFromCj(orderNumber);
    }

    @PostMapping("/cj/sync-pending")
    public Map<String, Object> syncPendingCj(@RequestBody(required = false) Map<String, Integer> body) {
        int limit = body != null && body.get("limit") != null ? body.get("limit") : 25;
        return cjFulfillmentService.syncPendingCjOrders(limit);
    }
}
