package com.fetch.backend.user.admin;

import com.fetch.backend.order.repo.OrderRepository;
import com.fetch.backend.payment.repo.PaymentRepository;
import com.fetch.backend.user.repo.UserRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminUserController {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    public AdminUserController(UserRepository userRepository, OrderRepository orderRepository, PaymentRepository paymentRepository) {
        this.userRepository = userRepository; this.orderRepository = orderRepository; this.paymentRepository = paymentRepository;
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
        var orders = orderRepository.findAll();
        var payments = paymentRepository.findAll();
        BigDecimal sales = payments.stream().filter(p -> "CAPTURED".equals(p.getStatus())).map(p -> p.getAmount() == null ? BigDecimal.ZERO : p.getAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);
        long fulfillmentQueue = orders.stream().filter(o -> "PAID".equals(o.getStatus()) || "PROCESSING".equals(o.getStatus())).count();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalSales", sales);
        m.put("orders", orders.size());
        m.put("fulfillmentQueue", fulfillmentQueue);
        m.put("customers", userRepository.count());
        return m;
    }
}
