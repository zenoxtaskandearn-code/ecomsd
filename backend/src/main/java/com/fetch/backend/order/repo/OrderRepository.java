package com.fetch.backend.order.repo;

import com.fetch.backend.order.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByOrderNumber(String orderNumber);
    Optional<Order> findByOrderNumberAndUserId(String orderNumber, Long userId);
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Order> findByCjOrderIdIsNotNullAndStatusNotIn(java.util.Collection<String> statuses);
    Optional<Order> findByCjOrderId(String cjOrderId);
}
