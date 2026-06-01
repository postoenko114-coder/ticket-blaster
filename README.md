# Ticket Blaster

Ticket Blaster is an MVP backend for high-demand ticket drops. The goal is to accept booking requests quickly, protect ticket inventory from overselling, and process orders asynchronously through Kafka.

This repository is intentionally built as a small distributed system rather than a single CRUD service. It demonstrates service boundaries, Redis-based inventory reservation, Kafka messaging with versioned event envelopes, transactional outbox, idempotency keys, database migrations, observability hooks, Docker Compose, and automated tests.

## Architecture

Services:

- `api-gateway` - single public entry point, routes client traffic to booking APIs.
- `booking-service` - validates booking requests, reserves ticket quantity in Redis, and publishes order events to Kafka.
- `order-service` - consumes booking events, stores orders in PostgreSQL, and writes notification events through the outbox pattern.
- `notification-service` - consumes notification events, sends confirmation emails, and stores delivery status for audit/debugging.

Infrastructure:

- Separate PostgreSQL databases for durable order storage and notification delivery tracking.
- Redis for fast ticket availability counters.
- Kafka for asynchronous service communication.
- Flyway for database schema migrations.
- Zipkin/Micrometer tracing for request visibility.
- k6 for load testing.

Runtime:

- Java 21 LTS.
- Spring Boot virtual threads are enabled for blocking services that interact with Redis, PostgreSQL, Kafka acknowledgements, and SMTP.

## Why These Patterns Matter

Redis reservation keeps the critical booking path fast and atomic: decrementing a counter is much cheaper than locking rows under heavy traffic.

Idempotency keys protect the system from duplicate client retries. `booking-service` reserves the idempotency key in Redis before decrementing ticket inventory, rejects key reuse with a different payload, and treats completed duplicate requests as idempotent replays. `order-service` also stores the key with a unique constraint as a second layer of protection.

Kafka event envelopes keep service contracts explicit. Each Kafka message includes metadata such as `eventId`, `eventType`, `schemaVersion`, `occurredAt`, and `aggregateId`, while the actual business data stays inside `payload`. Consumers validate the event type and schema version before processing.

Transactional outbox avoids the dual-write problem. `order-service` saves the order and the outgoing event in the same database transaction. A scheduled publisher then sends unprocessed events to Kafka and marks them processed only after Kafka acknowledges the send.

Dead-letter topics make poison messages visible. `order-service` retries failed order messages and publishes unrecoverable records to `<topic>.DLT` instead of losing them silently.

Notification delivery tracking makes email side effects observable. `notification-service` stores each notification event with `RECEIVED`, `SENT`, or `FAILED` status, attempt count, timestamps, and the last error. That turns SMTP problems into queryable operational data instead of hidden log lines.

Java 21 virtual threads reduce the cost of blocking I/O. They do not make CPU-heavy work faster, but they allow the services to handle many concurrent requests while some operations are waiting on Redis, PostgreSQL, Kafka, or SMTP. This keeps the code simple and synchronous while improving thread scalability for I/O-heavy workloads.

## Run Locally

Create environment variables:

```bash
cp .env.example .env
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Build and test:

```bash
./mvnw test
./mvnw package -DskipTests
```

On Windows PowerShell:

```powershell
.\mvnw.cmd test
.\mvnw.cmd package -DskipTests
```

If Docker Desktop uses the `desktop-linux` context on Windows and Testcontainers cannot find Docker, run tests with:

```powershell
$env:DOCKER_HOST='npipe:////./pipe/docker_engine_linux'
.\mvnw.cmd test
```

Start infrastructure and services:

```bash
docker compose up --build
```

Useful local URLs:

- API Gateway: `http://localhost:8080`
- Kafka UI: `http://localhost:8090`
- Zipkin: `http://localhost:9411`
- Booking Swagger UI: `http://localhost:8081/swagger-ui.html`
- Notification Swagger UI: `http://localhost:8083/swagger-ui.html`
- Order PostgreSQL: `localhost:5432/order_db`
- Notification PostgreSQL: `localhost:5433/notification_db`
- Booking readiness: `http://localhost:8081/actuator/health/readiness`
- Order readiness: `http://localhost:8082/actuator/health/readiness`
- Notification readiness: `http://localhost:8083/actuator/health/readiness`

## Demo Flow

Initialize tickets for an event:

```bash
curl -X POST "http://localhost:8080/api/book/init?eventId=1&count=1000"
```

Book tickets:

```bash
curl -X POST "http://localhost:8080/api/book" \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: demo-user-100-event-1-attempt-1" \
  -d '{
    "userId": 100,
    "userEmail": "buyer@example.com",
    "eventId": 1,
    "quantity": 2
  }'
```

Expected response: `202 Accepted`. The booking request is accepted, then processed asynchronously by `order-service`.

Inspect notification delivery status:

```bash
curl "http://localhost:8083/api/notifications/deliveries"
curl "http://localhost:8083/api/notifications/deliveries?status=FAILED"
```

## Load Test

After services are running:

```bash
k6 run load-test.js
```

The k6 script initializes ticket inventory, sends concurrent booking requests through the gateway, and expects `202 Accepted` responses.

## Verification

Current automated checks:

```bash
./mvnw test
```

Covered so far:

- Redis ticket counter is decremented when stock is available.
- Redis counter is rolled back when a booking would oversell.
- Ticket initialization stores availability with TTL.
- Booking idempotency handles new requests, completed replays, in-progress duplicates, and key reuse with a different payload.
- Duplicate order idempotency keys do not create new orders.
- New orders create versioned outbox events with operational metadata.
- Booking controller returns expected HTTP statuses for success, validation errors, sold-out inventory, and Kafka publishing failures.
- Kafka listeners deserialize messages and delegate to business services.
- Notification listener builds email messages and handles invalid payloads or SMTP failures.
- Notification delivery tracking records sent and failed emails.
- Spring application contexts load for booking and notification services.

Integration tests:

- `TicketServiceRedisIntegrationTest` uses a real `redis:7-alpine` container.
- `OrderRepositoryIntegrationTest` uses a real `postgres:15-alpine` container.
- `OutboxEventRepositoryIntegrationTest` uses a real `postgres:15-alpine` container.
- `KafkaOrderFlowIntegrationTest` uses real Kafka and PostgreSQL containers to verify the order flow and dead-letter handling.
- `NotificationDeliveryRepositoryIntegrationTest` uses a real `postgres:15-alpine` container.

These tests are based on Testcontainers. If Docker Desktop is not running, they are skipped so the unit test suite still works. Start Docker Desktop and run `./mvnw test` again to execute them against real containers.

## Current MVP Limits

- There is no user-facing frontend yet.
- Event and venue management are still represented by `eventId` instead of full domain entities.
- Email delivery is best-effort in local development unless real SMTP credentials are configured.
- Notification delivery has a status table and API, but not a polished dashboard UI yet.
