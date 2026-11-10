package com.fetch.backend.order.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_items")
public class OrderItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id", nullable = false)
    private Long orderId;
    @Column(name = "product_variant_id")
    private Long productVariantId;
    @Column(name = "product_name_snapshot", nullable = false)
    private String productNameSnapshot;
    @Column(name = "variant_title_snapshot")
    private String variantTitleSnapshot;
    @Column(name = "sku_snapshot", nullable = false)
    private String skuSnapshot;
    @Column(name = "unit_price_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPriceAmount;
    @Column(nullable = false)
    private int quantity;
    @Column(name = "line_total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotalAmount;
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
    @PrePersist void onCreate() { createdAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getProductVariantId() { return productVariantId; }
    public void setProductVariantId(Long productVariantId) { this.productVariantId = productVariantId; }
    public String getProductNameSnapshot() { return productNameSnapshot; }
    public void setProductNameSnapshot(String productNameSnapshot) { this.productNameSnapshot = productNameSnapshot; }
    public String getVariantTitleSnapshot() { return variantTitleSnapshot; }
    public void setVariantTitleSnapshot(String variantTitleSnapshot) { this.variantTitleSnapshot = variantTitleSnapshot; }
    public String getSkuSnapshot() { return skuSnapshot; }
    public void setSkuSnapshot(String skuSnapshot) { this.skuSnapshot = skuSnapshot; }
    public BigDecimal getUnitPriceAmount() { return unitPriceAmount; }
    public void setUnitPriceAmount(BigDecimal unitPriceAmount) { this.unitPriceAmount = unitPriceAmount; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getLineTotalAmount() { return lineTotalAmount; }
    public void setLineTotalAmount(BigDecimal lineTotalAmount) { this.lineTotalAmount = lineTotalAmount; }
}
