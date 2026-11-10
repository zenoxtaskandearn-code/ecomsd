package com.fetch.backend.payment.repo;

import com.fetch.backend.payment.entity.Shipment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ShipmentRepository extends JpaRepository<Shipment, Long> {
    List<Shipment> findByOrderId(Long orderId);
    java.util.Optional<Shipment> findFirstByOrderIdAndTrackingNumber(Long orderId, String trackingNumber);
}
