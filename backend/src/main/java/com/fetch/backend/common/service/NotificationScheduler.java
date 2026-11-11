package com.fetch.backend.common.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NotificationScheduler {
    private final NotificationService notificationService;
    private final boolean processEnabled;

    public NotificationScheduler(NotificationService notificationService,
                                 @Value("${app.notifications.process-enabled:true}") boolean processEnabled) {
        this.notificationService = notificationService;
        this.processEnabled = processEnabled;
    }

    @Scheduled(fixedDelayString = "${app.notifications.process-delay-ms:30000}")
    public void process() {
        if (!processEnabled) return;
        try { notificationService.processPendingBatch(); } catch (Exception ignored) {}
    }
}
