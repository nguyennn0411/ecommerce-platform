ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS size VARCHAR(50);

ALTER TABLE order_items
    ADD COLUMN IF NOT EXISTS color VARCHAR(100);

UPDATE order_items
SET size = COALESCE(NULLIF(size, ''), 'UNKNOWN')
WHERE size IS NULL OR size = '';

ALTER TABLE order_items
    ALTER COLUMN size SET NOT NULL;
