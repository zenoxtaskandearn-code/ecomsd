package com.fetch.backend.order.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "refunds")
public class Refund {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id", nullable = false) private Long orderId;
    @Column(name = "payment_id") private Long paymentId;
    @Column(name = "return_request_id") private Long returnRequestId;
    @Column(nullable = false) private String provider = "PAYPAL";
    @Column(name = "provider_refund_id") private String providerRefundId;
    @Column(name = "idempotency_key") private String idempotencyKey;
    @Column(nullable = false) private String status = "PENDING";
    @Column(nullable = false, precision = 12, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 3) private String currency = "USD";
    @Column(columnDefinition = "TEXT") private String reason;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "raw_response", columnDefinition = "jsonb") private String rawResponse;
    @Column(name = "requested_by") private String requestedBy;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; } public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getPaymentId() { return paymentId; } public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public Long getReturnRequestId() { return returnRequestId; } public void setReturnRequestId(Long returnRequestId) { this.returnRequestId = returnRequestId; }
    public String getProvider() { return provider; } public void setProvider(String provider) { this.provider = provider; }
    public String getProviderRefundId() { return providerRefundId; } public void setProviderRefundId(String providerRefundId) { this.providerRefundId = providerRefundId; }
    public String getIdempotencyKey() { return idempotencyKey; } public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public BigDecimal getAmount() { return amount; } public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; } public void setCurrency(String currency) { this.currency = currency; }
    public String getReason() { return reason; } public void setReason(String reason) { this.reason = reason; }
    public String getRawResponse() { return rawResponse; } public void setRawResponse(String rawResponse) { this.rawResponse = rawResponse; }
    public String getRequestedBy() { return requestedBy; } public void setRequestedBy(String requestedBy) { this.requestedBy = requestedBy; }
}
