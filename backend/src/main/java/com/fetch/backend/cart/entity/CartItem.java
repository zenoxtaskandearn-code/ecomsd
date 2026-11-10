package com.fetch.backend.cart.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity @Table(name = "cart_items")
public class CartItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "cart_id", nullable = false) private Long cartId;
    @Column(name = "product_variant_id", nullable = false) private Long productVariantId;
    @Column(nullable = false) private int quantity;
    @Column(name = "unit_price_snapshot", nullable = false, precision = 12, scale = 2) private BigDecimal unitPriceSnapshot;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }
    public Long getId() { return id; }
    public Long getCartId() { return cartId; } public void setCartId(Long cartId) { this.cartId = cartId; }
    public Long getProductVariantId() { return productVariantId; } public void setProductVariantId(Long productVariantId) { this.productVariantId = productVariantId; }
    public int getQuantity() { return quantity; } public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPriceSnapshot() { return unitPriceSnapshot; } public void setUnitPriceSnapshot(BigDecimal unitPriceSnapshot) { this.unitPriceSnapshot = unitPriceSnapshot; }
}
