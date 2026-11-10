package com.fetch.backend.payment.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "webhook_events")
public class WebhookEvent {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false) private String provider;
    @Column(name = "event_id", nullable = false) private String eventId;
    @Column(name = "event_type") private String eventType;
    @Column(name = "order_id") private Long orderId;
    @Column(name = "payment_id") private Long paymentId;
    @Column(name = "payload_hash") private String payloadHash;
    @Column(name = "received_at", nullable = false) private Instant receivedAt;
    @Column(name = "processed_at") private Instant processedAt;
    @Column(name = "process_status", nullable = false) private String processStatus; // RECEIVED, PROCESSED, IGNORED, FAILED
    @Column(name = "error_message", columnDefinition = "TEXT") private String errorMessage;
    @PrePersist void onCreate() { if (receivedAt == null) receivedAt = Instant.now(); if (processStatus == null) processStatus = "RECEIVED"; }

    public Long getId() { return id; }
    public void setProvider(String provider) { this.provider = provider; }
    public void setEventId(String eventId) { this.eventId = eventId; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }
    public void setPayloadHash(String payloadHash) { this.payloadHash = payloadHash; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    public String getProcessStatus() { return processStatus; }
    public void setProcessStatus(String processStatus) { this.processStatus = processStatus; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
