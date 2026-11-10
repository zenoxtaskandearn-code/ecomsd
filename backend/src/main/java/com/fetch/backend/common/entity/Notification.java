package com.fetch.backend.common.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id") private Long orderId;
    @Column(name = "user_id") private Long userId;
    @Column(nullable = false) private String type;
    @Column(nullable = false) private String channel = "EMAIL";
    private String recipient;
    private String subject;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "payload_json", columnDefinition = "jsonb") private String payloadJson;
    @Column(nullable = false) private String status = "PENDING";
    @Column(nullable = false) private int attempts = 0;
    @Column(name = "max_attempts", nullable = false) private int maxAttempts = 5;
    @Column(name = "dedupe_key") private String dedupeKey;
    @Column(name = "last_error", columnDefinition = "TEXT") private String lastError;
    @Column(name = "next_attempt_at") private Instant nextAttemptAt;
    @Column(name = "sent_at") private Instant sentAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; } public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getUserId() { return userId; } public void setUserId(Long userId) { this.userId = userId; }
    public String getType() { return type; } public void setType(String type) { this.type = type; }
    public String getChannel() { return channel; } public void setChannel(String channel) { this.channel = channel; }
    public String getRecipient() { return recipient; } public void setRecipient(String recipient) { this.recipient = recipient; }
    public String getSubject() { return subject; } public void setSubject(String subject) { this.subject = subject; }
    public String getPayloadJson() { return payloadJson; } public void setPayloadJson(String payloadJson) { this.payloadJson = payloadJson; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public int getAttempts() { return attempts; } public void setAttempts(int attempts) { this.attempts = attempts; }
    public int getMaxAttempts() { return maxAttempts; } public void setMaxAttempts(int maxAttempts) { this.maxAttempts = maxAttempts; }
    public String getDedupeKey() { return dedupeKey; } public void setDedupeKey(String dedupeKey) { this.dedupeKey = dedupeKey; }
    public String getLastError() { return lastError; } public void setLastError(String lastError) { this.lastError = lastError; }
    public Instant getNextAttemptAt() { return nextAttemptAt; } public void setNextAttemptAt(Instant nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }
    public Instant getSentAt() { return sentAt; } public void setSentAt(Instant sentAt) { this.sentAt = sentAt; }
}
