package com.fetch.backend.order.repo;

import com.fetch.backend.order.entity.SupplierOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface SupplierOrderRepository extends JpaRepository<SupplierOrder, Long> {
    List<SupplierOrder> findByOrderIdOrderByCreatedAtDesc(Long orderId);
    Optional<SupplierOrder> findByOrderIdAndSupplierName(Long orderId, String supplierName);
    Optional<SupplierOrder> findByOrderIdAndSupplierNameAndIdempotencyKey(Long orderId, String supplierName, String idempotencyKey);
}
