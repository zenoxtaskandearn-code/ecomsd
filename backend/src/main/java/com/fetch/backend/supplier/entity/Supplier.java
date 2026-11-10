package com.fetch.backend.supplier.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "suppliers")
public class Supplier {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, unique = true)
    private String code;
    @Column(name = "contact_email")
    private String contactEmail;
    @Column(nullable = false)
    private String status = "ACTIVE";
    @Column(name = "api_mode", nullable = false)
    private String apiMode = "MANUAL";
    @Column(columnDefinition = "TEXT")
    private String notes;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public String getName() { return name; } public void setName(String name) { this.name = name; }
    public String getCode() { return code; } public void setCode(String code) { this.code = code; }
    public String getContactEmail() { return contactEmail; } public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public String getApiMode() { return apiMode; } public void setApiMode(String apiMode) { this.apiMode = apiMode; }
    public String getNotes() { return notes; } public void setNotes(String notes) { this.notes = notes; }
}
