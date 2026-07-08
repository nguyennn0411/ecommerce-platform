CREATE TABLE IF NOT EXISTS inventory_stocks (
    product_id UUID PRIMARY KEY,
    product_name VARCHAR(255) NOT NULL,
    available_quantity INTEGER NOT NULL,
    reserved_quantity INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
