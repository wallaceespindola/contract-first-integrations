# Contract-First Integration
## Enabling Parallel Systems Development

**Wallace Espindola**
Senior Software Engineer

wallace.espindola@gmail.com
linkedin.com/in/wallaceespindola
github.com/wallaceespindola

---

## What is Contract-First?

**Contract-First** = Define the integration boundary **first**, then implement code that conforms to it.

A contract includes:
- Operations (endpoints, topics)
- Data shapes (request/response, events)
- Validation rules
- Error models
- Security requirements
- Non-functional rules (timeouts, retries, idempotency)
- Versioning strategy

**Key Principle:** The contract is the **single source of truth**

---

## The Problem: Traditional Integration

```
System A ----[waiting...]----> System B
      ↓
Team A: "When will B be ready?"
Team B: "We're still implementing..."
      ↓
Integration happens LATE
Surprises emerge
Assumptions drift
```

**Result:** Serial dependency, delayed integration, late surprises

---

## The Solution: Contract-First

```
1. Agree on contract (API/event schemas)
   ↓
2. Generate stubs/clients/mocks from contract
   ↓
3. System A implements provider (parallel)
4. System B implements consumer (parallel)
   ↓
5. CI enforces contract alignment
```

**Result:** Parallel development, early validation, no surprises

---

## Three Contract Types in Java

### 1️⃣ REST API Contract
- **Format:** OpenAPI 3.0 YAML/JSON
- **Generates:** Server stubs, DTOs, client SDKs, mock servers

### 2️⃣ Kafka/Event Contract
- **Format:** Avro/Protobuf schemas + AsyncAPI
- **Generates:** Java classes, consumer fixtures

### 3️⃣ Database Contract
- **Format:** Flyway SQL migrations
- **Generates:** Versioned, repeatable deployments

---

## Repository Layout

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
```

Contracts are **first-class artifacts**

---

## REST Contract Example

**OpenAPI 3.0 Specification**

```yaml
paths:
  /v1/orders:
    post:
      operationId: createOrder
      requestBody:
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateOrderRequest'
      responses:
        '201':
          description: Created
        '400':
          description: Validation error
        '409':
          description: Idempotency conflict
```

---

## REST Implementation (Java/Spring)

```java
@RestController
@RequestMapping("/v1/orders")
public class OrdersController {

  @PostMapping
  public ResponseEntity<OrderResponse> create(
      @RequestBody CreateOrderRequest request) {

    OrderResponse created = service.createOrder(request);
    return ResponseEntity.status(201).body(created);
  }

  @GetMapping("/{orderId}")
  public ResponseEntity<OrderResponse> get(
      @PathVariable String orderId) {

    return service.getOrder(orderId)
        .map(ResponseEntity::ok)
        .orElse(ResponseEntity.notFound().build());
  }
}
```

---

## Consumer Parallel Work

**System B doesn't wait for System A!**

✅ Generate Java client from OpenAPI spec immediately

✅ Run mock server for development

✅ Build integration tests against mock

✅ Switch to real API when ready

**No runtime dependency on the provider being finished**

---

## Kafka Contract: Topic Semantics

```markdown
# Topic: orders.order-created.v1

- Purpose: Emitted when order created successfully
- Key: orderId (ordering guaranteed per order)
- Delivery: at-least-once
- Consumer requirement: idempotent processing
- Retry: consumer retries transient errors
- DLQ: orders.order-created.v1.dlq
- Compatibility: backward compatible evolution
```

**Human-readable contract for semantics**

---

## Kafka Contract: Avro Schema

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
    { "name": "source", "type": ["null", "string"],
      "default": null },
    { "name": "items", "type": {"type": "array", ...} }
  ]
}
```

**Note:** `source` is nullable with default → **backward-compatible**

---

## Kafka Producer (Java)

```java
public class OrderEventPublisher {

  private final KafkaTemplate<String, OrderCreated> kafka;

  public void publishOrderCreated(OrderCreated event) {
    kafka.send(
      "orders.order-created.v1",
      event.getOrderId(),
      event
    );
  }
}
```

**Simple, type-safe publishing**

---

## Kafka Consumer with Idempotency

```java
@KafkaListener(topics = "orders.order-created.v1",
               groupId = "billing")
public void onOrderCreated(OrderCreated event) {

  // Check if already processed (at-least-once delivery)
  if (processedEventsRepo.existsByEventId(
      event.getEventId())) {
    return; // skip duplicate
  }

  // Process event
  billingService.createInvoice(
    event.getOrderId(),
    event.getCustomerId(),
    event.getItems()
  );

  // Store eventId to prevent reprocessing
  processedEventsRepo.save(event.getEventId());
}
```

---

## Database Contract: Flyway Migration

**V1__create_orders.sql**

```sql
CREATE TABLE orders (
  id           VARCHAR(32) PRIMARY KEY,
  customer_id  VARCHAR(32) NOT NULL,
  status       VARCHAR(16) NOT NULL,
  created_at   TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE order_items (
  order_id  VARCHAR(32) REFERENCES orders(id),
  sku       VARCHAR(64) NOT NULL,
  quantity  INT NOT NULL CHECK (quantity > 0),
  PRIMARY KEY (order_id, sku)
);

CREATE INDEX idx_orders_customer_id
  ON orders(customer_id);
```

---

## Schema Evolution: Expand/Migrate/Contract

**V2__add_order_source.sql** (Expand)

```sql
ALTER TABLE orders ADD COLUMN source VARCHAR(32);
```

**Backfill** (Migrate)

```sql
UPDATE orders SET source = 'UNKNOWN'
WHERE source IS NULL;
```

**Enforce** (Contract)

```sql
ALTER TABLE orders
ALTER COLUMN source SET NOT NULL;
```

**Zero-downtime schema evolution**

---

## End-to-End Flow

```
1. REST API Request
   ↓ (OpenAPI validation)
2. Persist to Database
   ↓ (Flyway schema)
3. Publish Kafka Event
   ↓ (Avro schema + Schema Registry)
4. Consumers Process Event
   (with idempotency)
```

**Three contracts working together**

---

## Service Implementation

```java
@Transactional
public OrderResponse createOrder(CreateOrderRequest req) {
  String orderId = idGenerator.newOrderId();

  // 1. Persist (DB contract)
  ordersRepository.save(
    new OrderEntity(orderId, req.getCustomerId(), "CREATED")
  );
  orderItemsRepository.saveAll(
    OrderItemEntity.from(orderId, req.getItems())
  );

  // 2. Publish event (Kafka contract)
  OrderCreated event = OrderCreated.newBuilder()
      .setEventId(UUID.randomUUID().toString())
      .setOccurredAt(Instant.now().toString())
      .setOrderId(orderId)
      .setCustomerId(req.getCustomerId())
      .setItems(toAvroItems(req.getItems()))
      .build();

  publisher.publishOrderCreated(event);

  // 3. Return response (REST contract)
  return new OrderResponse(orderId, ...);
}
```

---

## Parallel Development Scenarios

### 🔹 REST Integration
- **Provider** builds server behind OpenAPI
- **Consumer** builds client using generated SDK or mock
- **No blocking** - both work independently

### 🔹 Event-Driven Integration
- **Publisher** and **consumer** build against same Avro schema
- **No runtime dependency** on each other
- Schema validation ensures compatibility

### 🔹 Legacy ↔ New System
- **Legacy** exposes integration contract
- **New system** builds against contract before cutover
- Contract serves as the "bridge"

---

## CI/CD Gates: Making Contracts Real

### 🛡️ REST: OpenAPI Breaking Change Detection
- Run OpenAPI diff tool in CI
- Fail on breaking changes without version bump

### 🛡️ Kafka: Schema Compatibility Check
- Enforce backward compatibility in Schema Registry
- CI fails if schema update is incompatible

### 🛡️ DB: Migration Validation
- Flyway validates migrations
- Deployment fails early if mismatched

### 🛡️ Consumer-Driven Contract Tests
- Tools like Pact validate consumer expectations

---

## Practical Rules in Contracts

### Versioning
- REST: `/v1`, `/v2` or semantic versioning
- Kafka: compatibility policy + versioned topics

### Errors & Retries
- REST: standardized `ErrorResponse`, retriable codes
- Kafka: retry policy + DLQ envelope

### Idempotency
- REST: `Idempotency-Key` header or business key
- Kafka: `eventId` + consumer dedupe

### Non-Functional Requirements
- Timeouts, rate limits, SLAs
- Ordering guarantees (Kafka keying)

---

## Mental Model Shift

### Without Contract-First
```
❌ Serial dependency
❌ Teams block each other
❌ Assumptions drift
❌ Late integration surprises
❌ "When will the other team finish?"
```

### With Contract-First
```
✅ Parallel development
✅ Early validation
✅ No surprises
✅ CI enforces alignment
✅ "Contract agreed - let's build!"
```

---

## From Coordination to Governance

**Traditional Integration:** *Coordination Problem*
- Team A waits for Team B
- Manual coordination meetings
- Integration happens late
- High coupling between teams

**Contract-First Integration:** *Governance Problem*
- Teams agree on contract upfront
- Generate tools automatically
- CI enforces compliance
- Low coupling, high autonomy

**Key Insight:** Transform integration from serial to parallel

---

## Benefits Summary

✅ **Enables Parallel Development**
Teams no longer block each other

✅ **Reduces Integration Risk**
Contracts catch breaking changes early

✅ **Improves Evolution**
Versioning and compatibility are explicit

✅ **Essential for Microservices**
Clear boundaries enable independent scaling

✅ **Turns Teams into Ecosystem Contributors**
Not dependencies on each other

---

## Real-World Impact

**Before Contract-First:**
- 🕐 Integration: 2-3 weeks waiting + 1 week debugging
- 🐛 Breaking changes discovered in production
- 👥 Teams constantly coordinating schedules

**After Contract-First:**
- 🕐 Integration: 1 day (contract agreement) + parallel work
- 🐛 Breaking changes caught in CI before merge
- 👥 Teams work independently, CI enforces compliance

**Result:** 10x faster integration, zero production surprises

---

## Getting Started

1. **Choose a contract type** (REST, Kafka, or DB)

2. **Define the contract first** (before any code)

3. **Generate tools** (stubs, clients, classes)

4. **Build independently** (provider and consumer in parallel)

5. **Add CI gates** (breaking change detection)

6. **Evolve safely** (versioning + compatibility checks)

---

## Tools & Technologies

### REST Contracts
- OpenAPI 3.0 specification
- openapi-generator (Java SDK generation)
- Swagger UI / ReDoc (documentation)
- Prism / Stoplight (mock servers)

### Kafka Contracts
- Apache Avro / Protobuf
- Confluent Schema Registry
- AsyncAPI (documentation)
- avro-maven-plugin (code generation)

### Database Contracts
- Flyway / Liquibase
- Version control for schemas
- Migration validation

---

## Best Practices

### ✅ DO:
- Version all contracts explicitly
- Use semantic versioning
- Generate code from contracts
- Enforce compatibility in CI
- Document breaking changes
- Test contract compliance

### ❌ DON'T:
- Hand-write DTOs (generate them!)
- Skip schema validation
- Make breaking changes without version bump
- Deploy without migration validation
- Assume consumers can adapt

---

## Common Pitfalls

### ❌ "We'll add the contract later"
**Wrong:** Contract **must** come first

### ❌ "Just make a quick change"
**Wrong:** All changes go through contract update + CI validation

### ❌ "Our team doesn't need contracts"
**Wrong:** Even single-team projects benefit from contract discipline

### ❌ "Contracts slow us down"
**Wrong:** Contracts enable speed through parallel work and safety

---

## Advanced Patterns

### Idempotency Strategies
- REST: Idempotency-Key header + request hash
- Kafka: eventId + processed events table

### Schema Evolution
- Add fields with defaults (backward compatible)
- Expand → Migrate → Contract pattern
- Version negotiation

### Dead Letter Queues
- Envelope schema for DLQ messages
- Error classification (retriable vs non-retriable)
- Replay mechanisms

---

## Case Study: Order System

**Contracts:**
- REST: `orders-api.v1.yaml` (create, get orders)
- Kafka: `OrderCreated.v1.avsc` (event when created)
- DB: `V1__create_orders.sql` (schema)

**Teams:**
- **Orders Team:** Implements REST API + event publisher
- **Billing Team:** Implements Kafka consumer
- **Fulfillment Team:** Implements Kafka consumer

**Result:** All teams work in parallel from day 1

---

## Integration with CI/CD

```yaml
# GitHub Actions example
name: Contract Validation

on: [pull_request]

jobs:
  openapi-check:
    - name: Check for breaking changes
      run: |
        npx openapi-diff \
          main/contracts/orders-api.v1.yaml \
          ${{ github.ref }}/contracts/orders-api.v1.yaml

  schema-compatibility:
    - name: Validate Avro compatibility
      run: |
        mvn schema-registry:test-compatibility
```

---

## Monitoring & Observability

**Track Contract Compliance:**
- Schema Registry compatibility violations
- OpenAPI spec violations in production
- Failed Flyway migrations

**Metrics to Monitor:**
- Contract update frequency
- Breaking change attempts blocked
- Consumer lag (Kafka)
- API error rates by endpoint

---

## Migration Strategy

### Phase 1: Adopt for New Features
Start with new APIs/events only

### Phase 2: Retrofit Critical Paths
Add contracts to high-traffic integrations

### Phase 3: Mandate for All
Make contract-first mandatory for all integrations

### Phase 4: Automate Governance
CI/CD enforces contract compliance automatically

**Tip:** Start small, prove value, then expand

---

## Team Organization

```
┌─────────────────┐
│  Architecture   │ ← Defines contract standards
│      Team       │    and reviews all contracts
└────────┬────────┘
         │
    ┌────┴────────────────┬──────────────┐
    │                     │              │
┌───┴────┐          ┌────┴────┐    ┌───┴────┐
│Provider│          │Consumer │    │Consumer│
│  Team  │          │  Team 1 │    │  Team 2│
└────────┘          └─────────┘    └────────┘
    │                     │              │
    └──────────┬──────────┴──────────────┘
               │
          ┌────┴─────┐
          │    CI    │ ← Enforces contracts
          └──────────┘
```

---

## Success Metrics

### Development Speed
- Time to integrate: **-80%** (weeks → days)
- Parallel work enabled: **+300%**

### Quality
- Production integration bugs: **-95%**
- Breaking changes caught in CI: **100%**

### Team Autonomy
- Cross-team coordination meetings: **-70%**
- Independent deployment capability: **100%**

---

## Resources

### Documentation
- OpenAPI Specification: openapis.org
- AsyncAPI Specification: asyncapi.com
- Apache Avro: avro.apache.org
- Confluent Schema Registry docs

### Tools
- openapi-generator.tech
- Postman / Insomnia (API testing)
- Kafka Connect / Schema Registry
- Pact (consumer-driven contracts)

### Further Reading
- "Building Microservices" - Sam Newman
- "Release It!" - Michael Nygard

---

## Key Takeaways

1️⃣ **Contract-First = Parallel Development**
   Teams work independently, not sequentially

2️⃣ **Three Contract Types**
   REST (OpenAPI), Kafka (Avro), DB (Flyway)

3️⃣ **CI Enforces Alignment**
   Automated validation prevents drift

4️⃣ **Evolution is Built-In**
   Versioning and compatibility are first-class

5️⃣ **From Coordination to Governance**
   Automation replaces manual coordination

---

## Call to Action

### 🚀 Start Today:

1. Pick one integration to convert
2. Write the contract FIRST
3. Generate tools from contract
4. Add CI validation
5. Measure the impact

### 📚 Learn More:

- Reference implementation: github.com/wallaceespindola/contract-first-integrations
- Articles and guides in `/docs`
- Contact: wallace.espindola@gmail.com

---

## Questions?

**Wallace Espindola**
Sr. Software Engineer / Solution Architect

📧 wallace.espindola@gmail.com
🔗 linkedin.com/in/wallaceespindola
💻 github.com/wallaceespindola

**This Presentation:**
github.com/wallaceespindola/contract-first-integrations

Thank you! 🙏

