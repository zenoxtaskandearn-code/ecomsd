package com.fetch.backend.order.web;

import com.fetch.backend.order.dto.OrderDtos;
import com.fetch.backend.order.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {
    private final OrderService orderService;
    public OrderController(OrderService orderService) { this.orderService = orderService; }

    @PostMapping("/preview")
    public OrderDtos.CheckoutPreviewResponse preview(@Valid @RequestBody OrderDtos.CreateOrderRequest request) { return orderService.preview(request); }

    @PostMapping
    public OrderDtos.OrderResponse create(@Valid @RequestBody OrderDtos.CreateOrderRequest request) { return orderService.createFromCart(request); }

    @GetMapping
    public List<OrderDtos.OrderResponse> mine() { return orderService.myOrders(); }

    @GetMapping("/{orderNumber}")
    public OrderDtos.OrderResponse get(@PathVariable String orderNumber) { return orderService.myOrder(orderNumber); }
}
