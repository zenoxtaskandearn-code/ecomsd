CREATE TABLE suppliers (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(80) NOT NULL,
    contact_email VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    api_mode VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    notes TEXT,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_suppliers_code UNIQUE (code)
);

CREATE TABLE supplier_products (
    id BIGSERIAL PRIMARY KEY,
    supplier_id BIGINT NOT NULL,
    product_variant_id BIGINT NOT NULL,
    supplier_sku VARCHAR(150),
    supplier_product_ref VARCHAR(255),
    cost_amount DECIMAL(12,2) NULL,
    currency VARCHAR(3) NULL,
    shipping_notes TEXT,
    last_synced_at TIMESTAMP(3) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_supplier_products_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT fk_supplier_products_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants(id)
);
CREATE INDEX idx_supplier_products_supplier ON supplier_products (supplier_id);
CREATE INDEX idx_supplier_products_variant ON supplier_products (product_variant_id);
