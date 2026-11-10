package com.fetch.backend.common.repo;

import com.fetch.backend.common.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
    Optional<Notification> findByDedupeKey(String dedupeKey);
    List<Notification> findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(java.util.Collection<String> statuses, Instant now);
}
