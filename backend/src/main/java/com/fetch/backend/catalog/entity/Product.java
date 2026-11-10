package com.fetch.backend.catalog.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products")
public class Product {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, unique = true)
    private String slug;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String description;
    @Column(name = "base_price_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal basePriceAmount;
    @Column(nullable = false, length = 3)
    private String currency = "USD";
    @Column(nullable = false)
    private String status; // DRAFT, ACTIVE, ARCHIVED
    @Column(name = "seo_title")
    private String seoTitle;
    @Column(name = "seo_description", columnDefinition = "TEXT")
    private String seoDescription;
    @Column(name = "cj_product_id", length = 100)
    private String cjProductId;
    @Column(name = "supplier_source", length = 40)
    private String supplierSource;
    @Column(name = "supplier_id")
    private Long supplierId;
    @Column(name = "estimated_delivery")
    private String estimatedDelivery;
    @Column(name = "shipping_options_json", columnDefinition = "TEXT")
    private String shippingOptionsJson;
    @Column(name = "supplier_shipping_cost_amount", precision = 12, scale = 2)
    private BigDecimal supplierShippingCostAmount;
    @Column(name = "internal_notes", columnDefinition = "TEXT")
    private String internalNotes;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getBasePriceAmount() { return basePriceAmount; }
    public void setBasePriceAmount(BigDecimal basePriceAmount) { this.basePriceAmount = basePriceAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSeoTitle() { return seoTitle; }
    public void setSeoTitle(String seoTitle) { this.seoTitle = seoTitle; }
    public String getSeoDescription() { return seoDescription; }
    public void setSeoDescription(String seoDescription) { this.seoDescription = seoDescription; }
    public String getCjProductId() { return cjProductId; }
    public void setCjProductId(String cjProductId) { this.cjProductId = cjProductId; }
    public String getSupplierSource() { return supplierSource; }
    public void setSupplierSource(String supplierSource) { this.supplierSource = supplierSource; }
    public Long getSupplierId() { return supplierId; }
    public void setSupplierId(Long supplierId) { this.supplierId = supplierId; }
    public String getEstimatedDelivery() { return estimatedDelivery; }
    public void setEstimatedDelivery(String estimatedDelivery) { this.estimatedDelivery = estimatedDelivery; }
    public String getShippingOptionsJson() { return shippingOptionsJson; }
    public void setShippingOptionsJson(String shippingOptionsJson) { this.shippingOptionsJson = shippingOptionsJson; }
    public BigDecimal getSupplierShippingCostAmount() { return supplierShippingCostAmount; }
    public void setSupplierShippingCostAmount(BigDecimal supplierShippingCostAmount) { this.supplierShippingCostAmount = supplierShippingCostAmount; }
    public String getInternalNotes() { return internalNotes; }
    public void setInternalNotes(String internalNotes) { this.internalNotes = internalNotes; }
}
