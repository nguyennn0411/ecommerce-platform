-- Keep all active products at one low price for checkout demonstrations.
UPDATE products
SET base_price = 2000.00
WHERE status = 'ACTIVE';
