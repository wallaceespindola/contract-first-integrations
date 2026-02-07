# Contract-First for Systems Integration (Java + REST APIs + Kafka + Databases)

This file consolidates the full context we discussed about **Contract-First** (API-First / Schema-First) development for **systems integration**, with a focus on **Java**, **REST APIs**, **Kafka**, and **database integrations**, including **YAML/SQL scripts and Java code examples**.

---

## 1) Definition: what “Contract-First” means

**Contract-First** is an approach where you **define the integration boundary first**—the *contract*—and then implement code that **conforms** to that contract.

A contract typically includes:

- **Operations** (endpoints, topics)
- **Data shapes** (request/response DTOs, event schemas)
- **Validation rules** (required fields, constraints)
- **Error model** (REST errors, DLQ envelope)
- **Security/auth** (where applicable)
- **Non-functional rules** (timeouts, retries, idempotency, compatibility, SLAs)
- **Versioning and evolution rules**

**Key principle:** the contract is the **single source of truth**. Code, docs, SDKs, mocks, and tests are derived from it.

---

## 2) Why Contract-First enables parallel work between systems

When two systems must integrate (A ↔ B), contract-first lets teams work **in parallel**:

1. **Agree on the contract** early (API/event schemas + semantics).
2. **Generate stubs/clients/mocks** from the contract.
3. **System A** implements the provider side (API, event publisher).
4. **System B** implements the consumer side (client calls, event consumer).
5. **Contract tests + CI gates** prevent drift and breaking changes.

Instead of “wait for the other system to finish”, both teams implement against the contract immediately.

---

## 3) Contract types in Java-based integration

### A) REST API Contract (OpenAPI)
- Contract format: **OpenAPI 3.x** (`.yaml` / `.json`)
- Generated artifacts:
  - Server stubs/interfaces (Spring)
  - DTO models
  - Client SDKs
  - Mock servers + docs

### B) Kafka/Event Contract (Schema + topic semantics)
- Contract formats:
  - **Avro / Protobuf / JSON Schema**
  - (Optional) **AsyncAPI** for documentation of topics + payloads
- Enforcement:
  - **Schema Registry compatibility checks**
- Generated artifacts:
  - Java classes for events
  - Consumer fixtures for tests

### C) Database Contract (Migrations)
- Contract formats:
  - **Flyway** SQL migrations or **Liquibase** changelogs
- Enforcement:
  - migration validation + repeatable deployments

---

## 4) Recommended repository layout

A practical layout that keeps contracts explicit:

```
repo/
  contracts/
    openapi/
      orders-api.v1.yaml
    events/
      avro/
        OrderCreated.v1.avsc
        DeadLetterEnvelope.v1.avsc
      topics.md
      asyncapi.yaml
    db/
      flyway/
        V1__create_orders.sql
        V2__add_order_source.sql
  service/
    src/main/java/...
    src/test/java/...
```

This layout helps reviewers and CI treat contracts as first-class artifacts.

---

## 5) REST Contract-First (OpenAPI)

### 5.1 Example OpenAPI YAML contract

**File:** `contracts/openapi/orders-api.v1.yaml`

```yaml
openapi: 3.0.3
info:
  title: Orders API
  version: 1.0.0
servers:
  - url: https://api.example.com

paths:
  /v1/orders:
    post:
      operationId: createOrder
      summary: Create a new order
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateOrderRequest'
      responses:
        '201':
          description: Created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/OrderResponse'
        '400':
          description: Validation error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '409':
          description: Conflict (idempotency key reused with different payload)
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

  /v1/orders/{orderId}:
    get:
      operationId: getOrder
      summary: Get an existing order
      parameters:
        - name: orderId
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: Found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/OrderResponse'
        '404':
          description: Not found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

components:
  schemas:
    CreateOrderRequest:
      type: object
      required: [customerId, items]
      properties:
        customerId:
          type: string
          example: CUST-123
        idempotencyKey:
          type: string
          description: Optional. Used to safely retry the same request.
          example: 2f7dbe6b-4b0b-4d62-a497-b1b99ce1b2da
        items:
          type: array
          minItems: 1
          items:
            $ref: '#/components/schemas/OrderItem'

    OrderItem:
      type: object
      required: [sku, quantity]
      properties:
        sku:
          type: string
          example: SKU-001
        quantity:
          type: integer
          minimum: 1
          example: 2

    OrderResponse:
      type: object
      required: [orderId, customerId, status, items]
      properties:
        orderId:
          type: string
          example: ORD-10001
        customerId:
          type: string
          example: CUST-123
        status:
          type: string
          enum: [CREATED, REJECTED]
        items:
          type: array
          items:
            $ref: '#/components/schemas/OrderItem'

    ErrorResponse:
      type: object
      required: [code, message, traceId]
      properties:
        code:
          type: string
          example: VALIDATION_ERROR
        message:
          type: string
          example: items must not be empty
        traceId:
          type: string
          example: 6b22c6b3c2c14c9aa9bafdf5c3f1c4f1
```

### 5.2 Java: implementing the provider side (Spring)

You can generate server interfaces from OpenAPI (e.g., with openapi-generator) and implement them.
Below is an illustrative implementation style.

```java
@RestController
@RequestMapping("/v1/orders")
public class OrdersController {

  private final OrderService service;

  public OrdersController(OrderService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<OrderResponse> create(@RequestBody CreateOrderRequest request) {
    OrderResponse created = service.createOrder(request);
    return ResponseEntity.status(201).body(created);
  }

  @GetMapping("/{orderId}")
  public ResponseEntity<OrderResponse> get(@PathVariable String orderId) {
    return service.getOrder(orderId)
        .map(ResponseEntity::ok)
        .orElseGet(() -> ResponseEntity.status(404).build());
  }
}
```

### 5.3 Consumer parallel work: generate a client + mock server

- **System B** can generate a Java client from the OpenAPI spec immediately.
- **System B** can also run a mock server (many tools can serve OpenAPI mocks) and develop integration without System A being done.

---

## 6) Kafka Contract-First (schemas + topic semantics)

### 6.1 Topic semantics contract (human-readable)

**File:** `contracts/events/topics.md`

```md
# Topics and Semantics

## orders.order-created.v1
- Purpose: emitted when an order is created successfully
- Key: orderId (ordering guaranteed per order)
- Delivery: at-least-once
- Consumer requirement: idempotent processing (dedupe by eventId)
- Retry: consumer retries transient errors
- DLQ: orders.order-created.v1.dlq for poison messages
- Compatibility: backward compatible schema evolution
```

### 6.2 Avro schema contract (with evolution-friendly defaults)

**File:** `contracts/events/avro/OrderCreated.v1.avsc`

```json
{
  "type": "record",
  "name": "OrderCreated",
  "namespace": "com.acme.events",
  "fields": [
    { "name": "eventId", "type": "string" },
    { "name": "occurredAt", "type": "string" },
    { "name": "orderId", "type": "string" },
    { "name": "customerId", "type": "string" },
    { "name": "source", "type": ["null", "string"], "default": null },

    { "name": "items",
      "type": { "type": "array",
        "items": {
          "type": "record",
          "name": "OrderItem",
          "fields": [
            { "name": "sku", "type": "string" },
            { "name": "quantity", "type": "int" }
          ]
        }
      }
    }
  ]
}
```

Notice `source` is nullable with a default. This supports **backward-compatible evolution**.

### 6.3 Dead-letter envelope schema (DLQ contract)

**File:** `contracts/events/avro/DeadLetterEnvelope.v1.avsc`

```json
{
  "type": "record",
  "name": "DeadLetterEnvelope",
  "namespace": "com.acme.events",
  "fields": [
    { "name": "originalTopic", "type": "string" },
    { "name": "partition", "type": "int" },
    { "name": "offset", "type": "long" },
    { "name": "consumerGroup", "type": "string" },
    { "name": "errorClass", "type": "string" },
    { "name": "errorMessage", "type": "string" },
    { "name": "failedAt", "type": "string" },
    { "name": "payloadBase64", "type": "string" }
  ]
}
```

### 6.4 Optional: AsyncAPI (docs for event APIs)

**File:** `contracts/events/asyncapi.yaml`

```yaml
asyncapi: '2.6.0'
info:
  title: Orders Events
  version: '1.0.0'
channels:
  orders.order-created.v1:
    publish:
      message:
        name: OrderCreated
        contentType: application/avro
        payload:
          $ref: '#/components/schemas/OrderCreated'
components:
  schemas:
    OrderCreated:
      type: object
      required: [eventId, occurredAt, orderId, customerId, items]
      properties:
        eventId: { type: string }
        occurredAt: { type: string }
        orderId: { type: string }
        customerId: { type: string }
        source: { type: string, nullable: true }
        items:
          type: array
          items:
            type: object
            required: [sku, quantity]
            properties:
              sku: { type: string }
              quantity: { type: integer }
```

### 6.5 Java Kafka producer (publisher system)

```java
public class OrderEventPublisher {

  private final KafkaTemplate<String, OrderCreated> kafka;

  public OrderEventPublisher(KafkaTemplate<String, OrderCreated> kafka) {
    this.kafka = kafka;
  }

  public void publishOrderCreated(OrderCreated event) {
    kafka.send("orders.order-created.v1", event.getOrderId(), event);
  }
}
```

### 6.6 Java Kafka consumer with idempotency (consumer system)

At-least-once delivery means duplicates are possible; consumers must be idempotent.

```java
@KafkaListener(topics = "orders.order-created.v1", groupId = "billing")
public void onOrderCreated(OrderCreated event) {
  if (processedEventsRepository.existsByEventId(event.getEventId())) {
    return; // already processed
  }

  billingService.createInvoice(event.getOrderId(), event.getCustomerId(), event.getItems());

  processedEventsRepository.save(event.getEventId());
}
```

---

## 7) Database Contract-First (Flyway)

### 7.1 Base schema migration

**File:** `contracts/db/flyway/V1__create_orders.sql`

```sql
CREATE TABLE orders (
  id           VARCHAR(32) PRIMARY KEY,
  customer_id  VARCHAR(32) NOT NULL,
  status       VARCHAR(16) NOT NULL,
  created_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE order_items (
  order_id  VARCHAR(32) NOT NULL REFERENCES orders(id),
  sku       VARCHAR(64) NOT NULL,
  quantity  INT NOT NULL CHECK (quantity > 0),
  PRIMARY KEY (order_id, sku)
);

CREATE INDEX idx_orders_customer_id ON orders(customer_id);
```

### 7.2 Expand/migrate/contract example (schema evolution)

**Goal:** add `source` to orders without breaking old code.

**Expand**
**File:** `contracts/db/flyway/V2__add_order_source.sql`

```sql
ALTER TABLE orders ADD COLUMN source VARCHAR(32);
```

**Migrate**
- Backfill `source` for existing rows (optional, depends on semantics).

```sql
UPDATE orders SET source = 'UNKNOWN' WHERE source IS NULL;
```

**Contract**
- Later, once all systems produce `source`, you can enforce `NOT NULL`.

```sql
ALTER TABLE orders ALTER COLUMN source SET NOT NULL;
```

---

## 8) End-to-end example: API → DB → Kafka (with outbox note)

### 8.1 Service flow (concept)

1. Validate REST request (OpenAPI contract)
2. Persist to DB (Flyway contract)
3. Publish Kafka event (Avro + topic semantics contract)

**Important integration concern:** If you write to DB and publish to Kafka in the same request,
you can hit “DB updated but event not published” failures.
A common solution is the **Outbox pattern** (persist event in DB first, then publish reliably).

### 8.2 Java service example (simplified)

```java
@Transactional
public OrderResponse createOrder(CreateOrderRequest req) {
  String orderId = idGenerator.newOrderId();

  ordersRepository.save(new OrderEntity(orderId, req.getCustomerId(), "CREATED"));
  orderItemsRepository.saveAll(OrderItemEntity.from(orderId, req.getItems()));

  OrderCreated event = OrderCreated.newBuilder()
      .setEventId(UUID.randomUUID().toString())
      .setOccurredAt(Instant.now().toString())
      .setOrderId(orderId)
      .setCustomerId(req.getCustomerId())
      .setItems(toAvroItems(req.getItems()))
      .build();

  publisher.publishOrderCreated(event);

  return new OrderResponse(orderId, req.getCustomerId(), "CREATED", req.getItems());
}
```

---

## 9) Parallel development scenarios

Contract-first enables multiple integration patterns to run simultaneously:

### 9.1 REST Integration
- **Provider** builds the server implementation behind the OpenAPI contract.
- **Consumer** builds the client integration using a generated Java client or mock server.
- Both teams work **independently**—no need for the provider to finish first.

### 9.2 Kafka/Event-Driven Integration
- **Publisher** and **consumer** build against the same Avro schema simultaneously.
- **Publisher** implements the event emitter.
- **Consumer** implements listeners independently.
- **No runtime dependency** on the other side—use schema validation and testing instead.

### 9.3 Legacy ↔ New System Integration
- **Legacy system** exposes its integration contract (API endpoints or event topics).
- **New system** builds against that contract **before cutover**.
- Contracts serve as the "bridge" and source of truth during migration.

---

## 10) CI/CD gates that make Contract-First real

Contracts should be **enforced**, not just documented.

### 10.1 REST: OpenAPI breaking change detection
- Run an OpenAPI diff tool in CI and fail on breaking changes unless version bump is applied.

### 10.2 Kafka: Schema compatibility check
- Enforce backward compatibility in Schema Registry.
- CI can fail a build if a schema update is incompatible.

### 10.3 DB: migration validation
- Flyway/Liquibase validates migrations; deployment fails early if mismatched.

### 10.4 Consumer-driven contract tests
- Tools like Pact can validate consumer expectations against provider implementations.

---

## 11) Practical rules that should be in the contract

### Versioning
- REST: `/v1`, `/v2` or semantic versioning for specs
- Kafka: schema compatibility policy + (optionally) versioned topics

### Errors & retries
- REST: standardized `ErrorResponse` and retriable vs non-retriable codes
- Kafka: retry policy + DLQ envelope

### Idempotency
- REST: `Idempotency-Key` or request business key
- Kafka: `eventId` + consumer dedupe strategy

### Non-functional requirements
- timeouts, rate limits, SLAs
- ordering guarantees (Kafka keying)

---

## 12) Mental Model

> **Agree on the contract → generate tools → build independently → let CI enforce alignment.**

Without contract-first, systems integration is a **coordination problem**:
- Team A waits for Team B to finish
- Assumptions drift over time
- Integration surprises emerge late

With contract-first, systems integration becomes a **governance and automation problem**:
- Teams agree on the contract upfront
- Each team generates tools and builds independently
- CI automatically enforces alignment
- Parallel development becomes the default

**Key insight:** Contract-first transforms integration from a serial, dependency-heavy process into a parallel, governance-based practice that scales.

---

## 13) Summary

Contract-first integration:

- **Enables parallel development** — teams no longer block each other waiting for implementations
- **Reduces integration risk** — contracts catch breaking changes early via CI gates
- **Improves long-term system evolution** — versioning and compatibility rules are explicit
- **Is essential for microservices and distributed systems** — clear boundaries enable independent scaling and deployment
- **Turns teams into contributors to the same ecosystem** — not dependencies on each other

**When done properly:** teams integrate by design, not by accident. The contract becomes the shared source of truth that permits true organizational parallelism.

