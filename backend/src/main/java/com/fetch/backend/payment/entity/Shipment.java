package com.fetch.backend.payment.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity @Table(name = "shipments")
public class Shipment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "order_id", nullable = false) private Long orderId;
    @Column(name = "supplier_id") private Long supplierId;
    private String source; // MANUAL, CJ
    private String carrier;
    @Column(name = "tracking_number") private String trackingNumber;
    @Column(name = "tracking_url", length = 1024) private String trackingUrl;
    @Column(nullable = false) private String status = "PENDING";
    @Column(name = "shipped_at") private Instant shippedAt;
    @Column(name = "delivered_at") private Instant deliveredAt;
    @Column(name = "cj_track_synced_at") private Instant cjTrackSyncedAt;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "cj_raw_snapshot", columnDefinition = "jsonb") private String cjRawSnapshot;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public Long getOrderId() { return orderId; } public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getSupplierId() { return supplierId; } public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getSource() { return source; } public void setSource(String source) { this.source = source; }
    public String getCarrier() { return carrier; } public void setCarrier(String carrier) { this.carrier = carrier; }
    public String getTrackingNumber() { return trackingNumber; } public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public String getTrackingUrl() { return trackingUrl; } public void setTrackingUrl(String trackingUrl) { this.trackingUrl = trackingUrl; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public Instant getShippedAt() { return shippedAt; } public void setShippedAt(Instant shippedAt) { this.shippedAt = shippedAt; }
    public Instant getDeliveredAt() { return deliveredAt; } public void setDeliveredAt(Instant deliveredAt) { this.deliveredAt = deliveredAt; }
    public Instant getCjTrackSyncedAt() { return cjTrackSyncedAt; } public void setCjTrackSyncedAt(Instant cjTrackSyncedAt) { this.cjTrackSyncedAt = cjTrackSyncedAt; }
    public String getCjRawSnapshot() { return cjRawSnapshot; } public void setCjRawSnapshot(String cjRawSnapshot) { this.cjRawSnapshot = cjRawSnapshot; }
}
