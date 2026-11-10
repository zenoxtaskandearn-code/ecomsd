package com.fetch.backend.order.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_number", nullable = false, unique = true)
    private String orderNumber;
    @Column(name = "user_id")
    private Long userId;
    @Column(nullable = false)
    private String status; // overall/order lifecycle status (kept for compatibility)
    @Column(name = "payment_status", nullable = false)
    private String paymentStatus = "PENDING"; // PENDING, FAILED, CAPTURED, REFUNDED, PARTIALLY_REFUNDED, CANCELLED
    @Column(name = "fulfillment_status", nullable = false)
    private String fulfillmentStatus = "PENDING"; // PENDING, REVIEW_REQUIRED, PROCESSING, SUBMITTED, SHIPPED, IN_TRANSIT, DELIVERED, CANCELLED, FAILED
    @Column(nullable = false, length = 3)
    private String currency = "USD";
    @Column(name = "subtotal_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotalAmount;
    @Column(name = "shipping_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal shippingAmount = BigDecimal.ZERO;
    @Column(name = "tax_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    @Column(name = "discount_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;
    @Column(name = "customer_email", nullable = false)
    private String customerEmail;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "shipping_address_snapshot", nullable = false, columnDefinition = "jsonb")
    private String shippingAddressSnapshot;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "billing_address_snapshot", columnDefinition = "jsonb")
    private String billingAddressSnapshot;
    @Column(name = "notes_internal", columnDefinition = "TEXT")
    private String notesInternal;
    @Column(name = "cj_order_id", length = 128)
    private String cjOrderId;
    @Column(name = "cj_order_number", length = 128)
    private String cjOrderNumber;
    @Column(name = "cj_fulfillment_status", length = 60)
    private String cjFulfillmentStatus;
    @Column(name = "cj_submitted_at")
    private Instant cjSubmittedAt;
    @Column(name = "cj_last_sync_at")
    private Instant cjLastSyncAt;
    @Column(name = "cj_last_error", columnDefinition = "TEXT")
    private String cjLastError;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cj_raw_snapshot", columnDefinition = "jsonb")
    private String cjRawSnapshot;
    @Column(name = "placed_at") private Instant placedAt;
    @Column(name = "paid_at") private Instant paidAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }
    public String getFulfillmentStatus() { return fulfillmentStatus; }
    public void setFulfillmentStatus(String fulfillmentStatus) { this.fulfillmentStatus = fulfillmentStatus; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getSubtotalAmount() { return subtotalAmount; }
    public void setSubtotalAmount(BigDecimal subtotalAmount) { this.subtotalAmount = subtotalAmount; }
    public BigDecimal getShippingAmount() { return shippingAmount; }
    public void setShippingAmount(BigDecimal shippingAmount) { this.shippingAmount = shippingAmount; }
    public BigDecimal getTaxAmount() { return taxAmount; }
    public void setTaxAmount(BigDecimal taxAmount) { this.taxAmount = taxAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getShippingAddressSnapshot() { return shippingAddressSnapshot; }
    public void setShippingAddressSnapshot(String shippingAddressSnapshot) { this.shippingAddressSnapshot = shippingAddressSnapshot; }
    public String getBillingAddressSnapshot() { return billingAddressSnapshot; }
    public void setBillingAddressSnapshot(String billingAddressSnapshot) { this.billingAddressSnapshot = billingAddressSnapshot; }
    public String getNotesInternal() { return notesInternal; }
    public void setNotesInternal(String notesInternal) { this.notesInternal = notesInternal; }
    public String getCjOrderId() { return cjOrderId; }
    public void setCjOrderId(String cjOrderId) { this.cjOrderId = cjOrderId; }
    public String getCjOrderNumber() { return cjOrderNumber; }
    public void setCjOrderNumber(String cjOrderNumber) { this.cjOrderNumber = cjOrderNumber; }
    public String getCjFulfillmentStatus() { return cjFulfillmentStatus; }
    public void setCjFulfillmentStatus(String cjFulfillmentStatus) { this.cjFulfillmentStatus = cjFulfillmentStatus; }
    public Instant getCjSubmittedAt() { return cjSubmittedAt; }
    public void setCjSubmittedAt(Instant cjSubmittedAt) { this.cjSubmittedAt = cjSubmittedAt; }
    public Instant getCjLastSyncAt() { return cjLastSyncAt; }
    public void setCjLastSyncAt(Instant cjLastSyncAt) { this.cjLastSyncAt = cjLastSyncAt; }
    public String getCjLastError() { return cjLastError; }
    public void setCjLastError(String cjLastError) { this.cjLastError = cjLastError; }
    public String getCjRawSnapshot() { return cjRawSnapshot; }
    public void setCjRawSnapshot(String cjRawSnapshot) { this.cjRawSnapshot = cjRawSnapshot; }
    public Instant getPlacedAt() { return placedAt; }
    public void setPlacedAt(Instant placedAt) { this.placedAt = placedAt; }
    public Instant getPaidAt() { return paidAt; }
    public void setPaidAt(Instant paidAt) { this.paidAt = paidAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
