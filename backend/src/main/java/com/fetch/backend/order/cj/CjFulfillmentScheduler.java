package com.fetch.backend.order.cj;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CjFulfillmentScheduler {
    private static final Logger log = LoggerFactory.getLogger(CjFulfillmentScheduler.class);
    private final CjFulfillmentService cjFulfillmentService;
    private final boolean enabled;
    private final int batchSize;

    public CjFulfillmentScheduler(CjFulfillmentService cjFulfillmentService,
                                  @Value("${CJ_FULFILLMENT_SYNC_ENABLED:false}") boolean enabled,
                                  @Value("${CJ_FULFILLMENT_SYNC_BATCH_SIZE:10}") int batchSize) {
        this.cjFulfillmentService = cjFulfillmentService;
        this.enabled = enabled;
        this.batchSize = Math.min(50, Math.max(1, batchSize));
    }

    // Runs periodically to auto-sync tracking/shipping/delivery status from CJ for submitted orders.
    @Scheduled(fixedDelayString = "${CJ_FULFILLMENT_SYNC_DELAY_MS:900000}", initialDelayString = "${CJ_FULFILLMENT_SYNC_INITIAL_DELAY_MS:120000}")
    public void syncPending() {
        if (!enabled) return;
        try {
            var res = cjFulfillmentService.syncPendingCjOrders(batchSize);
            Object synced = res.get("synced");
            Object failed = res.get("failed");
            if ((synced instanceof Number n && n.intValue() > 0) || (failed instanceof Number f && f.intValue() > 0)) {
                log.info("CJ fulfillment auto-sync: synced={}, failed={}", synced, failed);
            }
        } catch (Exception e) {
            log.warn("CJ fulfillment auto-sync skipped/failed: {}", e.getMessage());
        }
    }
}
