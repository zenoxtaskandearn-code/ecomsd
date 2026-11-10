package com.fetch.backend.catalog.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "product_variants")
public class ProductVariant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;
    @Column(nullable = false, unique = true)
    private String sku;
    private String title;
    @Column(name = "option_values_json", columnDefinition = "JSON")
    private String optionValuesJson;
    @Column(name = "price_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal priceAmount;
    @Column(nullable = false, length = 3)
    private String currency = "USD";
    @Column(name = "cj_variant_id", length = 100)
    private String cjVariantId;
    @Column(name = "supplier_sku", length = 150)
    private String supplierSku;
    @Column(name = "supplier_cost_amount", precision = 12, scale = 2)
    private BigDecimal supplierCostAmount;
    @Column(name = "shipping_cost_amount", precision = 12, scale = 2)
    private BigDecimal shippingCostAmount;
    @Column(name = "weight_grams")
    private Integer weightGrams;
    @Column(name = "stock_availability", length = 120)
    private String stockAvailability;
    @Column(name = "warehouse_info")
    private String warehouseInfo;
    @Column(name = "dimensions_json", columnDefinition = "TEXT")
    private String dimensionsJson;
    @Column(name = "is_active", nullable = false)
    private boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Product getProduct() { return product; }
    public void setProduct(Product product) { this.product = product; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getOptionValuesJson() { return optionValuesJson; }
    public void setOptionValuesJson(String optionValuesJson) { this.optionValuesJson = optionValuesJson; }
    public BigDecimal getPriceAmount() { return priceAmount; }
    public void setPriceAmount(BigDecimal priceAmount) { this.priceAmount = priceAmount; }
    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getCjVariantId() { return cjVariantId; }
    public void setCjVariantId(String cjVariantId) { this.cjVariantId = cjVariantId; }
    public String getSupplierSku() { return supplierSku; }
    public void setSupplierSku(String supplierSku) { this.supplierSku = supplierSku; }
    public BigDecimal getSupplierCostAmount() { return supplierCostAmount; }
    public void setSupplierCostAmount(BigDecimal supplierCostAmount) { this.supplierCostAmount = supplierCostAmount; }
    public BigDecimal getShippingCostAmount() { return shippingCostAmount; }
    public void setShippingCostAmount(BigDecimal shippingCostAmount) { this.shippingCostAmount = shippingCostAmount; }
    public Integer getWeightGrams() { return weightGrams; }
    public void setWeightGrams(Integer weightGrams) { this.weightGrams = weightGrams; }
    public String getStockAvailability() { return stockAvailability; }
    public void setStockAvailability(String stockAvailability) { this.stockAvailability = stockAvailability; }
    public String getWarehouseInfo() { return warehouseInfo; }
    public void setWarehouseInfo(String warehouseInfo) { this.warehouseInfo = warehouseInfo; }
    public String getDimensionsJson() { return dimensionsJson; }
    public void setDimensionsJson(String dimensionsJson) { this.dimensionsJson = dimensionsJson; }
}
