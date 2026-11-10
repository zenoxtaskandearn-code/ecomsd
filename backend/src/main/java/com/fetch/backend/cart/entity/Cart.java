package com.fetch.backend.cart.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity @Table(name = "carts")
public class Cart {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "user_id") private Long userId;
    @Column(nullable = false) private String status = "ACTIVE";
    @Column(nullable = false, length = 3) private String currency = "USD";
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public Long getUserId() { return userId; } public void setUserId(Long userId) { this.userId = userId; }
    public String getStatus() { return status; } public void setStatus(String status) { this.status = status; }
    public String getCurrency() { return currency; } public void setCurrency(String currency) { this.currency = currency; }
}
