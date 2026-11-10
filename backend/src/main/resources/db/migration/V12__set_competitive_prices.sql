-- Set competitive retail prices (includes margin/fulfillment buffer; review and adjust as needed).
-- Product 75: Car seat dog safety belt (typical retail ~ $15-$20)
UPDATE product_variants SET price_amount = 17.99, updated_at = NOW() WHERE product_id = 75;
UPDATE products SET base_price_amount = 17.99, updated_at = NOW() WHERE id = 75;

-- Product 91: Nylon anti-grind dog leash/collar (typical retail ~ $12-$17)
UPDATE product_variants SET price_amount = 14.99, updated_at = NOW() WHERE product_id = 91;
UPDATE products SET base_price_amount = 14.99, updated_at = NOW() WHERE id = 91;
