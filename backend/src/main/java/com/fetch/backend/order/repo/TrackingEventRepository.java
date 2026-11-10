package com.fetch.backend.order.repo;

import com.fetch.backend.order.entity.TrackingEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface TrackingEventRepository extends JpaRepository<TrackingEvent, Long> {
    List<TrackingEvent> findByOrderIdOrderByEventTimeAscCreatedAtAsc(Long orderId);
    Optional<TrackingEvent> findByOrderIdAndDedupeKey(Long orderId, String dedupeKey);
}
