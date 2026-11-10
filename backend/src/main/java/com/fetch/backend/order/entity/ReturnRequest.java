package com.fetch.backend.order.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

@Entity
@Table(name = "return_requests")
public class ReturnRequest {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id", nullable = false) private Long orderId;
    @Column(name = "user_id") private Long userId;
    @Column(nullable = false) private String status = "REQUESTED";
    private String reason;
    @Column(columnDefinition = "TEXT") private String description;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "evidence_json", columnDefinition = "jsonb") private String evidenceJson;
    @Column(name = "admin_notes", columnDefinition = "TEXT") private String adminNotes;
    @Column(name = "requested_at", nullable = false) private Instant requestedAt;
    @Column(name = "reviewed_at") private Instant reviewedAt;
    @Column(name = "resolved_at") private Instant resolvedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void onCreate() { Instant now = Instant.now(); if (requestedAt == null) requestedAt = now; createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; } public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getUserId() { return userId; } public void setUserId(Long userId) { this.userId = userId; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public String getReason() { return reason; } public void setReason(String reason) { this.reason = reason; }
    public String getDescription() { return description; } public void setDescription(String description) { this.description = description; }
    public String getEvidenceJson() { return evidenceJson; } public void setEvidenceJson(String evidenceJson) { this.evidenceJson = evidenceJson; }
    public String getAdminNotes() { return adminNotes; } public void setAdminNotes(String adminNotes) { this.adminNotes = adminNotes; }
    public Instant getRequestedAt() { return requestedAt; } public void setRequestedAt(Instant requestedAt) { this.requestedAt = requestedAt; }
    public Instant getReviewedAt() { return reviewedAt; } public void setReviewedAt(Instant reviewedAt) { this.reviewedAt = reviewedAt; }
    public Instant getResolvedAt() { return resolvedAt; } public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }
}
