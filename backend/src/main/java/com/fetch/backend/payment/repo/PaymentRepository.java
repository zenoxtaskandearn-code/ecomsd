package com.fetch.backend.payment.repo;

import com.fetch.backend.payment.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByOrderIdAndProvider(Long orderId, String provider);
    Optional<Payment> findByProviderOrderId(String providerOrderId);
}
