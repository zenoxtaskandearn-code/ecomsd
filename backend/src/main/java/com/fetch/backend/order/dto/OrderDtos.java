package com.fetch.backend.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public class OrderDtos {
    public record AddressSnapshot(
            @NotBlank String fullName,
            @NotBlank String line1,
            String line2,
            @NotBlank String city,
            @NotBlank String state,
            @NotBlank String postalCode,
            @NotBlank String countryCode,
            String phone
    ) {}

    public record CheckoutPreviewRequest(@Valid @NotNull AddressSnapshot shippingAddress, @Valid AddressSnapshot billingAddress) {}

    public record CheckoutPreviewResponse(
            String currency, BigDecimal subtotalAmount, BigDecimal shippingAmount, BigDecimal taxAmount,
            BigDecimal discountAmount, BigDecimal totalAmount, List<OrderItemResponse> items,
            List<String> warnings, String policyNote
    ) {}

    public record ShipmentResponse(
            String carrier, String trackingNumber, String trackingUrl, String status,
            String shippedAt, String deliveredAt, String lastTrackedAt, String source
    ) {}
    public record TimelineEvent(String key, String label, String at, boolean done, boolean current, String note) {}
    public record FulfillmentInfo(
            String supplierName, String fulfillmentStatus, String supplierOrderId,
            String submittedAt, String lastUpdatedAt, String expectedDelivery,
            String statusMessage, String lastError
    ) {}
    public record OrderItemResponse(String productName, String variantTitle, String sku, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {}
    public record OrderResponse(String orderNumber, String status, String currency, BigDecimal subtotalAmount, BigDecimal shippingAmount,
                                BigDecimal taxAmount, BigDecimal discountAmount, BigDecimal totalAmount, String customerEmail,
                                String placedAt, String paidAt, Map<String, Object> shippingAddress, List<OrderItemResponse> items,
                                List<ShipmentResponse> shipments, List<TimelineEvent> timeline, FulfillmentInfo fulfillment) {}

    public record CreateOrderRequest(@Valid @NotNull AddressSnapshot shippingAddress, @Valid AddressSnapshot billingAddress, String customerEmail, String idempotencyKey) {}
    public record TrackOrderRequest(@NotBlank String orderNumber, @NotBlank String trackingToken) {}
}
