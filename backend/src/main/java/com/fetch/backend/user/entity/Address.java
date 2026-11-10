package com.fetch.backend.user.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "addresses")
public class Address {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "user_id", nullable = false)
    private Long userId;
    @Column(nullable = false)
    private String type = "SHIPPING";
    @Column(name = "full_name")
    private String fullName;
    @Column(nullable = false)
    private String line1;
    private String line2;
    @Column(nullable = false)
    private String city;
    @Column(nullable = false)
    private String state;
    @Column(name = "postal_code", nullable = false)
    private String postalCode;
    @Column(name = "country_code", nullable = false, columnDefinition = "char(2)")
    private String countryCode = "US";
    private String phone;
    @Column(name = "is_default", nullable = false)
    private boolean defaultAddress = false;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getUserId() { return userId; } public void setUserId(Long userId) { this.userId = userId; }
    public String getType() { return type; } public void setType(String type) { this.type = type; }
    public String getFullName() { return fullName; } public void setFullName(String fullName) { this.fullName = fullName; }
    public String getLine1() { return line1; } public void setLine1(String line1) { this.line1 = line1; }
    public String getLine2() { return line2; } public void setLine2(String line2) { this.line2 = line2; }
    public String getCity() { return city; } public void setCity(String city) { this.city = city; }
    public String getState() { return state; } public void setState(String state) { this.state = state; }
    public String getPostalCode() { return postalCode; } public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public String getCountryCode() { return countryCode; } public void setCountryCode(String countryCode) { this.countryCode = countryCode; }
    public String getPhone() { return phone; } public void setPhone(String phone) { this.phone = phone; }
    public boolean isDefaultAddress() { return defaultAddress; } public void setDefaultAddress(boolean defaultAddress) { this.defaultAddress = defaultAddress; }
}
