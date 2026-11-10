package com.fetch.backend.order.repo;

import com.fetch.backend.order.entity.ReturnRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface ReturnRequestRepository extends JpaRepository<ReturnRequest, Long> {
    List<ReturnRequest> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    List<ReturnRequest> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<ReturnRequest> findByIdAndUserId(Long id, Long userId);
}
