-- CJdropshipping fulfillment integration (official CJ API). Internal/admin-only fields.
ALTER TABLE orders
    ADD COLUMN cj_order_id VARCHAR(128) NULL AFTER notes_internal,
    ADD COLUMN cj_order_number VARCHAR(128) NULL AFTER cj_order_id,
    ADD COLUMN cj_fulfillment_status VARCHAR(60) NULL AFTER cj_order_number,
    ADD COLUMN cj_submitted_at DATETIME(3) NULL AFTER cj_fulfillment_status,
    ADD COLUMN cj_last_sync_at DATETIME(3) NULL AFTER cj_submitted_at,
    ADD COLUMN cj_last_error TEXT NULL AFTER cj_last_sync_at,
    ADD COLUMN cj_raw_snapshot JSON NULL AFTER cj_last_error;

CREATE INDEX idx_orders_cj_order_id ON orders (cj_order_id);
CREATE INDEX idx_orders_cj_status ON orders (cj_fulfillment_status);

ALTER TABLE shipments
    ADD COLUMN source VARCHAR(30) NULL AFTER supplier_id,
    ADD COLUMN cj_track_synced_at DATETIME(3) NULL AFTER delivered_at,
    ADD COLUMN cj_raw_snapshot JSON NULL AFTER cj_track_synced_at;

CREATE INDEX idx_shipments_tracking ON shipments (tracking_number);
