package com.fetch.backend.order.cj;

import com.fetch.backend.catalog.cj.CjDropshippingClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class CjAutoSubmitter {
    private static final Logger log = LoggerFactory.getLogger(CjAutoSubmitter.class);
    private final CjFulfillmentService fulfillmentService;
    private final CjDropshippingClient cj;
    private final boolean submitOnPaid;
    private final boolean submitOnCreate;

    public CjAutoSubmitter(CjFulfillmentService fulfillmentService, CjDropshippingClient cj,
                           @Value("${CJ_AUTO_SUBMIT_ON_PAID:true}") boolean submitOnPaid,
                           @Value("${CJ_AUTO_SUBMIT_ON_CREATE:false}") boolean submitOnCreate) {
        this.fulfillmentService = fulfillmentService;
        this.cj = cj;
        this.submitOnPaid = submitOnPaid;
        this.submitOnCreate = submitOnCreate;
    }

    public void onOrderPaid(String orderNumber) {
        if (!submitOnPaid || !cj.isConfigured() || orderNumber == null || orderNumber.isBlank()) return;
        afterCommit(() -> safeSubmit(orderNumber, false, "paid"));
    }

    public void onOrderCreated(String orderNumber) {
        if (!submitOnCreate || !cj.isConfigured() || orderNumber == null || orderNumber.isBlank()) return;
        // submitOnCreate is explicit opt-in; allows submission before payment confirmation (business risk).
        afterCommit(() -> safeSubmit(orderNumber, true, "created"));
    }

    private void safeSubmit(String orderNumber, boolean allowPending, String reason) {
        try {
            fulfillmentService.submitOrderToCj(orderNumber, allowPending);
            log.info("CJ auto-submit succeeded for order {} (trigger={})", orderNumber, reason);
        } catch (Exception e) {
            log.warn("CJ auto-submit failed for order {} (trigger={}): {}", orderNumber, reason, e.getMessage());
            // error is recorded on the order by fulfillment service when possible
        }
    }

    private void afterCommit(Runnable action) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { action.run(); }
            });
        } else {
            action.run();
        }
    }
}
