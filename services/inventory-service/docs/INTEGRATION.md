# Inventory Service — Tài liệu tích hợp (Order & Product)

> **Owner:** Tú Anh (inventory-service)  
> **Đọc bởi:** Phúc (order-service), Thành (product-catalog-service)  
> **Base URL (local):** `http://localhost:8083`  
> **API prefix:** `/api/v1/inventory`

---

## 1. Service này làm gì?

`inventory-service` quản lý **tồn kho theo biến thể giày** (variant):

- Một **sản phẩm catalog** (`product_id`) có nhiều dòng tồn: khác **size** và/hoặc **color**.
- Mỗi dòng `inventory_items` có `id` riêng (UUID).
- Hỗ trợ **saga đặt hàng**: giữ hàng → xác nhận trừ kho → hoàn giữ hàng khi hủy/thanh toán fail.

**Không làm:** giao hàng, trả hàng, QC, chọn kho theo địa chỉ.

---

## 2. Mô hình dữ liệu

### 2.1 `inventory_items` — tồn theo variant

| Cột | Ý nghĩa |
|-----|---------|
| `id` | PK — ID dòng tồn kho (không gửi từ order) |
| `product_id` | ID sản phẩm từ **product-catalog** (cùng model giày) |
| `size` | Size bán được (VD: `42`, `43`) — **bắt buộc** |
| `color` | Màu (VD: `Black`) — **optional**, `null` nếu không phân màu |
| `quantity` | Số lượng thật trên kệ |
| `reserved_quantity` | Đang giữ cho đơn chưa/chờ xử lý |
| `status` | `IN_STOCK`, `OUT_OF_STOCK`, `DISCONTINUED` |

**Unique:** `(product_id, size, color)` — mỗi variant một dòng.

**Available (có thể bán):** `quantity - reserved_quantity`

### 2.2 `stock_reservations` — giữ hàng theo đơn

| Cột | Ý nghĩa |
|-----|---------|
| `order_id` | ID đơn từ **order-service** |
| `product_id` | Copy từ item (để audit) |
| `inventory_item_id` | FK → dòng variant đã giữ |
| `quantity` | Số lượng giữ |
| `status` | `RESERVED` → `CONFIRMED` hoặc `RELEASED` |

### 2.3 Phân biệt ID (quan trọng)

```
product_id (catalog)     →  "Nike AF1" — chung cho mọi size/màu
inventory_items.id       →  từng dòng variant cụ thể (42 / Black)
order_id                 →  đơn hàng của Phúc
```

Order **không** gửi `inventory_items.id`. Order gửi **`product_id + size + color`** để inventory tìm đúng variant.

---

## 3. API cho Order Service (Feign)

Response **không** bọc `ApiResponse` — body JSON trực tiếp (Feign đọc field `reserved` / `success`).

### 3.1 Reserve — giữ hàng khi tạo đơn

```
POST /api/v1/inventory/reservations
Content-Type: application/json
```

**Request:**

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

| Field | Bắt buộc | Ghi chú |
|-------|----------|---------|
| `orderId` | Có | UUID đơn hàng |
| `items[].productId` | Có | UUID từ product-catalog |
| `items[].size` | Có | Trim, không rỗng |
| `items[].color` | Không | `null` hoặc bỏ field = variant không màu |
| `items[].quantity` | Có | ≥ 1 |

**Response:**

```json
{
  "reserved": true,
  "message": "Reserved stock for order ..."
}
```

| `reserved` | Ý nghĩa |
|------------|---------|
| `true` | Giữ hàng thành công (hoặc đã giữ trước đó — idempotent) |
| `false` | Không đủ hàng / variant không tồn tại / đơn đã confirm |

**Logic:** All-or-nothing — một item fail thì **không** giữ item nào.

### 3.2 Confirm — trừ tồn khi thanh toán OK

```
POST /api/v1/inventory/reservations/{orderId}/confirm
```

**Response:**

```json
{
  "success": true,
  "message": "Confirmed inventory deduction for order ..."
}
```

- Chỉ xử lý reservation `RESERVED`.
- `reserved_quantity` giảm, `quantity` giảm (đã bán).
- Gọi lại khi đã confirm → `success: true` (idempotent).

### 3.3 Release — hoàn giữ hàng

```
POST /api/v1/inventory/reservations/{orderId}/release?reason=Payment%20failed
```

**Response:**

```json
{
  "success": true,
  "message": "Released reserved stock for order ... (Payment failed)"
}
```

- Chỉ hoàn phần còn `RESERVED` (chưa confirm).
- `quantity` không đổi, `reserved_quantity` giảm.
- Dùng khi: thanh toán fail, hủy đơn trước khi trừ kho, saga compensate.

**Không** tự động cộng lại kho khi giao hàng thất bại (ngoài scope dự án).

---

## 4. API đọc tồn theo variant (Frontend)

Response bọc `ApiResponse` (giống product-catalog). **Không** có tổng tồn theo `productId` — chỉ theo variant (`size` + `color`), kiểu Shopee.

### 4.1 List variant stocks (khuyến nghị cho product page)

```
GET /api/v1/inventory/stocks/{productId}/variants
```

Frontend load 1 lần, chọn sẵn size/màu mặc định, đổi variant thì lookup trong `variants` (không gọi lại API).

**Response `data`:**

```json
{
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
}
```

| Field | Ý nghĩa |
|-------|---------|
| `availableQuantity` | `quantity - reservedQuantity` — hiển thị "Còn X đôi" |
| `inStock` | `true` khi `status = IN_STOCK` và `availableQuantity > 0` |

### 4.2 Một variant

```
GET /api/v1/inventory/stocks?productId={uuid}&size=42&color=Black
```

`color` optional (bỏ hoặc để trống = variant không màu).

Variant không có trong DB → `availableQuantity: 0`, `inStock: false` (HTTP 200).

### 4.3 Luồng UI gợi ý

1. `GET /api/v1/products/{productId}` — lấy danh sách size/color từ catalog
2. `GET /api/v1/inventory/stocks/{productId}/variants` — lấy tồn từng variant
3. Chọn sẵn variant đầu tiên → hiện `availableQuantity` ngay
4. User đổi size/màu → cập nhật từ list đã load

---

## 5. Luồng Saga (Order ↔ Inventory)

```
[Tạo đơn]
  Order → POST /reservations (productId + size + color + qty)
  Inventory → reserved_quantity ↑

[Thanh toán OK]
  Order → POST /reservations/{orderId}/confirm
  Inventory → quantity ↓, reserved_quantity ↓

[Thanh toán FAIL / Hủy đơn (chưa confirm)]
  Order → POST /reservations/{orderId}/release
  Inventory → reserved_quantity ↓ (quantity giữ nguyên)
```

---

## 6. Order Service (Phúc) — cần làm gì?

### 6.1 Cập nhật Feign DTO (bắt buộc để khớp)

File `order-service/.../integration/ReserveInventoryItemRequest.java` hiện chỉ có `productId`, `quantity`.

**Cần thêm:**

```java
public record ReserveInventoryItemRequest(
        UUID productId,
        String size,      // @NotBlank khi gửi
        String color,     // optional
        Integer quantity
) {}
```

### 6.2 `OrderItem` + Create Order API

Lưu và gửi khi reserve:

- `productId`
- `size` (bắt buộc)
- `color` (optional)

`OrderSagaOrchestrator.toReserveInventoryItemRequest()` map đủ 4 field.

### 6.3 `InventoryClient`

Path đã đúng, chỉ cần body item có `size`/`color`:

```java
@FeignClient(name = "inventory-service", path = "/api/v1/inventory")
// POST /reservations
// POST /reservations/{orderId}/confirm
// POST /reservations/{orderId}/release
```

### 6.4 Bật gọi thật

`application.yml`:

```yaml
ecommerce:
  saga:
    external-services-enabled: true
```

Feign trỏ `inventory-service` → `http://localhost:8083` (hoặc Eureka khi có).

### 6.5 Checklist Phúc

- [ ] `ReserveInventoryItemRequest` có `size`, `color`
- [ ] `OrderItem` + migration order DB có `size`, `color`
- [ ] Create order API nhận size/color từ client
- [ ] Saga map đủ field khi gọi reserve
- [ ] Test: tạo đơn → reserve → pay → confirm / pay fail → release

---

## 7. Product Catalog (Thành) — cần làm gì?

Inventory **không** validate tên/giá sản phẩm — order saga gọi product trước khi reserve.

Product service nên:

1. **`product_id` ổn định** — UUID catalog mà order/inventory dùng chung.
2. **Mô tả variant** — API product trả về danh sách size/color (hoặc SKU) để frontend biết gửi gì khi đặt hàng.
3. **Đồng bộ seed** — khi có data thật, tạo dòng `inventory_items` tương ứng (hoặc API nhập kho sau).

### Seed dev hiện tại (V2 migration)

| product_id | size | color | quantity |
|------------|------|-------|----------|
| `aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa` | 42 | Black | 10 |
| `aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa` | 43 | Black | 5 |
| `aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa` | 42 | White | 3 |
| `bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb` | 40 | null | 8 |

Thành có thể thay UUID bằng ID product thật trong catalog — **nhớ báo team** để cập nhật seed hoặc insert inventory.

### Checklist Thành

- [x] Product API expose `id`, variants (size, color) nếu có — xem `services/product-catalog-service/docs/INTEGRATION.md`
- [x] Document UUID sản phẩm dùng cho test tích hợp (seed AF1 / Samba)
- [ ] (Tuỳ chọn) Event/webhook khi tạo sản phẩm mới → inventory tạo dòng tồn 0

---

## 8. Test nhanh (Postman / curl)

**List variant stocks (product page):**

```bash
curl http://localhost:8083/api/v1/inventory/stocks/aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa/variants
```

**Một variant:**

```bash
curl "http://localhost:8083/api/v1/inventory/stocks?productId=aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa&size=42&color=Black"
```

**Reserve OK:**

```bash
curl -X POST http://localhost:8083/api/v1/inventory/reservations \
  -H "Content-Type: application/json" \
  -d "{\"orderId\":\"550e8400-e29b-41d4-a716-446655440000\",\"items\":[{\"productId\":\"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\",\"size\":\"42\",\"color\":\"Black\",\"quantity\":1}]}"
```

**Confirm:**

```bash
curl -X POST http://localhost:8083/api/v1/inventory/reservations/550e8400-e29b-41d4-a716-446655440000/confirm
```

**Release:**

```bash
curl -X POST "http://localhost:8083/api/v1/inventory/reservations/550e8400-e29b-41d4-a716-446655440000/release?reason=Payment%20failed"
```

**Reserve fail (hết hàng):** quantity > available hoặc sai size/color → `"reserved": false`.

---

## 9. Lỗi & HTTP status

| Tình huống | HTTP | Body |
|------------|------|------|
| Reserve fail (hết hàng) | 200 | `reserved: false` |
| JSON thiếu/sai | 400 | `ApiResponse` từ `common-web` |
| Confirm/release không có reservation | 200 | `success: false` |

Order saga nên check `reserved()` / `success()` như code hiện tại.

---

## 10. Flyway & Docker init

- Schema: `V1__init_inventory_schema.sql`
- Seed dev: `V2__seed_dev_inventory.sql`
- Docker `infra/postgres/init/02-init-schemas.sql` cũng tạo bảng inventory — **giữ đồng bộ** với V1 (đã bỏ `warehouse_location`).

Sau `down -v` + `up`, chạy app để Flyway apply V2 seed.

---

## 11. Liên hệ / thay đổi contract

Mọi thay đổi field API reserve → sync **order Feign DTO** + tài liệu này.

Owner inventory: **Tú Anh**
