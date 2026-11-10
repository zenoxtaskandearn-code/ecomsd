package com.fetch.backend.order.repo;

import com.fetch.backend.order.entity.Refund;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund, Long> {
    List<Refund> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    Optional<Refund> findByOrderIdAndIdempotencyKey(Long orderId, String idempotencyKey);
    Optional<Refund> findByProviderAndProviderRefundId(String provider, String providerRefundId);
}
