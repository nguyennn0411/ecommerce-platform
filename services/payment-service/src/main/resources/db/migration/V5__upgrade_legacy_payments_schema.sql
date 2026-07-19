-- Bring databases created by the legacy payment schema up to the current entity model.
-- Existing rows are retained; only the new provider field is backfilled from its old name.
ALTER TABLE payments
    ADD COLUMN IF NOT EXISTS order_code BIGINT,
    ADD COLUMN IF NOT EXISTS buyer_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS buyer_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS payment_provider VARCHAR(50),
    ADD COLUMN IF NOT EXISTS payment_link_id VARCHAR(255),
    ADD COLUMN IF NOT EXISTS checkout_url TEXT,
    ADD COLUMN IF NOT EXISTS qr_code TEXT,
    ADD COLUMN IF NOT EXISTS paid_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS cancelled_at TIMESTAMP;

UPDATE payments
SET payment_provider = COALESCE(provider, 'PAYOS')
WHERE payment_provider IS NULL;

ALTER TABLE payments
    ALTER COLUMN payment_provider SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_payments_order_code
    ON payments(order_code)
    WHERE order_code IS NOT NULL;
