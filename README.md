# 🎫 System Architecture PoC: High-Load Ticketing Service

Ticket Blaster is a robust backend application for selling and booking tickets. It is engineered to handle massive spikes in user traffic (e.g., ticket drops for major concerts) utilizing an Event-Driven Architecture (EDA) and asynchronous processing to ensure high availability and zero data loss.

## 🚀 Key Features & Engineering Patterns

* **High-Throughput Processing:** Leverages Redis for ultra-fast, in-memory seat availability checks, preventing double-booking during high-concurrency events.
* **Transactional Outbox Pattern:** Guarantees atomicity between local database transactions (PostgreSQL) and message publishing (Kafka), eliminating the dual-write problem.
* **Idempotency:** Implements idempotency keys to safely process retries during network failures, ensuring no duplicate charges.
* **Distributed Observability:** Integrated Micrometer Tracing and Zipkin to generate unified `traceId`s across all microservices, allowing for complete end-to-end request tracking.

## 🛠 Tech Stack

* **Backend:** Java 17, Spring Boot 3
* **Databases & Cache:** PostgreSQL, Flyway, Redis
* **Message Broker:** Apache Kafka
* **API Gateway:** Spring Cloud Gateway
* **Observability:** Micrometer Tracing, Zipkin
* **Performance Testing:** k6
* **Infrastructure:** Docker, Docker Compose

## 🧩 Microservices Architecture

The system is decoupled into four independent services:

* **`api-gateway`**: The single entry point for all client requests, managing routing and initiating the distributed trace context.
* **`booking-service`**: Validates incoming requests and utilizes Redis to quickly reserve seats. It asynchronously publishes booking events to Kafka.
* **`order-service`**: Consumes booking events, persists order details reliably in PostgreSQL, and orchestrates outbound events via the Outbox pattern.
* **`notification-service`**: An isolated background worker that listens for successful orders and emulates the dispatch of confirmation emails.

## 📊 Performance Testing Results

The architecture was stress-tested using **k6** to validate its resilience under heavy load. By offloading write operations to Kafka and utilizing Redis for immediate validation, the system easily absorbs traffic spikes.

Testing with **300 simultaneous users** yielded the following metrics:
* **Throughput:** > 1,627 Requests Per Second (RPS)
* **Volume:** > 162,800 requests processed
* **Latency:** p95 response time under 300 ms
* **Reliability:** 100% success rate (zero dropped requests or errors)
