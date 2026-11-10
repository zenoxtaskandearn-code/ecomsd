package com.fetch.backend.payment.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fetch.backend.common.error.BadRequestException;
import com.fetch.backend.common.error.NotFoundException;
import com.fetch.backend.order.entity.Order;
import com.fetch.backend.order.repo.OrderRepository;
import com.fetch.backend.payment.entity.Payment;
import com.fetch.backend.payment.entity.WebhookEvent;
import com.fetch.backend.payment.repo.PaymentRepository;
import com.fetch.backend.payment.repo.WebhookEventRepository;
import com.fetch.backend.order.cj.CjAutoSubmitter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

@Service
public class PaymentService {
    private final PayPalClient payPalClient;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final WebhookEventRepository webhookEventRepository;
    private final CjAutoSubmitter cjAutoSubmitter;

    public PaymentService(PayPalClient payPalClient, OrderRepository orderRepository,
                          PaymentRepository paymentRepository, WebhookEventRepository webhookEventRepository,
                          CjAutoSubmitter cjAutoSubmitter) {
        this.payPalClient = payPalClient;
        this.orderRepository = orderRepository;
        this.paymentRepository = paymentRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.cjAutoSubmitter = cjAutoSubmitter;
    }

    @Transactional
    public Map<String, Object> createPayPalOrder(String orderNumber, Long userId) {
        Order order = ownedPendingOrder(orderNumber, userId);
        payPalClient.requireConfigured();
        Payment payment = paymentRepository.findByOrderIdAndProvider(order.getId(), "PAYPAL").orElseGet(() -> {
            Payment p = new Payment();
            p.setOrderId(order.getId());
            p.setProvider("PAYPAL");
            p.setStatus("PENDING");
            p.setAmount(order.getTotalAmount());
            p.setCurrency(order.getCurrency());
            return p;
        });
        if ("CAPTURED".equals(payment.getStatus())) {
            return Map.of("paypalOrderId", payment.getProviderOrderId(), "status", "CAPTURED");
        }
        JsonNode node = payPalClient.createOrder(order.getOrderNumber(), order.getTotalAmount(), order.getCurrency());
        String paypalOrderId = node.path("id").asText();
        if (paypalOrderId == null || paypalOrderId.isBlank()) throw new BadRequestException("PayPal did not return an order id");
        payment.setProviderOrderId(paypalOrderId);
        payment.setStatus("PENDING");
        payment.setRawResponseRedacted("{\"id\":\"" + paypalOrderId + "\"}");
        paymentRepository.save(payment);
        return Map.of("paypalOrderId", paypalOrderId, "internalOrderNumber", order.getOrderNumber());
    }

    @Transactional
    public Map<String, Object> capturePayPalOrder(String orderNumber, String paypalOrderId, Long userId) {
        Order order = ownedPendingOrder(orderNumber, userId);
        Payment payment = paymentRepository.findByOrderIdAndProvider(order.getId(), "PAYPAL")
                .orElseThrow(() -> new BadRequestException("No PayPal payment intent found for this order"));
        if ("CAPTURED".equals(payment.getStatus()) && "PAID".equals(order.getStatus())) {
            return Map.of("orderNumber", order.getOrderNumber(), "status", order.getStatus(), "captureId", payment.getProviderCaptureId());
        }
        if (payment.getProviderOrderId() != null && !payment.getProviderOrderId().equals(paypalOrderId)) {
            throw new BadRequestException("PayPal order id does not match this internal order");
        }
        JsonNode capture = payPalClient.captureOrder(paypalOrderId);
        String status = capture.path("status").asText();
        String captureId = extractCaptureId(capture);
        if (!"COMPLETED".equalsIgnoreCase(status)) {
            payment.setStatus("FAILED");
            payment.setRawResponseRedacted("{\"status\":\"" + status + "\"}");
            paymentRepository.save(payment);
            order.setStatus("PAYMENT_FAILED");
            orderRepository.save(order);
            throw new BadRequestException("Payment was not completed");
        }
        // Financial safety: verify captured amount/currency match the server-side order total.
        verifyCaptureMatchesOrder(capture, order);
        applyCaptured(order, payment, captureId, "{\"status\":\"COMPLETED\"}");
        return Map.of("orderNumber", order.getOrderNumber(), "status", order.getStatus(), "captureId", captureId);
    }

    @Transactional
    public void handleWebhook(Map<String, String> headers, String rawBody) {
        // Verify signature first (requires configured webhook id). Throws if verification fails.
        if (!payPalClient.verifyWebhookSignature(headers, rawBody)) {
            throw new BadRequestException("Invalid PayPal webhook signature");
        }
        // Parse after verification for idempotent processing.
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            JsonNode event = om.readTree(rawBody);
            String eventId = event.path("id").asText();
            String eventType = event.path("event_type").asText();
            if (eventId == null || eventId.isBlank()) throw new BadRequestException("Missing webhook event id");
            if (webhookEventRepository.findByProviderAndEventId("PAYPAL", eventId).isPresent()) {
                return; // idempotent ignore
            }
            WebhookEvent record = new WebhookEvent();
            record.setProvider("PAYPAL");
            record.setEventId(eventId);
            record.setEventType(eventType);
            record.setPayloadHash(Integer.toHexString(rawBody.hashCode()));
            if (eventType != null && eventType.contains("CAPTURE.COMPLETED")) {
                String paypalOrderId = event.at("/resource/supplementary_data/related_ids/order_id").asText(null);
                String captureId = event.path("resource").path("id").asText(null);
                String currency = event.at("/resource/amount/currency_code").asText(null);
                String value = event.at("/resource/amount/value").asText(null);
                if (paypalOrderId != null) {
                    paymentRepository.findByProviderOrderId(paypalOrderId).ifPresent(payment -> {
                        orderRepository.findById(payment.getOrderId()).ifPresent(order -> {
                            boolean mismatch = false;
                            String mismatchMsg = null;
                            if (currency != null && order.getCurrency() != null && !currency.equalsIgnoreCase(order.getCurrency())) {
                                mismatch = true; mismatchMsg = "Webhook capture currency mismatch";
                            }
                            if (!mismatch && value != null && order.getTotalAmount() != null) {
                                BigDecimal v = new BigDecimal(value);
                                if (v.setScale(2, java.math.RoundingMode.HALF_UP).compareTo(order.getTotalAmount().setScale(2, java.math.RoundingMode.HALF_UP)) != 0) {
                                    mismatch = true; mismatchMsg = "Webhook capture amount mismatch";
                                }
                            }
                            if (mismatch) {
                                record.setOrderId(order.getId());
                                record.setPaymentId(payment.getId());
                                record.setProcessStatus("FAILED");
                                record.setErrorMessage(mismatchMsg);
                                record.setProcessedAt(Instant.now());
                                webhookEventRepository.save(record);
                                return; // acknowledge but do not mark paid / do not fulfill
                            }
                            applyCaptured(order, payment, captureId, "{\"event\":\"CAPTURE.COMPLETED\"}");
                            record.setOrderId(order.getId());
                            record.setPaymentId(payment.getId());
                        });
                    });
                }
            }
            record.setProcessStatus("PROCESSED");
            record.setProcessedAt(Instant.now());
            webhookEventRepository.save(record);
        } catch (BadRequestException e) { throw e; }
        catch (Exception e) { throw new BadRequestException("Unable to process webhook"); }
    }

    private void applyCaptured(Order order, Payment payment, String captureId, String redacted) {
        if ("CAPTURED".equals(payment.getStatus()) && "PAID".equals(order.getStatus())) return;
        payment.setProviderCaptureId(captureId);
        payment.setStatus("CAPTURED");
        payment.setRawResponseRedacted(redacted);
        paymentRepository.save(payment);
        order.setStatus("PAID");
        order.setPaidAt(Instant.now());
        if (order.getPlacedAt() == null) order.setPlacedAt(Instant.now());
        orderRepository.save(order);
        cjAutoSubmitter.onOrderPaid(order.getOrderNumber());
    }

    private Order ownedPendingOrder(String orderNumber, Long userId) {
        Order order = orderRepository.findByOrderNumber(orderNumber).orElseThrow(() -> new NotFoundException("Order not found"));
        if (userId != null && order.getUserId() != null && !order.getUserId().equals(userId)) {
            throw new NotFoundException("Order not found");
        }
        String st = order.getStatus() == null ? "" : order.getStatus().toUpperCase();
        if ("PAID".equals(st) || "CANCELLED".equals(st) || "REFUNDED".equals(st) || "PARTIALLY_REFUNDED".equals(st)) {
            return order; // idempotent callers will handle
        }
        if (!"PENDING_PAYMENT".equals(st) && !"PAYMENT_FAILED".equals(st)) {
            // allow re-capture attempts only for payment-awaiting states
            throw new BadRequestException("Order is not awaiting payment");
        }
        return order;
    }

    private void verifyCaptureMatchesOrder(JsonNode capture, Order order) {
        try {
            JsonNode cap = capture.at("/purchase_units/0/payments/captures/0");
            if (cap.isMissingNode() || cap.isNull()) cap = capture.at("/purchase_units/0/payments/captures").isArray() && capture.at("/purchase_units/0/payments/captures").size() > 0
                    ? capture.at("/purchase_units/0/payments/captures/0") : capture;
            String currency = cap.at("/amount/currency_code").asText(null);
            if (currency == null) currency = capture.at("/purchase_units/0/amount/currency_code").asText(null);
            String value = cap.at("/amount/value").asText(null);
            if (value == null) value = capture.at("/purchase_units/0/amount/value").asText(null);
            if (currency != null && order.getCurrency() != null && !currency.equalsIgnoreCase(order.getCurrency())) {
                throw new BadRequestException("Captured currency does not match order currency");
            }
            if (value != null) {
                BigDecimal captured = new BigDecimal(value);
                BigDecimal expected = order.getTotalAmount() == null ? BigDecimal.ZERO : order.getTotalAmount();
                if (captured.setScale(2, java.math.RoundingMode.HALF_UP).compareTo(expected.setScale(2, java.math.RoundingMode.HALF_UP)) != 0) {
                    throw new BadRequestException("Captured amount does not match order total");
                }
            }
        } catch (BadRequestException e) {
            throw e;
        } catch (Exception e) {
            throw new BadRequestException("Unable to verify captured payment amount");
        }
    }

    private String extractCaptureId(JsonNode capture) {
        JsonNode captures = capture.at("/purchase_units/0/payments/captures");
        if (captures.isArray() && captures.size() > 0) return captures.get(0).path("id").asText(null);
        return null;
    }
}
