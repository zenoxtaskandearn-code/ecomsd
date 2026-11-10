package com.fetch.backend.user.admin;

import com.fetch.backend.catalog.repo.ProductRepository;
import com.fetch.backend.catalog.repo.ProductVariantRepository;
import com.fetch.backend.order.entity.Order;
import com.fetch.backend.order.entity.OrderItem;
import com.fetch.backend.order.repo.OrderItemRepository;
import com.fetch.backend.order.repo.OrderRepository;
import com.fetch.backend.payment.repo.PaymentRepository;
import com.fetch.backend.user.repo.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminUserController {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductRepository productRepository;

    public AdminUserController(UserRepository userRepository, OrderRepository orderRepository, PaymentRepository paymentRepository,
                               OrderItemRepository orderItemRepository, ProductVariantRepository variantRepository, ProductRepository productRepository) {
        this.userRepository = userRepository; this.orderRepository = orderRepository; this.paymentRepository = paymentRepository;
        this.orderItemRepository = orderItemRepository; this.variantRepository = variantRepository; this.productRepository = productRepository;
    }

    @GetMapping("/customers")
    public List<Map<String, Object>> customers() {
        return userRepository.findAll().stream().map(u -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", u.getId()); m.put("name", ((u.getFirstName() == null ? "" : u.getFirstName()) + " " + (u.getLastName() == null ? "" : u.getLastName())).trim());
            m.put("email", u.getEmail()); m.put("role", u.getRole()); m.put("status", u.getStatus());
            var orders = orderRepository.findByUserIdOrderByCreatedAtDesc(u.getId());
            m.put("orders", orders.size());
            m.put("spent", orders.stream().map(o -> o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount()).reduce(BigDecimal.ZERO, BigDecimal::add));
            return m;
        }).toList();
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        List<Order> orders = orderRepository.findAll();
        var payments = paymentRepository.findAll();

        BigDecimal totalSales = payments.stream()
                .filter(p -> "CAPTURED".equalsIgnoreCase(p.getStatus()))
                .map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Set<String> paidLike = Set.of("PAID", "PROCESSING", "FULFILLMENT_SUBMITTED", "SHIPPED", "DELIVERED");
        Set<String> revenueStatuses = Set.of("PAID", "PROCESSING", "FULFILLMENT_SUBMITTED", "SHIPPED", "DELIVERED");
        Set<String> openStatuses = Set.of("PENDING_PAYMENT", "PAYMENT_FAILED");

        long totalOrders = orders.size();
        long paidOrders = orders.stream().filter(o -> revenueStatuses.contains(nz(o.getStatus()).toUpperCase(Locale.ROOT))).count();
        long pendingPaymentOrders = orders.stream().filter(o -> openStatuses.contains(nz(o.getStatus()).toUpperCase(Locale.ROOT))).count();
        long shippedOrders = orders.stream().filter(o -> "SHIPPED".equalsIgnoreCase(nz(o.getStatus()))).count();
        long deliveredOrders = orders.stream().filter(o -> "DELIVERED".equalsIgnoreCase(nz(o.getStatus()))).count();
        long cancelledOrders = orders.stream().filter(o -> "CANCELLED".equalsIgnoreCase(nz(o.getStatus())) || "REFUNDED".equalsIgnoreCase(nz(o.getStatus()))).count();
        long fulfillmentQueue = orders.stream().filter(o -> {
            String s = nz(o.getStatus()).toUpperCase(Locale.ROOT);
            return "PAID".equals(s) || "PROCESSING".equals(s);
        }).count();
        long cjSubmitted = orders.stream().filter(o -> o.getCjOrderId() != null && !o.getCjOrderId().isBlank()).count();
        long cjPending = orders.stream().filter(o -> revenueStatuses.contains(nz(o.getStatus()).toUpperCase(Locale.ROOT)) && (o.getCjOrderId() == null || o.getCjOrderId().isBlank())).count();
        long cjErrors = orders.stream().filter(o -> o.getCjLastError() != null && !o.getCjLastError().isBlank() && (o.getCjOrderId() == null || o.getCjOrderId().isBlank())).count();

        BigDecimal paidRevenue = orders.stream()
                .filter(o -> revenueStatuses.contains(nz(o.getStatus()).toUpperCase(Locale.ROOT)))
                .map(o -> o.getTotalAmount() == null ? BigDecimal.ZERO : o.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal avgOrderValue = paidOrders > 0 ? paidRevenue.divide(BigDecimal.valueOf(paidOrders), 2, RoundingMode.HALF_UP) : BigDecimal.ZERO;

        // Estimated profit from paid/revenue orders: (line total - supplier/shipping costs) per item when variant cost data is known.
        Set<Long> revenueOrderIds = new HashSet<>();
        for (Order o : orders) if (revenueStatuses.contains(nz(o.getStatus()).toUpperCase(Locale.ROOT))) revenueOrderIds.add(o.getId());

        BigDecimal estimatedCost = BigDecimal.ZERO;
        BigDecimal estimatedProfit = BigDecimal.ZERO;
        BigDecimal profitRevenueBase = BigDecimal.ZERO;
        int itemsSold = 0;
        int itemsWithCost = 0;

        if (!revenueOrderIds.isEmpty()) {
            List<OrderItem> items = orderItemRepository.findAll().stream().filter(i -> revenueOrderIds.contains(i.getOrderId())).toList();
            Set<Long> variantIds = new HashSet<>();
            for (OrderItem i : items) if (i.getProductVariantId() != null) variantIds.add(i.getProductVariantId());
            Map<Long, com.fetch.backend.catalog.entity.ProductVariant> variantMap = new HashMap<>();
            if (!variantIds.isEmpty()) {
                for (com.fetch.backend.catalog.entity.ProductVariant v : variantRepository.findAllById(variantIds)) variantMap.put(v.getId(), v);
            }
            for (OrderItem i : items) {
                int qty = Math.max(0, i.getQuantity());
                itemsSold += qty;
                BigDecimal line = i.getLineTotalAmount() == null ? BigDecimal.ZERO : i.getLineTotalAmount();
                com.fetch.backend.catalog.entity.ProductVariant v = i.getProductVariantId() == null ? null : variantMap.get(i.getProductVariantId());
                BigDecimal supplier = v != null && v.getSupplierCostAmount() != null ? v.getSupplierCostAmount() : BigDecimal.ZERO;
                BigDecimal shipCost = v != null && v.getShippingCostAmount() != null ? v.getShippingCostAmount() : BigDecimal.ZERO;
                boolean hasCost = v != null && (v.getSupplierCostAmount() != null || v.getShippingCostAmount() != null);
                BigDecimal itemCost = supplier.add(shipCost).multiply(BigDecimal.valueOf(qty));
                if (hasCost) {
                    itemsWithCost += qty;
                    estimatedCost = estimatedCost.add(itemCost);
                    estimatedProfit = estimatedProfit.add(line.subtract(itemCost));
                    profitRevenueBase = profitRevenueBase.add(line);
                }
            }
        }

        BigDecimal profitMargin = profitRevenueBase.compareTo(BigDecimal.ZERO) > 0
                ? estimatedProfit.multiply(BigDecimal.valueOf(100)).divide(profitRevenueBase, 2, RoundingMode.HALF_UP)
                : null;

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (Order o : orders) {
            String s = nz(o.getStatus()).toUpperCase(Locale.ROOT);
            if (s.isBlank()) s = "UNKNOWN";
            byStatus.put(s, byStatus.getOrDefault(s, 0L) + 1);
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalSales", totalSales);
        m.put("paidRevenue", paidRevenue);
        m.put("orders", totalOrders);
        m.put("paidOrders", paidOrders);
        m.put("pendingPaymentOrders", pendingPaymentOrders);
        m.put("shippedOrders", shippedOrders);
        m.put("deliveredOrders", deliveredOrders);
        m.put("cancelledOrders", cancelledOrders);
        m.put("fulfillmentQueue", fulfillmentQueue);
        m.put("averageOrderValue", avgOrderValue);
        m.put("estimatedProfit", estimatedProfit);
        m.put("estimatedCost", estimatedCost);
        m.put("profitMarginPercent", profitMargin);
        m.put("itemsSold", itemsSold);
        m.put("itemsWithCostData", itemsWithCost);
        m.put("cjSubmitted", cjSubmitted);
        m.put("cjPending", cjPending);
        m.put("cjErrors", cjErrors);
        m.put("products", productRepository.count());
        m.put("customers", userRepository.count());
        m.put("ordersByStatus", byStatus);
        return m;
    }

    private static String nz(String s) { return s == null ? "" : s; }
}
