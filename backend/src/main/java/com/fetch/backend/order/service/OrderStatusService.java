package com.fetch.backend.order.service;

import com.fetch.backend.order.entity.Order;
import com.fetch.backend.order.entity.OrderStatusHistory;
import com.fetch.backend.order.repo.OrderStatusHistoryRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class OrderStatusService {
    private final OrderStatusHistoryRepository historyRepository;

    public OrderStatusService(OrderStatusHistoryRepository historyRepository) {
        this.historyRepository = historyRepository;
    }

    public void record(Long orderId, String domain, String fromStatus, String toStatus, String source, String note) {
        if (orderId == null || toStatus == null || toStatus.isBlank()) return;
        String from = norm(fromStatus);
        String to = norm(toStatus);
        if (from != null && from.equals(to)) return;
        OrderStatusHistory h = new OrderStatusHistory();
        h.setOrderId(orderId);
        h.setDomain(norm(domain) == null ? "ORDER" : norm(domain));
        h.setFromStatus(from);
        h.setToStatus(to);
        h.setSource(norm(source));
        h.setNote(note);
        historyRepository.save(h);
    }

    public void applyOrderStatus(Order order, String toStatus, String source, String note) {
        String from = order.getStatus();
        String to = norm(toStatus);
        if (to == null) return;
        order.setStatus(to);
        syncDerivedStatuses(order, to);
        if (order.getId() != null) record(order.getId(), "ORDER", from, to, source, note);
    }

    public void syncDerivedStatuses(Order order, String orderStatus) {
        String st = norm(orderStatus);
        if (st == null) st = norm(order.getStatus());
        if (st == null) return;
        // Payment status
        switch (st) {
            case "PAID", "PROCESSING", "FULFILLMENT_SUBMITTED", "SHIPPED", "DELIVERED", "AWAITING_FULFILMENT", "SUPPLIER_REVIEW_REQUIRED", "SUBMITTED_TO_CJ", "SUPPLIER_CONFIRMED", "IN_TRANSIT", "OUT_FOR_DELIVERY" ->
                { if (!"REFUNDED".equals(norm(order.getPaymentStatus())) && !"PARTIALLY_REFUNDED".equals(norm(order.getPaymentStatus()))) order.setPaymentStatus("CAPTURED"); }
            case "PAYMENT_FAILED" -> order.setPaymentStatus("FAILED");
            case "PENDING_PAYMENT" -> order.setPaymentStatus("PENDING");
            case "CANCELLED" -> { if (!"CAPTURED".equals(norm(order.getPaymentStatus()))) order.setPaymentStatus("CANCELLED"); }
            case "REFUNDED" -> order.setPaymentStatus("REFUNDED");
            case "PARTIALLY_REFUNDED" -> order.setPaymentStatus("PARTIALLY_REFUNDED");
            default -> { }
        }
        // Fulfillment status
        switch (st) {
            case "FULFILLMENT_SUBMITTED", "SUBMITTED_TO_CJ" -> order.setFulfillmentStatus("SUBMITTED");
            case "PROCESSING", "AWAITING_FULFILMENT", "SUPPLIER_CONFIRMED" -> order.setFulfillmentStatus("PROCESSING");
            case "SUPPLIER_REVIEW_REQUIRED", "FULFILMENT_FAILED" -> order.setFulfillmentStatus("REVIEW_REQUIRED".equals(st) || "SUPPLIER_REVIEW_REQUIRED".equals(st) ? "REVIEW_REQUIRED" : "FAILED");
            case "SHIPPED" -> order.setFulfillmentStatus("SHIPPED");
            case "IN_TRANSIT", "OUT_FOR_DELIVERY" -> order.setFulfillmentStatus("IN_TRANSIT");
            case "DELIVERED" -> order.setFulfillmentStatus("DELIVERED");
            case "CANCELLED" -> order.setFulfillmentStatus("CANCELLED");
            case "PENDING_PAYMENT", "PAYMENT_FAILED" -> { if (norm(order.getFulfillmentStatus()) == null) order.setFulfillmentStatus("PENDING"); }
            default -> { }
        }
    }

    public void markPayment(Order order, String paymentStatus, String source, String note) {
        String from = order.getPaymentStatus();
        String to = norm(paymentStatus);
        if (to == null) return;
        order.setPaymentStatus(to);
        if (order.getId() != null) record(order.getId(), "PAYMENT", from, to, source, note);
    }

    public void markFulfillment(Order order, String fulfillmentStatus, String source, String note) {
        String from = order.getFulfillmentStatus();
        String to = norm(fulfillmentStatus);
        if (to == null) return;
        order.setFulfillmentStatus(to);
        if (order.getId() != null) record(order.getId(), "FULFILLMENT", from, to, source, note);
    }

    private static String norm(String s) {
        if (s == null) return null;
        String t = s.trim().toUpperCase(Locale.ROOT);
        return t.isEmpty() ? null : t;
    }
}
