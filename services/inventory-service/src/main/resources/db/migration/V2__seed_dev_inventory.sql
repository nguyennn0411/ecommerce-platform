-- Dev/test stock only. Same product_id = one catalog product; each row = one variant (size + color).
-- Product catalog owner (Thanh): align product UUIDs or replace these IDs when product-service has real data.

INSERT INTO inventory_items (id, product_id, size, color, quantity, reserved_quantity, status)
VALUES
    ('11111111-1111-1111-1111-111111111101', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '42', 'Black', 10, 0, 'IN_STOCK'),
    ('11111111-1111-1111-1111-111111111102', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '43', 'Black', 5, 0, 'IN_STOCK'),
    ('11111111-1111-1111-1111-111111111103', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa', '42', 'White', 3, 0, 'IN_STOCK'),
    ('11111111-1111-1111-1111-111111111201', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb', '40', NULL, 8, 0, 'IN_STOCK')
ON CONFLICT (id) DO NOTHING;
