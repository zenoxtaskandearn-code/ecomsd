package com.fetch.backend.order.service;

import com.fetch.backend.order.entity.Order;
import com.fetch.backend.order.entity.OrderCost;
import com.fetch.backend.order.entity.OrderItem;
import com.fetch.backend.order.repo.OrderCostRepository;
import com.fetch.backend.order.repo.OrderItemRepository;
import com.fetch.backend.catalog.entity.ProductVariant;
import com.fetch.backend.catalog.repo.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
public class OrderCostService {
    private final OrderCostRepository orderCostRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository variantRepository;

    public OrderCostService(OrderCostRepository orderCostRepository, OrderItemRepository orderItemRepository, ProductVariantRepository variantRepository) {
        this.orderCostRepository = orderCostRepository;
        this.orderItemRepository = orderItemRepository;
        this.variantRepository = variantRepository;
    }

    @Transactional
    public OrderCost upsertForOrder(Order order) {
        OrderCost cost = orderCostRepository.findByOrderId(order.getId()).orElseGet(OrderCost::new);
        cost.setOrderId(order.getId());
        cost.setCurrency(nzStr(order.getCurrency(), "USD"));
        cost.setCustomerSubtotal(nz(order.getSubtotalAmount()));
        cost.setCustomerShipping(nz(order.getShippingAmount()));
        cost.setCustomerDiscount(nz(order.getDiscountAmount()));
        cost.setCustomerTax(nz(order.getTaxAmount()).add(nz(order.getCustomerTaxAmount())));
        cost.setCustomerTotal(nz(order.getTotalAmount()));
        cost.setRefundAmount(nz(order.getRefundTotalAmount()));

        BigDecimal supplierProduct = BigDecimal.ZERO;
        BigDecimal supplierShipping = nz(cost.getSupplierShippingCost());
        try {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            for (OrderItem it : items) {
                if (it.getProductVariantId() == null) continue;
                ProductVariant v = variantRepository.findById(it.getProductVariantId()).orElse(null);
                if (v != null) {
                    if (v.getSupplierCostAmount() != null) {
                        supplierProduct = supplierProduct.add(v.getSupplierCostAmount().multiply(BigDecimal.valueOf(Math.max(0, it.getQuantity()))));
                    }
                    // if per-variant fulfillment/shipping cost is modeled, accumulate only when not already set explicitly
                    if (supplierShipping.compareTo(BigDecimal.ZERO) == 0 && v.getShippingCostAmount() != null) {
                        supplierShipping = supplierShipping.add(v.getShippingCostAmount().multiply(BigDecimal.valueOf(Math.max(0, it.getQuantity()))));
                    }
                }
            }
        } catch (Exception ignored) {}
        cost.setSupplierProductCost(scale(supplierProduct));
        cost.setSupplierShippingCost(scale(supplierShipping));

        // Known/estimated fees not captured from provider in current integration are left as 0 unless set elsewhere.
        BigDecimal netRevenue = nz(cost.getCustomerTotal()).subtract(nz(cost.getRefundAmount()));
        BigDecimal supplierTotal = nz(cost.getSupplierProductCost()).add(nz(cost.getSupplierShippingCost())).add(nz(cost.getSupplierTaxDutyCost())).add(nz(cost.getSupplierOtherCost())).add(nz(cost.getCurrencyConversionCost()));
        BigDecimal gross = nz(cost.getCustomerSubtotal()).subtract(nz(cost.getCustomerDiscount())).subtract(nz(cost.getSupplierProductCost()));
        BigDecimal contribution = netRevenue.subtract(supplierTotal).subtract(nz(cost.getPaymentProcessingFee()));
        BigDecimal estimatedNet = contribution.subtract(nz(cost.getOtherBusinessCost()));
        cost.setNetRevenue(scale(netRevenue));
        cost.setGrossProfit(scale(gross));
        cost.setContributionProfit(scale(contribution));
        cost.setEstimatedNetProfit(scale(estimatedNet));
        return orderCostRepository.save(cost);
    }

    private static BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
    private static String nzStr(String v, String d) { return v == null || v.isBlank() ? d : v; }
    private static BigDecimal scale(BigDecimal v) { return nz(v).setScale(2, RoundingMode.HALF_UP); }
}
