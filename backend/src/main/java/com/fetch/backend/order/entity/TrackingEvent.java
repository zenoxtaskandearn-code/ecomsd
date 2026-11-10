package com.fetch.backend.order.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "tracking_events")
public class TrackingEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id", nullable = false) private Long orderId;
    @Column(name = "shipment_id") private Long shipmentId;
    @Column(nullable = false) private String source;
    private String status;
    @Column(columnDefinition = "TEXT") private String description;
    private String location;
    @Column(name = "event_time") private Instant eventTime;
    @Column(name = "tracking_number") private String trackingNumber;
    private String carrier;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "raw_payload", columnDefinition = "jsonb") private String rawPayload;
    @Column(name = "dedupe_key") private String dedupeKey;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @PrePersist void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; } public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getShipmentId() { return shipmentId; } public void setShipmentId(Long shipmentId) { this.shipmentId = shipmentId; }
    public String getSource() { return source; } public void setSource(String source) { this.source = source; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
    public String getLocation() { return location; } public void setLocation(String location) { this.location = location; }
    public Instant getEventTime() { return eventTime; } public void setEventTime(Instant eventTime) { this.eventTime = eventTime; }
    public String getTrackingNumber() { return trackingNumber; } public void setTrackingNumber(String trackingNumber) { this.trackingNumber = trackingNumber; }
    public String getCarrier() { return carrier; } public void setCarrier(String carrier) { this.carrier = carrier; }
    public String getRawPayload() { return rawPayload; } public void setRawPayload(String rawPayload) { this.rawPayload = rawPayload; }
    public String getDedupeKey() { return dedupeKey; } public void setDedupeKey(String dedupeKey) { this.dedupeKey = dedupeKey; }
    public Instant getCreatedAt() { return createdAt; }
}
