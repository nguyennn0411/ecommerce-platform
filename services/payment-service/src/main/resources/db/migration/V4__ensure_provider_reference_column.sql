ALTER TABLE payment_transactions
    ADD COLUMN IF NOT EXISTS provider_reference TEXT;

CREATE INDEX IF NOT EXISTS idx_payment_transactions_provider_reference
    ON payment_transactions(provider_reference);
