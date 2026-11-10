package com.fetch.backend.order.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "order_costs")
public class OrderCost {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "order_id", nullable = false, unique = true)
    private Long orderId;
    @Column(nullable = false, length = 3)
    private String currency = "USD";
    @Column(name = "customer_subtotal", nullable = false, precision = 12, scale = 2) private BigDecimal customerSubtotal = BigDecimal.ZERO;
    @Column(name = "customer_shipping", nullable = false, precision = 12, scale = 2) private BigDecimal customerShipping = BigDecimal.ZERO;
    @Column(name = "customer_discount", nullable = false, precision = 12, scale = 2) private BigDecimal customerDiscount = BigDecimal.ZERO;
    @Column(name = "customer_tax", nullable = false, precision = 12, scale = 2) private BigDecimal customerTax = BigDecimal.ZERO;
    @Column(name = "customer_total", nullable = false, precision = 12, scale = 2) private BigDecimal customerTotal = BigDecimal.ZERO;
    @Column(name = "payment_processing_fee", nullable = false, precision = 12, scale = 2) private BigDecimal paymentProcessingFee = BigDecimal.ZERO;
    @Column(name = "refund_amount", nullable = false, precision = 12, scale = 2) private BigDecimal refundAmount = BigDecimal.ZERO;
    @Column(name = "supplier_product_cost", nullable = false, precision = 12, scale = 2) private BigDecimal supplierProductCost = BigDecimal.ZERO;
    @Column(name = "supplier_shipping_cost", nullable = false, precision = 12, scale = 2) private BigDecimal supplierShippingCost = BigDecimal.ZERO;
    @Column(name = "supplier_tax_duty_cost", nullable = false, precision = 12, scale = 2) private BigDecimal supplierTaxDutyCost = BigDecimal.ZERO;
    @Column(name = "supplier_other_cost", nullable = false, precision = 12, scale = 2) private BigDecimal supplierOtherCost = BigDecimal.ZERO;
    @Column(name = "currency_conversion_cost", nullable = false, precision = 12, scale = 2) private BigDecimal currencyConversionCost = BigDecimal.ZERO;
    @Column(name = "other_business_cost", nullable = false, precision = 12, scale = 2) private BigDecimal otherBusinessCost = BigDecimal.ZERO;
    @Column(name = "net_revenue", precision = 12, scale = 2) private BigDecimal netRevenue;
    @Column(name = "gross_profit", precision = 12, scale = 2) private BigDecimal grossProfit;
    @Column(name = "contribution_profit", precision = 12, scale = 2) private BigDecimal contributionProfit;
    @Column(name = "estimated_net_profit", precision = 12, scale = 2) private BigDecimal estimatedNetProfit;
    @Column(name = "original_currency") private String originalCurrency;
    @Column(name = "fx_rate", precision = 18, scale = 8) private BigDecimal fxRate;
    @JdbcTypeCode(SqlTypes.JSON) @Column(name = "metadata_json", columnDefinition = "jsonb") private String metadataJson;
    @Column(name = "created_at", nullable = false, updatable = false) private Instant createdAt;
    @Column(name = "updated_at", nullable = false) private Instant updatedAt;
    @PrePersist void onCreate() { Instant now = Instant.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = Instant.now(); }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; } public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getCurrency() { return currency; } public void setCurrency(String currency) { this.currency = currency; }
    public BigDecimal getCustomerSubtotal() { return customerSubtotal; } public void setCustomerSubtotal(BigDecimal v) { this.customerSubtotal = nz(v); }
    public BigDecimal getCustomerShipping() { return customerShipping; } public void setCustomerShipping(BigDecimal v) { this.customerShipping = nz(v); }
    public BigDecimal getCustomerDiscount() { return customerDiscount; } public void setCustomerDiscount(BigDecimal v) { this.customerDiscount = nz(v); }
    public BigDecimal getCustomerTax() { return customerTax; } public void setCustomerTax(BigDecimal v) { this.customerTax = nz(v); }
    public BigDecimal getCustomerTotal() { return customerTotal; } public void setCustomerTotal(BigDecimal v) { this.customerTotal = nz(v); }
    public BigDecimal getPaymentProcessingFee() { return paymentProcessingFee; } public void setPaymentProcessingFee(BigDecimal v) { this.paymentProcessingFee = nz(v); }
    public BigDecimal getRefundAmount() { return refundAmount; } public void setRefundAmount(BigDecimal v) { this.refundAmount = nz(v); }
    public BigDecimal getSupplierProductCost() { return supplierProductCost; } public void setSupplierProductCost(BigDecimal v) { this.supplierProductCost = nz(v); }
    public BigDecimal getSupplierShippingCost() { return supplierShippingCost; } public void setSupplierShippingCost(BigDecimal v) { this.supplierShippingCost = nz(v); }
    public BigDecimal getSupplierTaxDutyCost() { return supplierTaxDutyCost; } public void setSupplierTaxDutyCost(BigDecimal v) { this.supplierTaxDutyCost = nz(v); }
    public BigDecimal getSupplierOtherCost() { return supplierOtherCost; } public void setSupplierOtherCost(BigDecimal v) { this.supplierOtherCost = nz(v); }
    public BigDecimal getCurrencyConversionCost() { return currencyConversionCost; } public void setCurrencyConversionCost(BigDecimal v) { this.currencyConversionCost = nz(v); }
    public BigDecimal getOtherBusinessCost() { return otherBusinessCost; } public void setOtherBusinessCost(BigDecimal v) { this.otherBusinessCost = nz(v); }
    public BigDecimal getNetRevenue() { return netRevenue; } public void setNetRevenue(BigDecimal netRevenue) { this.netRevenue = netRevenue; }
    public BigDecimal getGrossProfit() { return grossProfit; } public void setGrossProfit(BigDecimal grossProfit) { this.grossProfit = grossProfit; }
    public BigDecimal getContributionProfit() { return contributionProfit; } public void setContributionProfit(BigDecimal contributionProfit) { this.contributionProfit = contributionProfit; }
    public BigDecimal getEstimatedNetProfit() { return estimatedNetProfit; } public void setEstimatedNetProfit(BigDecimal estimatedNetProfit) { this.estimatedNetProfit = estimatedNetProfit; }
    public String getOriginalCurrency() { return originalCurrency; } public void setOriginalCurrency(String originalCurrency) { this.originalCurrency = originalCurrency; }
    public BigDecimal getFxRate() { return fxRate; } public void setFxRate(BigDecimal fxRate) { this.fxRate = fxRate; }
    public String getMetadataJson() { return metadataJson; } public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
