-- CJdropshipping/manual import + internal costing fields (admin-only; never exposed in public APIs)
ALTER TABLE products
  ADD COLUMN cj_product_id VARCHAR(100) NULL,
  ADD COLUMN supplier_source VARCHAR(40) NULL,
  ADD COLUMN supplier_id BIGINT NULL,
  ADD COLUMN estimated_delivery VARCHAR(255) NULL,
  ADD COLUMN shipping_options_json TEXT NULL,
  ADD COLUMN supplier_shipping_cost_amount DECIMAL(12,2) NULL,
  ADD COLUMN internal_notes TEXT NULL;

CREATE INDEX idx_products_cj_product_id ON products (cj_product_id);
CREATE INDEX idx_products_supplier_id ON products (supplier_id);

ALTER TABLE product_variants
  ADD COLUMN cj_variant_id VARCHAR(100) NULL,
  ADD COLUMN supplier_sku VARCHAR(150) NULL,
  ADD COLUMN supplier_cost_amount DECIMAL(12,2) NULL,
  ADD COLUMN shipping_cost_amount DECIMAL(12,2) NULL,
  ADD COLUMN stock_availability VARCHAR(120) NULL,
  ADD COLUMN warehouse_info VARCHAR(255) NULL,
  ADD COLUMN dimensions_json TEXT NULL;

CREATE INDEX idx_variants_cj_variant_id ON product_variants (cj_variant_id);
