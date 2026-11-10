package com.fetch.backend.order.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fetch.backend.cart.entity.CartItem;
import com.fetch.backend.cart.repo.CartItemRepository;
import com.fetch.backend.cart.repo.CartRepository;
import com.fetch.backend.catalog.entity.ProductVariant;
import com.fetch.backend.catalog.repo.ProductVariantRepository;
import com.fetch.backend.common.error.BadRequestException;
import com.fetch.backend.common.error.NotFoundException;
import com.fetch.backend.order.dto.OrderDtos;
import com.fetch.backend.order.entity.Order;
import com.fetch.backend.order.entity.OrderItem;
import com.fetch.backend.order.repo.OrderItemRepository;
import com.fetch.backend.order.repo.OrderRepository;
import com.fetch.backend.payment.repo.ShipmentRepository;
import com.fetch.backend.order.cj.CjAutoSubmitter;
import com.fetch.backend.user.service.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

@Service
public class OrderService {
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;
    private final ShipmentRepository shipmentRepository;
    private final UserService userService;
    private final ObjectMapper objectMapper;
    private final CjAutoSubmitter cjAutoSubmitter;

    public OrderService(OrderRepository orderRepository, OrderItemRepository orderItemRepository, CartRepository cartRepository,
                        CartItemRepository cartItemRepository, ProductVariantRepository variantRepository, ShipmentRepository shipmentRepository, UserService userService, ObjectMapper objectMapper,
                        CjAutoSubmitter cjAutoSubmitter) {
        this.orderRepository = orderRepository; this.orderItemRepository = orderItemRepository; this.cartRepository = cartRepository;
        this.cartItemRepository = cartItemRepository; this.variantRepository = variantRepository; this.shipmentRepository = shipmentRepository; this.userService = userService; this.objectMapper = objectMapper;
        this.cjAutoSubmitter = cjAutoSubmitter;
    }

    @Transactional(readOnly = true)
    public OrderDtos.CheckoutPreviewResponse preview(OrderDtos.CreateOrderRequest request) {
        var user = userService.currentUser();
        var cart = cartRepository.findByUserIdAndStatus(user.getId(), "ACTIVE").orElseThrow(() -> new BadRequestException("Cart is empty"));
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        if (items.isEmpty()) throw new BadRequestException("Cart is empty");
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderDtos.OrderItemResponse> itemDtos = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        for (CartItem ci : items) {
            ProductVariant v = variantRepository.findById(ci.getProductVariantId()).orElse(null);
            if (v == null || !v.isActive() || !"ACTIVE".equals(v.getProduct().getStatus())) {
                warnings.add("An item in your cart is no longer available.");
                continue;
            }
            BigDecimal line = v.getPriceAmount().multiply(BigDecimal.valueOf(ci.getQuantity()));
            subtotal = subtotal.add(line);
            itemDtos.add(new OrderDtos.OrderItemResponse(v.getProduct().getName(), v.getTitle(), v.getSku(), v.getPriceAmount(), ci.getQuantity(), line));
        }
        if (itemDtos.isEmpty()) throw new BadRequestException("No available items in cart");
        BigDecimal shipping = BigDecimal.ZERO;
        BigDecimal tax = BigDecimal.ZERO; // MVP: no automated US tax
        BigDecimal total = subtotal.add(shipping).add(tax);
        return new OrderDtos.CheckoutPreviewResponse("USD", subtotal, shipping, tax, BigDecimal.ZERO, total, itemDtos, warnings,
                "Shipping and any applicable taxes/duties are confirmed at finalization. International customers are responsible for duties/taxes where applicable.");
    }

    @Transactional
    public OrderDtos.OrderResponse createFromCart(OrderDtos.CreateOrderRequest request) {
        var user = userService.currentUser();
        var cart = cartRepository.findByUserIdAndStatus(user.getId(), "ACTIVE").orElseThrow(() -> new BadRequestException("Cart is empty"));
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());
        if (items.isEmpty()) throw new BadRequestException("Cart is empty");

        for (int attempt = 0; attempt < 5; attempt++) {
            try {
                Order order = new Order();
                order.setOrderNumber(generateOrderNumber());
                order.setUserId(user.getId());
                order.setStatus("PENDING_PAYMENT");
                order.setCurrency("USD");
                order.setCustomerEmail(request.customerEmail() != null && !request.customerEmail().isBlank() ? request.customerEmail().trim().toLowerCase() : user.getEmail());
                order.setShippingAddressSnapshot(writeJson(request.shippingAddress()));
                order.setBillingAddressSnapshot(request.billingAddress() == null ? null : writeJson(request.billingAddress()));
                order.setPlacedAt(Instant.now());

                BigDecimal subtotal = BigDecimal.ZERO;
                List<OrderItem> orderItems = new ArrayList<>();
                for (CartItem ci : items) {
                    ProductVariant v = variantRepository.findById(ci.getProductVariantId()).orElseThrow(() -> new BadRequestException("A cart item is no longer available"));
                    if (!v.isActive() || !"ACTIVE".equals(v.getProduct().getStatus())) throw new BadRequestException("A cart item is unavailable");
                    BigDecimal line = v.getPriceAmount().multiply(BigDecimal.valueOf(ci.getQuantity()));
                    subtotal = subtotal.add(line);
                    OrderItem oi = new OrderItem();
                    oi.setProductVariantId(v.getId());
                    oi.setProductNameSnapshot(v.getProduct().getName());
                    oi.setVariantTitleSnapshot(v.getTitle());
                    oi.setSkuSnapshot(v.getSku());
                    oi.setUnitPriceAmount(v.getPriceAmount());
                    oi.setQuantity(ci.getQuantity());
                    oi.setLineTotalAmount(line);
                    orderItems.add(oi);
                }
                order.setSubtotalAmount(subtotal);
                order.setShippingAmount(BigDecimal.ZERO);
                order.setTaxAmount(BigDecimal.ZERO); // MVP: no automated tax
                order.setDiscountAmount(BigDecimal.ZERO);
                order.setTotalAmount(subtotal);
                orderRepository.saveAndFlush(order);
                for (OrderItem oi : orderItems) { oi.setOrderId(order.getId()); orderItemRepository.save(oi); }

                cart.setStatus("CONVERTED");
                cartRepository.save(cart);
                cjAutoSubmitter.onOrderCreated(order.getOrderNumber());
                return toResponse(order, orderItems);
            } catch (DataIntegrityViolationException e) {
                if (attempt == 4) throw new BadRequestException("Could not create order. Please try again.");
            }
        }
        throw new BadRequestException("Could not create order. Please try again.");
    }

    @Transactional(readOnly = true)
    public List<OrderDtos.OrderResponse> myOrders() {
        var user = userService.currentUser();
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(o -> toResponse(o, orderItemRepository.findByOrderId(o.getId()))).toList();
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderResponse myOrder(String orderNumber) {
        var user = userService.currentUser();
        Order order = orderRepository.findByOrderNumberAndUserId(orderNumber, user.getId()).orElseThrow(() -> new NotFoundException("Order not found"));
        return toResponse(order, orderItemRepository.findByOrderId(order.getId()));
    }

    private OrderDtos.OrderResponse toResponse(Order order, List<OrderItem> items) {
        Map<String, Object> addr = readJson(order.getShippingAddressSnapshot());
        List<OrderDtos.OrderItemResponse> itemDtos = items.stream().map(i -> new OrderDtos.OrderItemResponse(
                i.getProductNameSnapshot(), i.getVariantTitleSnapshot(), i.getSkuSnapshot(), i.getUnitPriceAmount(), i.getQuantity(), i.getLineTotalAmount())).toList();
        List<OrderDtos.ShipmentResponse> shipments = shipmentRepository.findByOrderId(order.getId()).stream()
                .map(s -> new OrderDtos.ShipmentResponse(s.getCarrier(), s.getTrackingNumber(), s.getTrackingUrl(), s.getStatus(), s.getShippedAt() == null ? null : s.getShippedAt().toString()))
                .toList();
        return new OrderDtos.OrderResponse(order.getOrderNumber(), order.getStatus(), order.getCurrency(), order.getSubtotalAmount(),
                order.getShippingAmount(), order.getTaxAmount(), order.getDiscountAmount(), order.getTotalAmount(),
                order.getCustomerEmail(), order.getPlacedAt() == null ? null : order.getPlacedAt().toString(), order.getPaidAt() == null ? null : order.getPaidAt().toString(), addr, itemDtos, shipments);
    }

    private String generateOrderNumber() {
        return "FT-" + (100000 + new Random().nextInt(900000));
    }
    private String writeJson(Object value) { try { return objectMapper.writeValueAsString(value); } catch (Exception e) { throw new BadRequestException("Invalid address"); } }
    private Map<String, Object> readJson(String json) { try { return objectMapper.readValue(json, new TypeReference<>() {}); } catch (Exception e) { return Map.of(); } }
}
