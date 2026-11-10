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

    public record ShipmentResponse(String carrier, String trackingNumber, String trackingUrl, String status, String shippedAt) {}
    public record OrderItemResponse(String productName, String variantTitle, String sku, BigDecimal unitPrice, int quantity, BigDecimal lineTotal) {}
    public record OrderResponse(String orderNumber, String status, String currency, BigDecimal subtotalAmount, BigDecimal shippingAmount,
                                BigDecimal taxAmount, BigDecimal discountAmount, BigDecimal totalAmount, String customerEmail,
                                String placedAt, String paidAt, Map<String, Object> shippingAddress, List<OrderItemResponse> items, List<ShipmentResponse> shipments) {}

    public record CreateOrderRequest(@Valid @NotNull AddressSnapshot shippingAddress, @Valid AddressSnapshot billingAddress, String customerEmail) {}
}
