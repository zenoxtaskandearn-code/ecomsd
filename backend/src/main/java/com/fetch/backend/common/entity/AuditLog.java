package com.fetch.backend.common.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "actor_type") private String actorType;
    @Column(name = "actor_id") private String actorId;
    @Column(nullable = false) private String action;
    @Column(name = "entity_type") private String entityType;
    @Column(name = "entity_id") private String entityId;
    @Column(name = "order_id") private Long orderId;
    @Column(nullable = false) private String severity = "INFO";
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "details_json", columnDefinition = "jsonb") private String detailsJson;
    @Column(name = "ip_address") private String ipAddress;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @PrePersist void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public String getActorType() { return actorType; } public void setActorType(String actorType) { this.actorType = actorType; }
    public String getActorId() { return actorId; } public void setActorId(String actorId) { this.actorId = actorId; }
    public String getAction() { return action; } public void setAction(String action) { this.action = action; }
    public String getEntityType() { return entityType; } public void setEntityType(String entityType) { this.entityType = entityType; }
    public String getEntityId() { return entityId; } public void setEntityId(String entityId) { this.entityId = entityId; }
    public Long getOrderId() { return orderId; } public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getSeverity() { return severity; } public void setSeverity(String severity) { this.severity = severity; }
    public String getDetailsJson() { return detailsJson; } public void setDetailsJson(String detailsJson) { this.detailsJson = detailsJson; }
    public String getIpAddress() { return ipAddress; } public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public Instant getCreatedAt() { return createdAt; }
}
