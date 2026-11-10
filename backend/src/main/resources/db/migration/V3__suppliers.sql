CREATE TABLE suppliers (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(200) NOT NULL,
    code VARCHAR(80) NOT NULL,
    contact_email VARCHAR(255),
    status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
    api_mode VARCHAR(20) NOT NULL DEFAULT 'MANUAL',
    notes TEXT,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT uk_suppliers_code UNIQUE (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE supplier_products (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    supplier_id BIGINT NOT NULL,
    product_variant_id BIGINT NOT NULL,
    supplier_sku VARCHAR(150),
    supplier_product_ref VARCHAR(255),
    cost_amount DECIMAL(12,2) NULL,
    currency CHAR(3) NULL,
    shipping_notes TEXT,
    last_synced_at DATETIME(3) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at DATETIME(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    CONSTRAINT fk_supplier_products_supplier FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
    CONSTRAINT fk_supplier_products_variant FOREIGN KEY (product_variant_id) REFERENCES product_variants(id),
    INDEX idx_supplier_products_supplier (supplier_id),
    INDEX idx_supplier_products_variant (product_variant_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
