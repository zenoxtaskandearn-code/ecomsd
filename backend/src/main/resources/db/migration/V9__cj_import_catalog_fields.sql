-- CJdropshipping/manual import + internal costing fields (admin-only; never exposed in public APIs)
ALTER TABLE products
  ADD COLUMN cj_product_id VARCHAR(100) NULL AFTER status,
  ADD COLUMN supplier_source VARCHAR(40) NULL AFTER cj_product_id,
  ADD COLUMN supplier_id BIGINT NULL AFTER supplier_source,
  ADD COLUMN estimated_delivery VARCHAR(255) NULL AFTER seo_description,
  ADD COLUMN shipping_options_json TEXT NULL AFTER estimated_delivery,
  ADD COLUMN supplier_shipping_cost_amount DECIMAL(12,2) NULL AFTER shipping_options_json,
  ADD COLUMN internal_notes TEXT NULL AFTER supplier_shipping_cost_amount,
  ADD INDEX idx_products_cj_product_id (cj_product_id),
  ADD INDEX idx_products_supplier_id (supplier_id);

ALTER TABLE product_variants
  ADD COLUMN cj_variant_id VARCHAR(100) NULL AFTER sku,
  ADD COLUMN supplier_sku VARCHAR(150) NULL AFTER cj_variant_id,
  ADD COLUMN supplier_cost_amount DECIMAL(12,2) NULL AFTER currency,
  ADD COLUMN shipping_cost_amount DECIMAL(12,2) NULL AFTER supplier_cost_amount,
  ADD COLUMN stock_availability VARCHAR(120) NULL AFTER weight_grams,
  ADD COLUMN warehouse_info VARCHAR(255) NULL AFTER stock_availability,
  ADD COLUMN dimensions_json TEXT NULL AFTER warehouse_info,
  ADD INDEX idx_variants_cj_variant_id (cj_variant_id);
