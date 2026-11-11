package com.fetch.backend.common.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fetch.backend.common.entity.Notification;
import com.fetch.backend.common.repo.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationRepository repository;
    private final ObjectMapper objectMapper;
    private final boolean emailEnabled;
    private final String provider;
    private final String emailjsServiceId;
    private final String emailjsTemplateId;
    private final String emailjsPublicKey;
    private final String emailjsPrivateKey;
    private final RestClient restClient;

    public NotificationService(NotificationRepository repository, ObjectMapper objectMapper, RestClient.Builder restClientBuilder,
                               @Value("${app.notifications.email-enabled:false}") boolean emailEnabled,
                               @Value("${app.notifications.provider:emailjs}") String provider,
                               @Value("${app.notifications.emailjs.service-id:}") String emailjsServiceId,
                               @Value("${app.notifications.emailjs.template-id:}") String emailjsTemplateId,
                               @Value("${app.notifications.emailjs.public-key:}") String emailjsPublicKey,
                               @Value("${app.notifications.emailjs.private-key:}") String emailjsPrivateKey) {
        this.repository = repository;
        this.objectMapper = objectMapper;
        this.emailEnabled = emailEnabled;
        this.provider = provider == null ? "emailjs" : provider.trim().toLowerCase();
        this.emailjsServiceId = nz(emailjsServiceId);
        this.emailjsTemplateId = nz(emailjsTemplateId);
        this.emailjsPublicKey = nz(emailjsPublicKey);
        this.emailjsPrivateKey = nz(emailjsPrivateKey);
        this.restClient = restClientBuilder.baseUrl("https://api.emailjs.com").build();
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

    @Transactional
    public int processPendingBatch() {
        if (!emailEnabled) return 0;
        var items = repository.findTop50ByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(java.util.List.of("PENDING", "FAILED"), Instant.now());
        int sent = 0;
        for (Notification n : items) {
            if (n.getAttempts() >= n.getMaxAttempts()) { n.setStatus("FAILED"); continue; }
            try {
                n.setAttempts(n.getAttempts() + 1);
                send(n);
                n.setStatus("SENT");
                n.setSentAt(Instant.now());
                n.setLastError(null);
                n.setNextAttemptAt(null);
                sent++;
            } catch (Exception e) {
                n.setStatus(n.getAttempts() >= n.getMaxAttempts() ? "FAILED" : "PENDING");
                n.setLastError(safeMsg(e.getMessage()));
                n.setNextAttemptAt(Instant.now().plusSeconds(Math.min(3600, 60L * n.getAttempts() * n.getAttempts())));
                log.warn("Notification send failed id={} type={} attempt={}: {}", n.getId(), n.getType(), n.getAttempts(), safeMsg(e.getMessage()));
            }
            repository.save(n);
        }
        return sent;
    }

    private void send(Notification n) {
        if (!"EMAIL".equalsIgnoreCase(n.getChannel())) {
            throw new IllegalStateException("Unsupported notification channel: " + n.getChannel());
        }
        if (!"emailjs".equals(provider)) {
            throw new IllegalStateException("No configured email provider: " + provider);
        }
        if (emailjsServiceId.isBlank() || emailjsTemplateId.isBlank() || emailjsPublicKey.isBlank()) {
            throw new IllegalStateException("EmailJS not configured (service/template/public key required)");
        }
        if (n.getRecipient() == null || n.getRecipient().isBlank()) {
            throw new IllegalStateException("Notification recipient is missing");
        }
        Map<String, Object> templateParams = new HashMap<>();
        templateParams.put("to_email", n.getRecipient());
        templateParams.put("subject", nz(n.getSubject()));
        templateParams.put("notification_type", nz(n.getType()));
        templateParams.put("order_id", n.getOrderId() == null ? "" : String.valueOf(n.getOrderId()));
        if (n.getPayloadJson() != null && !n.getPayloadJson().isBlank()) {
            try {
                Map<String, Object> payload = objectMapper.readValue(n.getPayloadJson(), new TypeReference<>() {});
                // Flatten common keys for EmailJS templates (activation/reset/order links etc.)
                payload.forEach((k, v) -> templateParams.putIfAbsent(k, v == null ? "" : v));
                templateParams.put("payload_json", n.getPayloadJson());
            } catch (Exception ignored) {
                templateParams.put("payload_json", n.getPayloadJson());
            }
        }

        Map<String, Object> body = new HashMap<>();
        body.put("service_id", emailjsServiceId);
        body.put("template_id", emailjsTemplateId);
        body.put("user_id", emailjsPublicKey); // EmailJS public key
        if (!emailjsPrivateKey.isBlank()) body.put("accessToken", emailjsPrivateKey); // recommended for server-side
        body.put("template_params", templateParams);

        var resp = restClient.post()
                .uri("/api/v1.0/email/send")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .toEntity(String.class);
        if (!resp.getStatusCode().is2xxSuccessful()) {
            throw new IllegalStateException("EmailJS send failed: HTTP " + resp.getStatusCode().value());
        }
    }

    private static String nz(String s) { return s == null ? "" : s.trim(); }
    private static String safeMsg(String s) { if (s == null) return "Unknown error"; return s.length() > 500 ? s.substring(0, 500) : s; }
}
