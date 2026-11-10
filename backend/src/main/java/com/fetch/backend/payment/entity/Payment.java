package com.fetch.backend.payment.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payments")
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    @Column(nullable = false)
    private String provider; // PAYPAL
    @Column(name = "provider_order_id")
    private String providerOrderId;
    @Column(name = "provider_capture_id")
    private String providerCaptureId;
    @Column(nullable = false)
    private String status; // PENDING, CAPTURED, FAILED, REFUNDED
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;
    @Column(nullable = false, length = 3)
    private String currency = "USD";
    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_response_redacted", columnDefinition = "jsonb")
    private String rawResponseRedacted;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getProviderOrderId() { return providerOrderId; }
    public void setProviderOrderId(String providerOrderId) { this.providerOrderId = providerOrderId; }
    public String getProviderCaptureId() { return providerCaptureId; }
    public void setProviderCaptureId(String providerCaptureId) { this.providerCaptureId = providerCaptureId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getRawResponseRedacted() { return rawResponseRedacted; }
    public void setRawResponseRedacted(String rawResponseRedacted) { this.rawResponseRedacted = rawResponseRedacted; }
}
