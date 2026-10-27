# API Contracts

All client traffic goes through the **API Gateway** (`:8080`). Every `/api/**` call requires
`Authorization: Bearer <JWT>`. The gateway validates the token, strips any client-sent identity headers and
forwards `X-User-Id` / `X-User-Roles` to the services. Amounts are **integer cents**.

## Auth

### `POST /auth/login`  (public)
```json
{ "username": "alice", "password": "alice123" }
```
`200` → `{ "accessToken": "<jwt>", "tokenType": "Bearer", "expiresInSeconds": 3600 }` · `401` bad credentials

| User  | Password   | Roles        |
|-------|------------|--------------|
| alice | alice123   | USER         |
| bob   | bob123     | USER         |
| admin | admin123   | USER, ADMIN  |

JWT claims: `sub` (username), `roles` (array), `iat`, `exp`, `iss=ecommerce-gateway` — HS256.

## Orders (order-service)

### `POST /api/orders` → `202 Accepted`
```json
{ "items": [ { "productId": "P-1001", "quantity": 2, "unitPriceCents": 4999 } ] }
```
Response (`Location: /api/orders/{id}`):
```json
{ "id": "…", "customerId": "alice", "status": "PENDING", "totalAmountCents": 9998,
  "failureReason": null, "createdAt": "…", "updatedAt": "…", "items": [ … ] }
```
Errors: `400` validation · `409` insufficient stock (sync pre-check): `{"detail": "...", "unavailableProductIds": ["P-1003"]}`

The order is **asynchronous**: poll `GET /api/orders/{id}` until `status` leaves `PENDING`.

| Status     | Meaning                                                   |
|------------|-----------------------------------------------------------|
| PENDING    | saga in progress (or waiting for a recovering service)    |
| CONFIRMED  | stock reserved and payment captured                       |
| REJECTED   | stock unavailable or payment declined (`failureReason`)   |
| CANCELLED  | cancelled by customer (payment refunded, stock released)  |

### `GET /api/orders` — current user's orders (newest first)
### `GET /api/orders/{id}` — owner or ADMIN · `403` otherwise · `404` unknown
### `GET /api/orders/{id}/events` — the order's event-sourced history
```json
[ { "sequence": 1, "eventType": "OrderCreatedEvent", "payload": "{…}", "occurredAt": "…" }, … ]
```
### `POST /api/orders/{id}/cancel` — allowed while `PENDING` or `CONFIRMED`; `409` otherwise

## Catalog (inventory-service, MongoDB)
| Method | Path                  | Access | Notes |
|--------|-----------------------|--------|-------|
| GET    | `/api/products`       | any    | optional `?category=` |
| GET    | `/api/products/{id}`  | any    | |
| POST   | `/api/products`       | ADMIN  | body: `{id,name,description,priceCents,category,attributes{}}` · `409` if id exists |
| PUT    | `/api/products/{id}`  | ADMIN  | replace |
| DELETE | `/api/products/{id}`  | ADMIN  | |

## Stock (inventory-service, PostgreSQL)
| Method | Path                              | Access | Notes |
|--------|-----------------------------------|--------|-------|
| GET    | `/api/inventory/{productId}`      | any    | `{productId, availableQuantity}` |
| PUT    | `/api/inventory/{productId}/stock`| ADMIN  | body `{ "quantity": 100 }` (upsert) |
| POST   | `/api/inventory/check`            | internal | used by order-service: `{items:[{productId,quantity}]}` → `{checked, unavailableProductIds[]}` |

## Payments (payment-service)
### `GET /api/payments/order/{orderId}` — owner or ADMIN
`{ id, orderId, amountCents, status: COMPLETED|FAILED|REFUNDED|CANCELLED, failureReason, … }`
Demo rule: charges above `app.payment.decline-above-cents` (default 1,000,000 = $10,000.00) are declined.

## Kafka event contracts (Avro, namespace `com.zaalima.ecommerce.events`)

| Topic              | Key     | Event (producer)                                                        | Consumers |
|--------------------|---------|--------------------------------------------------------------------------|-----------|
| `order-events`     | orderId | `OrderCreatedEvent` (order)                                             | inventory, notification |
|                    |         | `OrderCancelledEvent` (order)                                           | inventory, payment |
|                    |         | `OrderConfirmedEvent` / `OrderRejectedEvent` (order)                    | notification |
| `inventory-events` | orderId | `InventoryReservedEvent` / `InventoryReservationFailedEvent` (inventory) | payment / order |
| `payment-events`   | orderId | `PaymentCompletedEvent` (payment)                                       | order |
|                    |         | `PaymentFailedEvent` (payment)                                          | order, inventory |

Schemas live in `common/src/main/avro/*.avsc`; subjects use `RecordNameStrategy`, so several event types share a topic
and are routed by `@KafkaHandler`. Every event carries a unique `eventId` used for idempotent consumption.

## Operational endpoints (every service)
`/actuator/health/{liveness,readiness}` · `/actuator/prometheus` · `/actuator/metrics`
