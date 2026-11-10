package com.fetch.backend.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fetch.backend.common.entity.Notification;
import com.fetch.backend.common.repo.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

@Service
public class NotificationService {
    private final NotificationRepository repository;
    private final ObjectMapper objectMapper;
    private final boolean emailEnabled;

    public NotificationService(NotificationRepository repository, ObjectMapper objectMapper,
                               @Value("${app.notifications.email-enabled:false}") boolean emailEnabled) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.emailEnabled = emailEnabled;
    }

    @Transactional
    public void enqueue(String type, String channel, String recipient, String subject, Long orderId, Long userId, Map<String, ?> payload, String dedupeKey) {
        if (type == null || type.isBlank()) return;
        if (dedupeKey != null && !dedupeKey.isBlank() && repository.findByDedupeKey(dedupeKey).isPresent()) return;
        Notification n = new Notification();
        n.setType(type);
        n.setChannel(channel == null || channel.isBlank() ? "EMAIL" : channel);
        n.setRecipient(recipient);
        n.setSubject(subject);
        n.setOrderId(orderId);
        n.setUserId(userId);
        n.setDedupeKey(dedupeKey);
        n.setStatus(emailEnabled ? "PENDING" : "SKIPPED");
        n.setNextAttemptAt(Instant.now());
        if (!emailEnabled) n.setLastError("Email delivery not configured; notification queued as skipped.");
        if (payload != null) {
            try { n.setPayloadJson(objectMapper.writeValueAsString(payload)); } catch (Exception ignored) {}
        }
        repository.save(n);
    }

    // Basic processor hook (delivery transport integration can be added via provider). Keeps retry/dedup state.
    @Transactional
    public int processPendingBatch() {
        if (!emailEnabled) return 0;
        var items = repository.findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(java.util.List.of("PENDING", "FAILED"), Instant.now());
        int sent = 0;
        for (Notification n : items) {
            if (n.getAttempts() >= n.getMaxAttempts()) { n.setStatus("FAILED"); continue; }
            try {
                n.setAttempts(n.getAttempts() + 1);
                // TODO: integrate real email/SMS provider. For now mark as FAILED with explicit reason when not integrated.
                n.setStatus("FAILED");
                n.setLastError("No notification transport/provider configured.");
                n.setNextAttemptAt(Instant.now().plusSeconds(Math.min(3600, 60L * n.getAttempts() * n.getAttempts())));
            } catch (Exception e) {
                n.setStatus("FAILED");
                n.setLastError(e.getMessage());
                n.setNextAttemptAt(Instant.now().plusSeconds(300));
            }
            repository.save(n);
        }
        return sent;
    }
}
