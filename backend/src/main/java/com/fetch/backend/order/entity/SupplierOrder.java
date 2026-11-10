package com.fetch.backend.order.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "supplier_orders")
public class SupplierOrder {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    @Column(name = "supplier_name", nullable = false)
    private String supplierName = "CJ";
    @Column(name = "supplier_order_id")
    private String supplierOrderId;
    @Column(name = "supplier_order_number")
    private String supplierOrderNumber;
    @Column(name = "submit_status", nullable = false)
    private String submitStatus = "PENDING";
    @Column(name = "idempotency_key")
    private String idempotencyKey;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "request_payload", columnDefinition = "jsonb")
    private String requestPayload;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "response_payload", columnDefinition = "jsonb")
    private String responsePayload;
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;
    @Column(name = "submitted_at")
    private Instant submittedAt;
    @Column(name = "last_synced_at")
    private Instant lastSyncedAt;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; } public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getSupplierName() { return supplierName; } public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getSupplierOrderId() { return supplierOrderId; } public void setSupplierOrderId(String supplierOrderId) { this.supplierOrderId = supplierOrderId; }
    public String getSupplierOrderNumber() { return supplierOrderNumber; } public void setSupplierOrderNumber(String supplierOrderNumber) { this.supplierOrderNumber = supplierOrderNumber; }
    public String getSubmitStatus() { return submitStatus; } public void setSubmitStatus(String submitStatus) { this.submitStatus = submitStatus; }
    public String getIdempotencyKey() { return idempotencyKey; } public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public String getRequestPayload() { return requestPayload; } public void setRequestPayload(String requestPayload) { this.requestPayload = requestPayload; }
    public String getResponsePayload() { return responsePayload; } public void setResponsePayload(String responsePayload) { this.responsePayload = responsePayload; }
    public String getLastError() { return lastError; } public void setLastError(String lastError) { this.lastError = lastError; }
    public Instant getSubmittedAt() { return submittedAt; } public void setSubmittedAt(Instant submittedAt) { this.submittedAt = submittedAt; }
    public Instant getLastSyncedAt() { return lastSyncedAt; } public void setLastSyncedAt(Instant lastSyncedAt) { this.lastSyncedAt = lastSyncedAt; }
    public Instant getCreatedAt() { return createdAt; } public Instant getUpdatedAt() { return updatedAt; }
}
