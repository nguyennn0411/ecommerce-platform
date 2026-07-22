# Inventory Service — Integration Guide

> **Owner:** Tú Anh  
> **Consumers:** Product Catalog (Thành), Order (Phúc), Frontend  
> **Base URL (local):** `http://localhost:8083`  
> **API prefix:** `/api/v1/inventory`

---

## 1. Scope

Inventory quản lý **tồn kho theo variant** `(productId + size + color)`:

| Có | Không |
|----|--------|
| Đọc tồn (GET stocks) | CRUD sản phẩm / giá / ảnh |
| Nhập/sửa tồn (PUT stocks) | Cart, thanh toán, giao hàng |
| Saga: reserve → confirm / release | Validate tên/giá (product làm) |

**Available bán được:** `quantity - reserved_quantity`

Order và Staff **không** gửi `inventory_items.id` — luôn gửi `productId + size + color`.

---

## 2. Data model

### `inventory_items`

| Column | Notes |
|--------|--------|
| `id` | UUID PK |
| `product_id` | Logical ref → catalog |
| `size` | Required |
| `color` | Optional (`null` = no color) |
| `quantity` | On-hand stock |
| `reserved_quantity` | Held for open orders |
| `status` | `IN_STOCK` / `OUT_OF_STOCK` / `DISCONTINUED` |

**Unique:** `(product_id, size, color)`

### `stock_reservations`

| Column | Notes |
|--------|--------|
| `order_id` | Logical ref → order |
| `product_id` | Audit copy |
| `inventory_item_id` | FK → `inventory_items` |
| `quantity` | Held qty |
| `status` | `RESERVED` → `CONFIRMED` \| `RELEASED` |

---

## 3. Stocks API (Product Catalog + Frontend)

Response bọc **`ApiResponse`**.

Normalize: `size` trim bắt buộc; `color` trim, rỗng → `null`.

### 3.1 List variants (product page)

```
GET /api/v1/inventory/stocks/{productId}/variants
```

```json
{
  "success": true,
  "data": {
    "productId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
    "variants": [
      {
        "productId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
        "size": "42",
        "color": "Black",
        "quantity": 10,
        "reservedQuantity": 0,
        "availableQuantity": 10,
        "status": "IN_STOCK",
        "inStock": true
      }
    ]
  },
  "message": "OK"
}
```

| Field | Meaning |
|-------|---------|
| `availableQuantity` | `quantity - reservedQuantity` |
| `inStock` | `status = IN_STOCK` **and** `availableQuantity > 0` |

### 3.2 One variant

```
GET /api/v1/inventory/stocks?productId={uuid}&size=42&color=Black
```

`color` optional. Not found → HTTP 200, `availableQuantity: 0`, `inStock: false`.

### 3.3 Upsert one variant (Staff / after create product)

```
PUT /api/v1/inventory/stocks
```

```json
{
  "productId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "size": "42",
  "color": "Black",
  "quantity": 20
}
```

| Field | Required | Notes |
|-------|----------|--------|
| `productId` | Yes | Catalog UUID |
| `size` | Yes | Non-blank |
| `color` | No | Omit / null / blank → no color |
| `quantity` | Yes | ≥ 0, **absolute** set (not increment) |

- Missing row → create (`reservedQuantity = 0`)
- Existing → set `quantity`
- `quantity < reservedQuantity` → **HTTP 400**
- `available > 0` → `IN_STOCK`, else `OUT_OF_STOCK`
- Response `data` = same shape as GET variant

### 3.4 Bulk upsert (recommended for create-product form)

```
PUT /api/v1/inventory/stocks/bulk
```

```json
{
  "productId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "items": [
    { "size": "42", "color": "Black", "quantity": 10 },
    { "size": "43", "color": "Black", "quantity": 5 },
    { "size": "42", "color": "White", "quantity": 3 }
  ]
}
```

All-or-nothing transaction. Response `data` = `{ productId, variants: [...] }`.

### 3.5 Handshake with Product Catalog

```
1. POST /api/v1/products              → productId + size/color list
2. PUT  /api/v1/inventory/stocks/bulk → set quantities
3. GET  /api/v1/inventory/stocks/{productId}/variants → verify
```

Catalog **does not** store stock. Size/color strings must match exactly (after trim).

---

## 4. Reservations API (Order saga)

Response **raw JSON** (no `ApiResponse`) — Feign reads `reserved` / `success`.

### 4.1 Reserve

```
POST /api/v1/inventory/reservations
```

```json
{
  "orderId": "550e8400-e29b-41d4-a716-446655440000",
  "items": [
    {
      "productId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
      "size": "42",
      "color": "Black",
      "quantity": 2
    }
  ]
}
```

```json
{ "reserved": true, "message": "..." }
```

- All-or-nothing
- Idempotent if already `RESERVED` for `orderId`
- `reserved: false` if not enough stock / unknown variant / already confirmed

### 4.2 Confirm (payment OK)

```
POST /api/v1/inventory/reservations/{orderId}/confirm
```

```json
{ "success": true, "message": "..." }
```

Decrements `quantity` and `reserved_quantity`. Idempotent if already confirmed.

### 4.3 Release (pay fail / cancel / compensate)

```
POST /api/v1/inventory/reservations/{orderId}/release?reason=Payment%20failed
```

```json
{ "success": true, "message": "..." }
```

Only `RESERVED` rows: `reserved_quantity` ↓, `quantity` unchanged.

### 4.4 Saga flow

```
Create order  → POST /reservations          → reserved ↑
Payment OK    → POST .../confirm            → quantity ↓, reserved ↓
Pay fail/Cancel → POST .../release          → reserved ↓
```

---

## 5. HTTP status

| Case | HTTP | Body |
|------|------|------|
| Reserve fail (no stock) | 200 | `{ "reserved": false, ... }` |
| Confirm/release no active reservation | 200 | `{ "success": false, ... }` |
| Upsert quantity &lt; reserved | 400 | `ApiResponse` error |
| Invalid JSON / validation | 400 | `ApiResponse` error |

---

## 6. Curl cheat sheet

```bash
# Set stock after create product
curl -X PUT http://localhost:8083/api/v1/inventory/stocks/bulk \
  -H "Content-Type: application/json" \
  -d "{\"productId\":\"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\"items\":[{\"size\":\"42\",\"color\":\"Black\",\"quantity\":20}]}"

# Read stock
curl http://localhost:8083/api/v1/inventory/stocks/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/variants

# Reserve
curl -X POST http://localhost:8083/api/v1/inventory/reservations \
  -H "Content-Type: application/json" \
  -d "{\"orderId\":\"550e8400-e29b-41d4-a716-446655440000\",\"items\":[{\"productId\":\"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\"size\":\"42\",\"color\":\"Black\",\"quantity\":1}]}"

# Confirm / Release
curl -X POST http://localhost:8083/api/v1/inventory/reservations/550e8400-e29b-41d4-a716-446655440000/confirm
curl -X POST "http://localhost:8083/api/v1/inventory/reservations/550e8400-e29b-41d4-a716-446655440000/release?reason=Payment%20failed"
```

---

## 7. Dev seed & migrations

| File | Role |
|------|------|
| `V1__init_inventory_schema.sql` | Tables |
| `V2__seed_dev_inventory.sql` | Demo rows |
| `V3__seed_extra_inventory.sql` | Extra seed (if present) |

Demo `product_id` values must stay aligned with product-catalog seed (e.g. Nike AF1 `aaaaaaaa-...`, Samba `bbbbbbbb-...`).

After `docker compose down -v` + up, start the app so Flyway applies seeds.

---

## 8. Contract change

Any change to stocks or reservations request/response fields → update this doc and notify Product / Order owners.

**Owner:** Tú Anh
