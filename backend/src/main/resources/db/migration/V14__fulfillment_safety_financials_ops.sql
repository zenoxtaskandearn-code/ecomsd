-- V14: fulfillment safety, idempotency, financials, returns/refunds, notifications, audit
-- Additive/safe changes only.

-- Orders: idempotency, tracking access, review/financial flags
ALTER TABLE orders ADD COLUMN IF NOT EXISTS checkout_idempotency_key VARCHAR(80);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS tracking_access_token VARCHAR(80);
ALTER TABLE orders ADD COLUMN IF NOT EXISTS review_required BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS review_reason TEXT;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_tax_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS customer_fee_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00;
ALTER TABLE orders ADD COLUMN IF NOT EXISTS refund_total_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00;

CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_checkout_idempotency ON orders(user_id, checkout_idempotency_key) WHERE checkout_idempotency_key IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_orders_tracking_access_token ON orders(tracking_access_token) WHERE tracking_access_token IS NOT NULL;

-- Backfill tracking tokens for existing orders (best effort)
UPDATE orders SET tracking_access_token = md5(random()::text || clock_timestamp()::text || id::text) WHERE tracking_access_token IS NULL;

-- CJ submit idempotency / supplier submission record (explicit)
CREATE TABLE IF NOT EXISTS supplier_orders (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    supplier_name VARCHAR(60) NOT NULL DEFAULT 'CJ',
    supplier_order_id VARCHAR(120),
    supplier_order_number VARCHAR(120),
    submit_status VARCHAR(40) NOT NULL DEFAULT 'PENDING', -- PENDING,SUBMITTED,FAILED,REQUIRES_REVIEW,CANCELLED
    idempotency_key VARCHAR(120),
    request_payload JSONB,
    response_payload JSONB,
    last_error TEXT,
    submitted_at TIMESTAMP(3),
    last_synced_at TIMESTAMP(3),
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_supplier_orders_order ON supplier_orders(order_id, created_at);
CREATE UNIQUE INDEX IF NOT EXISTS uk_supplier_orders_supplier_order ON supplier_orders(supplier_name, supplier_order_id) WHERE supplier_order_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_supplier_orders_idempotency ON supplier_orders(order_id, supplier_name, idempotency_key) WHERE idempotency_key IS NOT NULL;

-- Per-order financial breakdown
CREATE TABLE IF NOT EXISTS order_costs (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL UNIQUE REFERENCES orders(id),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    customer_subtotal DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    customer_shipping DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    customer_discount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    customer_tax DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    customer_total DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    payment_processing_fee DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    refund_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    supplier_product_cost DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    supplier_shipping_cost DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    supplier_tax_duty_cost DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    supplier_other_cost DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    currency_conversion_cost DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    other_business_cost DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    net_revenue DECIMAL(12,2),
    gross_profit DECIMAL(12,2),
    contribution_profit DECIMAL(12,2),
    estimated_net_profit DECIMAL(12,2),
    original_currency VARCHAR(3),
    fx_rate DECIMAL(18,8),
    metadata_json JSONB,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Tracking events (verified updates)
CREATE TABLE IF NOT EXISTS tracking_events (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    shipment_id BIGINT REFERENCES shipments(id),
    source VARCHAR(30) NOT NULL, -- CJ,CARRIER,MANUAL,SYSTEM
    status VARCHAR(60),
    description TEXT,
    location TEXT,
    event_time TIMESTAMP(3),
    tracking_number VARCHAR(120),
    carrier VARCHAR(120),
    raw_payload JSONB,
    dedupe_key VARCHAR(180),
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_tracking_events_order ON tracking_events(order_id, event_time, created_at);
CREATE UNIQUE INDEX IF NOT EXISTS uk_tracking_events_dedupe ON tracking_events(order_id, dedupe_key) WHERE dedupe_key IS NOT NULL;

-- Returns / refunds
CREATE TABLE IF NOT EXISTS return_requests (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    user_id BIGINT,
    status VARCHAR(40) NOT NULL DEFAULT 'REQUESTED', -- REQUESTED,APPROVED,REJECTED,RECEIVED,REFUND_PENDING,REFUNDED,CLOSED
    reason VARCHAR(120),
    description TEXT,
    evidence_json JSONB,
    admin_notes TEXT,
    requested_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP(3),
    resolved_at TIMESTAMP(3),
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_return_requests_order ON return_requests(order_id, created_at);

CREATE TABLE IF NOT EXISTS refunds (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    payment_id BIGINT REFERENCES payments(id),
    return_request_id BIGINT REFERENCES return_requests(id),
    provider VARCHAR(30) NOT NULL DEFAULT 'PAYPAL',
    provider_refund_id VARCHAR(120),
    idempotency_key VARCHAR(120),
    status VARCHAR(40) NOT NULL DEFAULT 'PENDING', -- PENDING,REQUESTED,COMPLETED,FAILED,CANCELLED
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    reason TEXT,
    raw_response JSONB,
    requested_by VARCHAR(60),
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_refunds_order ON refunds(order_id, created_at);
CREATE UNIQUE INDEX IF NOT EXISTS uk_refunds_provider_refund ON refunds(provider, provider_refund_id) WHERE provider_refund_id IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uk_refunds_idempotency ON refunds(order_id, idempotency_key) WHERE idempotency_key IS NOT NULL;

-- Notifications (queue/retry/dedup)
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT REFERENCES orders(id),
    user_id BIGINT,
    type VARCHAR(60) NOT NULL,
    channel VARCHAR(20) NOT NULL DEFAULT 'EMAIL',
    recipient VARCHAR(255),
    subject VARCHAR(255),
    payload_json JSONB,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDING', -- PENDING,SENT,FAILED,SKIPPED
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    dedupe_key VARCHAR(180),
    last_error TEXT,
    next_attempt_at TIMESTAMP(3),
    sent_at TIMESTAMP(3),
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_notifications_status_next ON notifications(status, next_attempt_at, created_at);
CREATE UNIQUE INDEX IF NOT EXISTS uk_notifications_dedupe ON notifications(dedupe_key) WHERE dedupe_key IS NOT NULL;

-- Audit log
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor_type VARCHAR(30), -- CUSTOMER,ADMIN,STAFF,SYSTEM,WEBHOOK
    actor_id VARCHAR(80),
    action VARCHAR(120) NOT NULL,
    entity_type VARCHAR(60),
    entity_id VARCHAR(80),
    order_id BIGINT,
    severity VARCHAR(20) NOT NULL DEFAULT 'INFO',
    details_json JSONB,
    ip_address VARCHAR(80),
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_audit_logs_order ON audit_logs(order_id, created_at);
CREATE INDEX IF NOT EXISTS idx_audit_logs_entity ON audit_logs(entity_type, entity_id, created_at);
