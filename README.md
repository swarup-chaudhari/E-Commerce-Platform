# E-Commerce Microservices Platform

Scalable e-commerce back end split into independently deployable services that stay consistent through **event sourcing**
and a **choreography-based Saga** over Kafka — no distributed transactions.

```
                       ┌──────────────┐   JWT validation, routing (lb://)
 client ──────────────▶│ API Gateway  │──────────────┐
                       └──────┬───────┘              │
        Eureka (registry) + Config Server            ▼
                       ┌──────────────┐  sync pre-check   ┌──────────────────┐
                       │ Order        │──(Resilience4j)──▶│ Inventory        │── PostgreSQL (stock)
                       │ service      │                   │ service          │── MongoDB (catalog)
                       │ PostgreSQL + │                   └──────────────────┘
                       │ event store  │                   ┌──────────────────┐
                       └──────────────┘                   │ Payment service  │── PostgreSQL
                                                          └──────────────────┘
        Kafka + Schema Registry (Avro)                    ┌──────────────────┐
                                                          │ Notification svc │
                                                          └──────────────────┘
```

| Module | Port | Role |
|---|---|---|
| `service-registry` | 8761 | Eureka |
| `config-server` | 8888 | Spring Cloud Config (native profile, `src/main/resources/config`) |
| `api-gateway` | 8080 | Spring Cloud Gateway, JWT auth (`/auth/login`), RBAC, identity header propagation |
| `order-service` | 8081 | Orders, append-only event store, saga start/finish, Resilience4j client |
| `inventory-service` | 8082 | Stock in PostgreSQL (pessimistic row locks), catalog in MongoDB |
| `payment-service` | 8083 | Simulated PSP, refunds |
| `notification-service` | 8084 | Order lifecycle notifications (logging adapter) |
| `frontend` | 5173 dev / 3000 docker / 30000 k8s | React 18 + Vite SPA (see below) |
| `common` | – | Avro schemas → generated event classes, Kafka config, after-commit publisher |

## The saga

```
OrderCreated ─▶ inventory: reserve stock ─┬─▶ InventoryReserved ─▶ payment: charge ─┬─▶ PaymentCompleted ─▶ order CONFIRMED
(order-events)        (inventory-events)  │                       (payment-events)  │
                                          └─▶ InventoryReservationFailed ─▶ order REJECTED
                                                                                    └─▶ PaymentFailed ─▶ order REJECTED
                                                                                                    └─▶ inventory RELEASES stock   ← compensation
OrderCancelled ─▶ payment REFUNDS + inventory RELEASES stock                                         ← compensation
```
Note: the brief's example order was "deduct inventory → charge payment → confirm". The compensations are therefore
*release stock if payment fails* and *refund + release if the customer cancels*.

Design points
- **Ordering**: every record is keyed by `orderId`, so all events for one order land on one partition, in order. Each service has its own consumer group (`order-service`, `inventory-service`, …); `listener.concurrency: 3` with 6 partitions per topic lets a service scale to 6 consumers.
- **Idempotency**: Kafka is at-least-once, so each consumer records processed `eventId`s (`processed_events`) in the same DB transaction as its state change. One reservation / one payment per order is also enforced by primary/unique keys.
- **Event sourcing**: `order_events` is append-only; `GET /api/orders/{id}/events` returns the full history. The `orders` table is the current-state projection.
- **No event leaks**: events are published only *after* the DB transaction commits (`TransactionalKafkaPublisher`).
- **Failure isolation**: if Payment is down, `InventoryReserved` events simply wait in Kafka; payment consumes them on recovery and orders complete — nothing is lost.
- **Resilience4j** (order → inventory pre-check): retry → circuit breaker → rate limiter, with fail-open fallback so orders are still accepted as `PENDING` while inventory is down. Tune in `order-service/src/main/resources/application.yml`.
- **Poison messages**: 5 retries (1 s apart), deserialization errors are never retried, then logged and skipped (see hardening notes).

## Prerequisites
JDK 17+, Maven 3.9+, Docker (+ Compose v2), `jq` and `curl` for the smoke test; Minikube or Docker Desktop Kubernetes + `kubectl` for the cluster.
The first build needs internet access (Maven Central and `packages.confluent.io`).

## Run locally (IDE / `java -jar`)
```bash
(cd backend && mvn clean package -DskipTests)   # also generates Avro classes
docker compose up -d                   # Kafka (KRaft), Schema Registry :8085, Postgres, Mongo, Zipkin :9411, Prometheus :9090
# start in this order (separate terminals):
java -jar backend/service-registry/target/service-registry-1.0.0.jar
java -jar backend/config-server/target/config-server-1.0.0.jar        # optional: services start without it
java -jar backend/api-gateway/target/api-gateway-1.0.0.jar
java -jar backend/order-service/target/order-service-1.0.0.jar
java -jar backend/inventory-service/target/inventory-service-1.0.0.jar
java -jar backend/payment-service/target/payment-service-1.0.0.jar
java -jar backend/notification-service/target/notification-service-1.0.0.jar
./scripts/smoke-test.sh
```
Or run everything in containers: `docker compose --profile apps up -d --build`.

## Deploy to Kubernetes (Minikube / Docker Desktop)
```bash
minikube start --memory 6144 --cpus 4        # Docker Desktop: enable Kubernetes instead
eval $(minikube docker-env)                  # build images straight into the cluster's Docker daemon
./scripts/build-images.sh
./scripts/deploy-k8s.sh
kubectl -n ecommerce port-forward svc/api-gateway 8080:8080 &
./scripts/smoke-test.sh http://localhost:8080
kubectl -n ecommerce port-forward svc/frontend 3000:80      # web app: http://localhost:3000
```
Manifests (`k8s/`): namespace, ConfigMap/Secret, infra (Postgres, Mongo, Kafka KRaft, Schema Registry, Zipkin, Prometheus),
one Deployment+Service per microservice with startup/readiness/liveness probes and resource limits, and an HPA that scales
**only `order-service`** (2→10 pods on CPU). Only the gateway is exposed (NodePort 30080); everything else is ClusterIP,
which is what makes trusting the gateway-injected `X-User-*` headers safe.

Black-Friday drill: `kubectl -n ecommerce scale deploy/order-service --replicas=6`.
Failure drill: `kubectl -n ecommerce scale deploy/payment-service --replicas=0`, place orders (they stay `PENDING`),
then scale back to 2 and watch them become `CONFIRMED`.

## Frontend (React + Vite)
`frontend/` is a single-page app: sign in, browse the catalog (live stock), cart, checkout, **live order tracking** (polls while
`PENDING`, with a saga timeline built from the order's event history), cancel with refund, and an admin page for products and stock.
```bash
cd frontend && npm install && npm run dev      # http://localhost:5173  (proxies /api and /auth to the gateway on :8080)
```
Set `GATEWAY_URL=http://host:port npm run dev` if the gateway runs elsewhere. In Docker/Kubernetes an nginx image serves the build and
proxies `/api` and `/auth` to `api-gateway`, so the browser only ever talks to one origin (no CORS config needed).
Demo logins: `alice/alice123`, `bob/bob123`, `admin/admin123`. The JWT is kept in `localStorage` for simplicity; for production
prefer an httpOnly cookie via a BFF. Like the back end, the frontend has not been built in this environment.

## Observability
- **Metrics**: Micrometer → `/actuator/prometheus` on every service; Prometheus on `:9090` (circuit breaker state, HTTP, JVM, Kafka client metrics).
- **Tracing**: Micrometer Tracing (Brave) → Zipkin `:9411`. Kafka template/listener observation is on, so one trace spans gateway → order → Kafka → inventory → payment → order.
- **Logs**: every line carries `[service,traceId,spanId]`. Ship stdout with Loki/ELK/CloudWatch to centralise; searching by `traceId` reconstructs a request across services.


