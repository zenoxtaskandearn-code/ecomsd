-- Add separate status tracking fields and immutable status history for auditability.
ALTER TABLE orders
    ADD COLUMN IF NOT EXISTS payment_status VARCHAR(40),
    ADD COLUMN IF NOT EXISTS fulfillment_status VARCHAR(40);

UPDATE orders
SET payment_status = CASE
    WHEN UPPER(COALESCE(status, '')) IN ('PAID','PROCESSING','FULFILLMENT_SUBMITTED','SHIPPED','DELIVERED') THEN 'CAPTURED'
    WHEN UPPER(COALESCE(status, '')) = 'PAYMENT_FAILED' THEN 'FAILED'
    WHEN UPPER(COALESCE(status, '')) IN ('CANCELLED') THEN 'CANCELLED'
    WHEN UPPER(COALESCE(status, '')) IN ('REFUNDED') THEN 'REFUNDED'
    ELSE 'PENDING'
END
WHERE payment_status IS NULL;

UPDATE orders
SET fulfillment_status = CASE
    WHEN UPPER(COALESCE(status, '')) = 'DELIVERED' THEN 'DELIVERED'
    WHEN UPPER(COALESCE(status, '')) = 'SHIPPED' THEN 'SHIPPED'
    WHEN UPPER(COALESCE(status, '')) = 'FULFILLMENT_SUBMITTED' OR cj_order_id IS NOT NULL THEN 'SUBMITTED'
    WHEN UPPER(COALESCE(status, '')) = 'PROCESSING' THEN 'PROCESSING'
    WHEN UPPER(COALESCE(status, '')) IN ('CANCELLED') THEN 'CANCELLED'
    WHEN UPPER(COALESCE(status, '')) IN ('REFUNDED') THEN 'REFUNDED'
    ELSE 'PENDING'
END
WHERE fulfillment_status IS NULL;

ALTER TABLE orders ALTER COLUMN payment_status SET DEFAULT 'PENDING';
ALTER TABLE orders ALTER COLUMN fulfillment_status SET DEFAULT 'PENDING';
UPDATE orders SET payment_status = 'PENDING' WHERE payment_status IS NULL;
UPDATE orders SET fulfillment_status = 'PENDING' WHERE fulfillment_status IS NULL;
ALTER TABLE orders ALTER COLUMN payment_status SET NOT NULL;
ALTER TABLE orders ALTER COLUMN fulfillment_status SET NOT NULL;

CREATE TABLE IF NOT EXISTS order_status_history (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL REFERENCES orders(id),
    domain VARCHAR(30) NOT NULL, -- ORDER, PAYMENT, FULFILLMENT
    from_status VARCHAR(60),
    to_status VARCHAR(60) NOT NULL,
    source VARCHAR(60), -- SYSTEM, CUSTOMER, ADMIN, PAYPAL, CJ, WEBHOOK
    note TEXT,
    metadata_json JSONB,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_order_status_history_order ON order_status_history(order_id, created_at);
CREATE INDEX IF NOT EXISTS idx_order_status_history_domain ON order_status_history(domain, created_at);
