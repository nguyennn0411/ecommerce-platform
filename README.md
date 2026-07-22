# E-commerce Platform — Maven Multi-module Spring Boot Microservices

A comprehensive online shopping platform where users can browse products, place orders, and make payments.

## Architecture

```text
ecommerce-platform/
├── pom.xml
├── docker-compose.yml
├── docker-compose.infra.yml
├── infra/
│   ├── eureka-server/
│   ├── config-server/
│   └── api-gateway/
├── services/
│   ├── user-service/
│   ├── product-catalog-service/
│   ├── inventory-service/
│   ├── order-service/
│   ├── payment-service/
│   └── notification-service/
├── shared/
│   ├── common-events/
│   ├── common-web/
│   └── common-security/
└── config-repo/
```

## Services and default ports

| Module | Port | Responsibility |
|---|---:|---|
| eureka-server | 8761 | Service discovery |
| config-server | 8888 | Centralized configuration |
| api-gateway | 8080 | Routing and cross-cutting concerns |
| user-service | 8081 | Customer registration, login, profiles, addresses |
| product-catalog-service | 8082 | Products, categories, pricing, search |
| inventory-service | 8083 | Stock levels and reservations |
| order-service | 8084 | Order creation and Saga orchestration |
| payment-service | 8085 | Payment processing integrations |
| notification-service | 8086 | Email/SMS confirmations |

## Run locally

```bash
mvn clean install -DskipTests
docker compose -f docker-compose.infra.yml up -d
cd services/user-service
mvn spring-boot:run
```

## PayOS webhook for local development

PayOS must call a public HTTPS URL; a return URL alone cannot confirm a transfer. Start a tunnel to the API Gateway and set the URL before starting payment-service:

```bash
ngrok http 8080
set PAYOS_WEBHOOK_URL=https://<your-ngrok-domain>.ngrok-free.app/api/payments/payos/webhook
set PAYOS_AUTO_CONFIRM_WEBHOOK=true
```

payment-service confirms this URL with PayOS at startup. The Gateway accepts both `/api/payments/**` and `/api/v1/payments/**`; use the first path for PayOS callbacks.

## Smoke tests

```bash
curl http://localhost:8081/api/v1/users/ping
curl http://localhost:8082/api/v1/products/ping
curl http://localhost:8083/api/v1/inventory/ping
curl http://localhost:8084/api/v1/orders/ping
curl http://localhost:8085/api/v1/payments/ping
curl http://localhost:8086/api/v1/notifications/ping
```

## Saga challenge

The order process should eventually evolve into a distributed transaction using the Saga Pattern:

1. Validate user.
2. Validate products and prices.
3. Reserve inventory.
4. Create order.
5. Process payment.
6. Confirm order and send notification.
7. Compensate inventory/payment/order if a later step fails.
