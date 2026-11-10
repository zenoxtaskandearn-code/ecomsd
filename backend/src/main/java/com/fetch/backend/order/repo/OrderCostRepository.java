package com.fetch.backend.order.repo;

import com.fetch.backend.order.entity.OrderCost;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OrderCostRepository extends JpaRepository<OrderCost, Long> {
    Optional<OrderCost> findByOrderId(Long orderId);
}
