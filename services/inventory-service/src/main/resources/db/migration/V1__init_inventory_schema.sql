-- Inventory schema for sneaker e-commerce (StepZone-style).
-- Each row = one sellable variant: product + size + color.
-- quantity       = physical stock on hand
-- reserved_quantity = units held for unpaid orders
-- available to sell = quantity - reserved_quantity

CREATE TABLE IF NOT EXISTS inventory_items (
    id UUID PRIMARY KEY,
    product_id UUID NOT NULL,
    size VARCHAR(20) NOT NULL,
    color VARCHAR(50),
    quantity INT NOT NULL DEFAULT 0,
    reserved_quantity INT NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'IN_STOCK',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT uq_inventory_items_product_size_color
        UNIQUE (product_id, size, color),
    CONSTRAINT chk_inventory_items_quantity_non_negative
        CHECK (quantity >= 0),
    CONSTRAINT chk_inventory_items_reserved_non_negative
        CHECK (reserved_quantity >= 0),
    CONSTRAINT chk_inventory_items_reserved_lte_quantity
        CHECK (reserved_quantity <= quantity)
);

CREATE TABLE IF NOT EXISTS stock_reservations (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    inventory_item_id UUID NOT NULL,
    quantity INT NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'RESERVED',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP,

    CONSTRAINT fk_stock_reservations_inventory
        FOREIGN KEY (inventory_item_id) REFERENCES inventory_items(id),
    CONSTRAINT chk_stock_reservations_quantity_positive
        CHECK (quantity > 0)
);

CREATE INDEX IF NOT EXISTS idx_inventory_items_product_id
    ON inventory_items(product_id);

CREATE INDEX IF NOT EXISTS idx_inventory_items_status
    ON inventory_items(status);

CREATE INDEX IF NOT EXISTS idx_stock_reservations_order_id
    ON stock_reservations(order_id);

CREATE INDEX IF NOT EXISTS idx_stock_reservations_order_status
    ON stock_reservations(order_id, status);

CREATE INDEX IF NOT EXISTS idx_stock_reservations_inventory_item_id
    ON stock_reservations(inventory_item_id);
