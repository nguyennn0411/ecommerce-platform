# Order Service Demo Note

## Scope da lam

- `order-service` da co API tao order va lay order.
- Khi tao order, service se:
  1. luu order vao database
  2. goi `inventory-service` de reserve stock
  3. goi `payment-service` de tao payment
  4. nghe event `payment.success`, `payment.failed`, `payment.cancelled`
  5. cap nhat trang thai order tuong ung
- Neu payment fail hoac bi huy, `order-service` se goi release inventory de compensation.

## Flow demo

1. seed stock cho `inventory-service`
2. tao order tu `order-service`
3. he thong reserve inventory
4. he thong tao payment
5. xem order da co `paymentId`, `checkoutUrl`, `status = PAYMENT_PENDING`
6. khi nhan event payment:
   - `payment.success` => order `CONFIRMED`
   - `payment.failed` => order `FAILED` va release inventory
   - `payment.cancelled` => order `CANCELLED` va release inventory

## Diem de noi khi bao cao

- Day la `Saga orchestration`, khong phai 2PC.
- `order-service` dong vai tro orchestrator.
- Giao tiep giua service dang ket hop:
  - sync REST: order -> inventory, order -> payment
  - async messaging: payment -> order
- Compensation hien tai da co nhanh co ban cho payment fail/cancel.

## Tien do co the bao cao

- Tuan 2-5: da co `order-service` voi REST + database rieng.
- Tuan 6: da co inter-service communication voi inventory va payment.
- Tuan 7: da co Saga orchestration co ban va compensation flow.
- Chua xong hoan toan:
  - resilience/circuit breaker
  - observability sau hon
  - test integration day du

## Luu y khi demo

- Can co stock truoc khi tao order.
- `payment-service` can chay de order tao payment thanh cong.
- RabbitMQ can chay neu muon demo cap nhat trang thai order bang event.
- Neu ben inventory doi contract API, can sua lai client trong `order-service`.
