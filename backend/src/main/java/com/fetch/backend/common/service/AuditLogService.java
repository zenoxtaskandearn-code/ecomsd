package com.fetch.backend.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fetch.backend.common.entity.AuditLog;
import com.fetch.backend.common.repo.AuditLogRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AuditLogService {
    private final AuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public AuditLogService(AuditLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public void record(String actorType, String actorId, String action, String entityType, String entityId, Long orderId, String severity, Map<String, ?> details, String ipAddress) {
        try {
            AuditLog log = new AuditLog();
            log.setActorType(actorType);
            log.setActorId(actorId);
            log.setAction(action);
            log.setEntityType(entityType);
            log.setEntityId(entityId);
            log.setOrderId(orderId);
            log.setSeverity(severity == null ? "INFO" : severity);
            log.setIpAddress(ipAddress);
            if (details != null) {
                try { log.setDetailsJson(objectMapper.writeValueAsString(details)); } catch (Exception ignored) {}
            }
            repository.save(log);
        } catch (Exception ignored) {
            // audit logging must not break primary flows
        }
    }
}
