CREATE TABLE payments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    provider VARCHAR(30) NOT NULL,
    provider_order_id VARCHAR(128) NULL,
    provider_capture_id VARCHAR(128) NULL,
    status VARCHAR(40) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    idempotency_key VARCHAR(128) NULL,
    raw_response_redacted JSONB NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT uk_payments_idempotency UNIQUE (idempotency_key)
);
CREATE INDEX idx_payments_order ON payments (order_id);
CREATE INDEX idx_payments_provider_order ON payments (provider_order_id);
CREATE INDEX idx_payments_provider_capture ON payments (provider_capture_id);

CREATE TABLE shipments (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    supplier_id BIGINT NULL,
    carrier VARCHAR(80),
    tracking_number VARCHAR(120),
    tracking_url VARCHAR(1024),
    status VARCHAR(40) NOT NULL,
    shipped_at TIMESTAMP(3) NULL,
    delivered_at TIMESTAMP(3) NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_shipments_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_shipments_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id)
);
CREATE INDEX idx_shipments_order ON shipments (order_id);

CREATE TABLE webhook_events (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(30) NOT NULL,
    event_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(120),
    order_id BIGINT NULL,
    payment_id BIGINT NULL,
    payload_hash VARCHAR(128),
    received_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP(3) NULL,
    process_status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    error_message TEXT NULL,
    CONSTRAINT uk_webhook_event UNIQUE (provider, event_id),
    CONSTRAINT fk_webhook_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_webhook_payment FOREIGN KEY (payment_id) REFERENCES payments(id)
);
CREATE INDEX idx_webhook_status ON webhook_events (process_status);
