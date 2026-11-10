CREATE TABLE payments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    provider VARCHAR(30) NOT NULL,
    provider_order_id VARCHAR(128) NULL,
    provider_capture_id VARCHAR(128) NULL,
    status VARCHAR(40) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,
    currency CHAR(3) NOT NULL DEFAULT 'USD',
    idempotency_key VARCHAR(128) NULL,
    raw_response_redacted JSON NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT uk_payments_idempotency UNIQUE (idempotency_key),
    INDEX idx_payments_order (order_id),
    INDEX idx_payments_provider_order (provider_order_id),
    INDEX idx_payments_provider_capture (provider_capture_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE shipments (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    supplier_id BIGINT NULL,
    carrier VARCHAR(80),
    tracking_number VARCHAR(120),
    tracking_url VARCHAR(1024),
    status VARCHAR(40) NOT NULL,
    shipped_at DATETIME(3) NULL,
    delivered_at DATETIME(3) NULL,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_shipments_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_shipments_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    INDEX idx_shipments_order (order_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE webhook_events (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    provider VARCHAR(30) NOT NULL,
    event_id VARCHAR(128) NOT NULL,
    event_type VARCHAR(120),
    order_id BIGINT NULL,
    payment_id BIGINT NULL,
    payload_hash VARCHAR(128),
    received_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    processed_at DATETIME(3) NULL,
    process_status VARCHAR(30) NOT NULL DEFAULT 'RECEIVED',
    error_message TEXT NULL,
    CONSTRAINT uk_webhook_event UNIQUE (provider, event_id),
    CONSTRAINT fk_webhook_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_webhook_payment FOREIGN KEY (payment_id) REFERENCES payments(id),
    INDEX idx_webhook_status (process_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
