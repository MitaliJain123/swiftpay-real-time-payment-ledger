# SwiftPay - Real-Time Payment Ledger

SwiftPay is a real-time peer-to-peer payment ledger system built with Java 21 and Spring Boot, using an event-driven microservice architecture.

## Architecture

```
Client
  |
  |  POST /v1/payments
  v
+---------------------+     payment-initiated    +----------------+    payment-completed    +------------------+
| Transaction Gateway | ---------(Kafka)-------> | Ledger Service | --------(Kafka)-------> | Analytics Worker |
|      :8080          |                          |     :8081      |                         |      :8082       |
+---------------------+                          +----------------+                         +------------------+
  |          |                                     |     |                                          |
  v          v                                     v     v                                          v
Redis     PostgreSQL  <------------------------ PostgreSQL  + payment-failed /               ClickHouse (OLAP)
(idempotency)              (shared swiftpay DB)               payment-initiated.DLT          volume monitoring
```

### Services

* **Transaction Gateway (`:8080`)** — accepts payment requests, validates them, enforces idempotency via Redis (atomic `SET NX`), persists a `PENDING` payment, and publishes a `payment-initiated` Kafka event.
* **Ledger Service (`:8081`)** — consumes `payment-initiated`, locks both users (in ascending id order to avoid deadlocks), performs the authoritative balance check, writes double-entry ledger records (DEBIT/CREDIT), marks the payment `COMPLETED`/`FAILED`, and publishes `payment-completed` / `payment-failed` events. Failed messages are retried 3 times and then routed to the `payment-initiated.DLT` dead-letter topic.
* **Analytics Worker (`:8082`)** — consumes `payment-completed` events and writes them to ClickHouse (OLAP) for real-time volume monitoring. The table is a `ReplacingMergeTree` keyed by `transaction\_id`, so redelivered Kafka events never double-count a payment. Exposes volume summary and per-minute timeseries endpoints.

### Technology Stack

* Java 21, Spring Boot
* PostgreSQL (shared ledger database)
* ClickHouse (OLAP analytics store)
* Apache Kafka (KRaft mode, no ZooKeeper)
* Redis (idempotency keys)
* Docker / Docker Compose

\---

## Prerequisites

The only thing you need installed is **Docker Desktop** (or Docker Engine + Compose v2 on Linux):

* Windows/Mac: https://www.docker.com/products/docker-desktop/
* Verify it works:

```bash
docker --version
docker compose version
```

You do **not** need Java, Maven, PostgreSQL, Kafka, or Redis installed — everything runs in containers, and the app images are built inside Docker.

\---

## Setup \& Start (step by step)

### 1\. Get the code

```bash
git clone <repository-url>
cd swiftpay-real-time-payment-ledger
```

### 2\. Build and start everything

From the repository root (the folder containing `docker-compose.yml`):

```bash
docker compose up -d --build
```

The first run takes several minutes (it downloads base images and Maven dependencies). Subsequent runs are fast because layers are cached.

### 3\. Wait until all containers are healthy

```bash
docker compose ps
```

Expected output — all seven containers `Up (healthy)`:

```
NAME                           STATUS
swiftpay-analytics-worker      Up (healthy)
swiftpay-clickhouse            Up (healthy)
swiftpay-kafka                 Up (healthy)
swiftpay-ledger                Up (healthy)
swiftpay-postgres              Up (healthy)
swiftpay-redis                 Up (healthy)
swiftpay-transaction-gateway   Up (healthy)
```

The ledger starts last by design (it waits for the gateway to create and seed the shared database schema). Give it \~1–2 minutes after `up`.

### 4\. Verify the services respond

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

All should return `"status":"UP"`.

### Ports used

|Port|What|
|-|-|
|8080|Transaction Gateway API|
|8081|Ledger Service API|
|8082|Analytics Worker API|
|8123|ClickHouse HTTP interface|
|5433|PostgreSQL (host access; internal 5432)|
|6380|Redis (host access; internal 6379)|
|29092|Kafka (host access; internal 9092)|

Postgres/Redis are deliberately mapped to non-default host ports (5433/6380) so they don't clash with a locally installed Postgres/Redis.

### Seeded demo users

Three users are inserted automatically on first startup:

|ID|Name|Balance|Currency|
|-|-|-|-|
|1|Alice|10000.00|INR|
|2|Bob|5000.00|INR|
|3|Charlie|750.00|INR|

\---

## API Testing — all curl commands

> \*\*Windows note:\*\* the commands below are for bash / Git Bash / WSL. In native PowerShell, `curl` is an alias for `Invoke-WebRequest` and single quotes around JSON behave differently — use the PowerShell equivalents shown at the end of this section, or run these in Git Bash.

### 1\. Health checks

```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8082/actuator/health
```

### 2\. Create a payment (happy path)

Alice (id 1) sends 250 INR to Bob (id 2):

```bash
curl -X POST http://localhost:8080/v1/payments \\
  -H "Content-Type: application/json" \\
  -d '{
    "transactionId": "txn-001",
    "senderId": 1,
    "receiverId": 2,
    "amount": 250.00,
    "currency": "INR"
  }'
```

Response (`202 Accepted`):

```json
{"transactionId":"txn-001","status":"PENDING","message":"Payment initiated successfully"}
```

The payment settles asynchronously via Kafka — usually within a second.

### 3\. Check transaction history (ledger entries)

```bash
# Alice - should show a DEBIT of 250, balanceAfter 9750.00
curl http://localhost:8081/v1/users/1/transactions

# Bob - should show a CREDIT of 250, balanceAfter 5250.00
curl http://localhost:8081/v1/users/2/transactions
```

### 4\. Idempotency — send the SAME transactionId again

```bash
curl -X POST http://localhost:8080/v1/payments \\
  -H "Content-Type: application/json" \\
  -d '{
    "transactionId": "txn-001",
    "senderId": 1,
    "receiverId": 2,
    "amount": 250.00,
    "currency": "INR"
  }'
```

Response — no double charge, returns the final status:

```json
{"transactionId":"txn-001","status":"COMPLETED","message":"Transaction already processed"}
```

### 5\. Insufficient funds

Charlie (id 3, balance 750) tries to send 10000:

```bash
curl -X POST http://localhost:8080/v1/payments \\
  -H "Content-Type: application/json" \\
  -d '{
    "transactionId": "txn-002",
    "senderId": 3,
    "receiverId": 1,
    "amount": 10000.00,
    "currency": "INR"
  }'
```

Response (`400 Bad Request`):

```json
{"error":"INSUFFICIENT\_FUNDS","message":"Insufficient funds","status":400}
```

### 6\. Sender and receiver are the same

```bash
curl -X POST http://localhost:8080/v1/payments \\
  -H "Content-Type: application/json" \\
  -d '{
    "transactionId": "txn-003",
    "senderId": 1,
    "receiverId": 1,
    "amount": 100.00,
    "currency": "INR"
  }'
```

Response (`400`): `{"error":"PAYMENT\_ERROR","message":"Sender and receiver cannot be the same",...}`

### 7\. Unknown user

```bash
curl -X POST http://localhost:8080/v1/payments \\
  -H "Content-Type: application/json" \\
  -d '{
    "transactionId": "txn-004",
    "senderId": 999,
    "receiverId": 1,
    "amount": 100.00,
    "currency": "INR"
  }'
```

Response (`400`): `{"error":"PAYMENT\_ERROR","message":"Sender not found",...}`

### 8\. Validation error (missing/invalid fields)

```bash
curl -X POST http://localhost:8080/v1/payments \\
  -H "Content-Type: application/json" \\
  -d '{
    "transactionId": "txn-005",
    "senderId": 1,
    "receiverId": 2,
    "amount": -5,
    "currency": "INR"
  }'
```

Response (`400`): `{"error":"VALIDATION\_ERROR","message":"amount: amount must be greater than zero",...}`

### 9\. Analytics — real-time volume monitoring (Analytics Worker)

Volume summary per currency over the last N minutes (default 60):

```bash
curl "http://localhost:8082/v1/analytics/volume?minutes=60"
```

Response:

```json
{
  "windowMinutes": 60,
  "currencies": \[
    {"currency": "INR", "payments": 4, "totalVolume": 675.50}
  ]
}
```

Per-minute timeseries (timestamps in UTC):

```bash
curl "http://localhost:8082/v1/analytics/volume/timeseries?minutes=60"
```

Response:

```json
{
  "windowMinutes": 60,
  "points": \[
    {"minute": "2026-09-12T21:36:00", "payments": 4, "volume": 675.50}
  ]
}
```

Note: only **completed** payments reach the analytics store (the worker consumes `payment-completed` events). `completed\_at` is the ingestion time in ClickHouse.

### PowerShell equivalents

```powershell
# Health
Invoke-RestMethod http://localhost:8080/actuator/health
Invoke-RestMethod http://localhost:8081/actuator/health

# Create a payment
$body = '{"transactionId":"txn-001","senderId":1,"receiverId":2,"amount":250.00,"currency":"INR"}'
Invoke-RestMethod -Method Post -Uri http://localhost:8080/v1/payments -ContentType 'application/json' -Body $body

# Transaction history
Invoke-RestMethod http://localhost:8081/v1/users/1/transactions
Invoke-RestMethod http://localhost:8081/v1/users/2/transactions

# Analytics
Invoke-RestMethod "http://localhost:8082/v1/analytics/volume?minutes=60"
Invoke-RestMethod "http://localhost:8082/v1/analytics/volume/timeseries?minutes=60"
```

\---

## Inspecting the system

### Application logs

```bash
docker compose logs -f transaction-gateway
docker compose logs -f ledger
```

### Database (payments, ledger entries, balances)

```bash
docker exec -it swiftpay-postgres psql -U swiftpay -d swiftpay

# inside psql:
#   SELECT \* FROM users;
#   SELECT \* FROM payments;
#   SELECT \* FROM ledger\_entries;
```

### Kafka topics and events

```bash
# List topics
docker exec swiftpay-kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server localhost:9092 --list

# Watch completed payments
docker exec swiftpay-kafka /opt/kafka/bin/kafka-console-consumer.sh \\
  --bootstrap-server localhost:9092 --topic payment-completed --from-beginning

# Watch failed payments
docker exec swiftpay-kafka /opt/kafka/bin/kafka-console-consumer.sh \\
  --bootstrap-server localhost:9092 --topic payment-failed --from-beginning
```

### Redis idempotency keys

```bash
docker exec -it swiftpay-redis redis-cli KEYS "swiftpay:idempotency:\*"
```

### ClickHouse (analytics events)

```bash
docker exec -it swiftpay-clickhouse clickhouse-client --user swiftpay --password swiftpay -d swiftpay\_analytics

# inside clickhouse-client:
#   SELECT \* FROM payment\_events FINAL ORDER BY completed\_at;
#   SELECT currency, count(), sum(amount) FROM payment\_events FINAL GROUP BY currency;
```

\---

## Stop / Reset

```bash
docker compose down        # stop everything, KEEP database data
docker compose down -v     # stop everything and DELETE database data (fresh start)
docker compose up -d       # start again (no rebuild needed if code unchanged)
docker compose up -d --build   # rebuild after code changes
```

\---

## Troubleshooting

|Problem|Fix|
|-|-|
|`Bind for 0.0.0.0:8080 failed: port is already allocated`|Something else uses that port. Stop it, or change the host-side port in `docker-compose.yml` (e.g. `"18080:8080"`).|
|Containers stuck in `health: starting`|Normal for the first \~1–2 minutes. Check progress with `docker compose logs -f <service>`.|
|Ledger exited / restarting|Run `docker logs swiftpay-ledger` — it depends on Kafka and Postgres being healthy; `docker compose up -d` again after they are.|
|Payment stays `PENDING` forever|The ledger consumer isn't processing. Check `docker compose logs ledger` and Kafka health (`docker compose ps`).|
|Want a completely clean slate|`docker compose down -v \&\& docker compose up -d --build`|

\---

## Running services locally (optional, for development)

If you want to run the Spring Boot apps from an IDE instead of Docker, start only the infrastructure:

```bash
docker compose up -d postgres redis kafka clickhouse
```

From the host: Postgres is on `localhost:5433`, Redis on `localhost:6380`, Kafka on `localhost:29092`, ClickHouse on `localhost:8123`. The default `application.properties` point at `localhost:5432/6379/9092`, so override them:

```bash
cd transaction-gateway
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:postgresql://localhost:5433/swiftpay --spring.data.redis.port=6380 --spring.kafka.bootstrap-servers=localhost:29092"

cd ledger
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:postgresql://localhost:5433/swiftpay --spring.kafka.bootstrap-servers=localhost:29092"

cd analytics-worker
./mvnw spring-boot:run -Dspring-boot.run.arguments="--spring.kafka.bootstrap-servers=localhost:29092"
```

(On Windows use `mvnw.cmd` instead of `./mvnw`.)

\---

## Reference

### Swagger / OpenAPI UI

Each service exposes interactive Swagger UI and the OpenAPI JSON:

|Service|Swagger UI|OpenAPI JSON|
|-|-|-|
|Transaction Gateway|http://localhost:8080/swagger-ui.html|http://localhost:8080/v3/api-docs|
|Ledger Service|http://localhost:8081/swagger-ui.html|http://localhost:8081/v3/api-docs|
|Analytics Worker|http://localhost:8082/swagger-ui.html|http://localhost:8082/v3/api-docs|

Open the Swagger UI URL in a browser to try the APIs without curl.

### API summary

|Method|Endpoint|Service|Description|
|-|-|-|-|
|POST|`/v1/payments`|Gateway|Initiate a payment|
|GET|`/v1/users/{id}/transactions`|Ledger|Ledger entries for a user|
|GET|`/v1/analytics/volume`|Analytics|Volume per currency (last N min)|
|GET|`/v1/analytics/volume/timeseries`|Analytics|Per-minute volume (last N min)|
|GET|`/actuator/health`|All|Health check|

### Payment request fields

|Field|Type|Rules|
|-|-|-|
|`transactionId`|string|Required, unique per payment (client-generated, used for idempotency)|
|`senderId`|number|Required, must exist|
|`receiverId`|number|Required, must exist, ≠ sender|
|`amount`|decimal|Required, ≥ 0.01|
|`currency`|string|Required, must match both users' currency (e.g. `INR`)|

### Kafka topics

|Topic|Producer|Consumer|Purpose|
|-|-|-|-|
|`payment-initiated`|Gateway|Ledger|New payment to settle|
|`payment-completed`|Ledger|Analytics Worker|Settlement succeeded → OLAP|
|`payment-failed`|Ledger|(future)|Settlement failed|
|`payment-initiated.DLT`|Ledger|(manual)|Dead letter after 3 failed tries|



