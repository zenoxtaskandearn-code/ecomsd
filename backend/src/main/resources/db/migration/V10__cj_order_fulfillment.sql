-- CJdropshipping fulfillment integration (official CJ API). Internal/admin-only fields.
ALTER TABLE orders
    ADD COLUMN cj_order_id VARCHAR(128) NULL,
    ADD COLUMN cj_order_number VARCHAR(128) NULL,
    ADD COLUMN cj_fulfillment_status VARCHAR(60) NULL,
    ADD COLUMN cj_submitted_at TIMESTAMP(3) NULL,
    ADD COLUMN cj_last_sync_at TIMESTAMP(3) NULL,
    ADD COLUMN cj_last_error TEXT NULL,
    ADD COLUMN cj_raw_snapshot JSONB NULL;

CREATE INDEX idx_orders_cj_order_id ON orders (cj_order_id);
CREATE INDEX idx_orders_cj_status ON orders (cj_fulfillment_status);

ALTER TABLE shipments
    ADD COLUMN source VARCHAR(30) NULL,
    ADD COLUMN cj_track_synced_at TIMESTAMP(3) NULL,
    ADD COLUMN cj_raw_snapshot JSONB NULL;

CREATE INDEX idx_shipments_tracking ON shipments (tracking_number);
