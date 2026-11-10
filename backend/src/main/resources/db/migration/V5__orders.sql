CREATE TABLE orders (
    id BIGSERIAL PRIMARY KEY,
    order_number VARCHAR(40) NOT NULL,
    user_id BIGINT NULL,
    status VARCHAR(40) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    subtotal_amount DECIMAL(12,2) NOT NULL,
    shipping_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    tax_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(12,2) NOT NULL,
    customer_email VARCHAR(255) NOT NULL,
    shipping_address_snapshot JSONB NOT NULL,
    billing_address_snapshot JSONB NULL,
    notes_internal TEXT NULL,
    placed_at TIMESTAMP(3) NULL,
    paid_at TIMESTAMP(3) NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_orders_number UNIQUE (order_number),
    CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id)
);
CREATE INDEX idx_orders_user_created ON orders (user_id, created_at);
CREATE INDEX idx_orders_status_created ON orders (status, created_at);

CREATE TABLE order_items (
    id BIGSERIAL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    product_variant_id BIGINT NULL,
    product_name_snapshot VARCHAR(200) NOT NULL,
    variant_title_snapshot VARCHAR(150),
    sku_snapshot VARCHAR(100) NOT NULL,
    unit_price_amount DECIMAL(12,2) NOT NULL,
    quantity INT NOT NULL,
    line_total_amount DECIMAL(12,2) NOT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
    CONSTRAINT fk_order_items_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants(id),
    CONSTRAINT chk_order_item_quantity CHECK (quantity > 0)
);
CREATE INDEX idx_order_items_order ON order_items (order_id);
