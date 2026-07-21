CREATE TABLE IF NOT EXISTS saga_transaction_logs (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    step VARCHAR(80) NOT NULL,
    status VARCHAR(20) NOT NULL,
    message VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_saga_transaction_logs_order_id
    ON saga_transaction_logs(order_id, created_at);
