# Contract-First Integration: Enable Parallel Development with Spring Boot, Kafka, and Flyway

## How defining API, event, and database contracts upfront enables teams to build independently and integrate in parallel

![Contract-First Integration Advanced Patterns](../images/javapro-featured-contract-first.png)

The fundamental benefit of contract-first integration isn't the specifications you write. It's enabling multiple teams to work simultaneously on the same integration boundary instead of waiting for each other.

This article demonstrates how to implement contract-first integration using OpenAPI 3.0 for REST APIs, Avro schemas with Confluent Schema Registry for Kafka events, and Flyway migrations for database contracts—all with Spring Boot. These contracts enable parallel development where provider and consumer teams work independently from day one.

## The Integration Challenge

Traditional distributed systems development creates sequential bottlenecks:

**Week 1-2**: Provider team implements API
**Week 3**: Consumer team waits, then starts integration
**Week 4-6**: Teams discover mismatches in assumptions
  - API returns different field names than expected
  - Event payloads don't include required fields
  - Database columns exist but have different constraints
**Week 7**: Debugging and rework

**Total: 7 weeks. Most of it blocking.**

Contract-first reverses this:

**Day 1**: Both teams design contracts together (OpenAPI, Avro, Flyway)
**Week 1-2**: Both teams implement simultaneously
  - Provider builds real API/services
  - Consumer builds against generated mocks and client SDKs
  - No blocking, no waiting
**Week 3**: Integration testing (usually just works)

**Total: 3 weeks. Most of it parallel.**

## The Three Contract Boundaries

Distributed systems have three integration boundaries. Each requires explicit contracts:

### 1. REST API Contracts (OpenAPI 3.0)

Define endpoint behavior, request/response schemas, error codes, and validation rules upfront. From this single contract, generate:
- Server stubs (forces provider to implement to spec)
- Client SDKs (consumer doesn't wait for provider)
- Mock servers (consumer develops independently)
- API documentation (always in sync)

### 2. Event Contracts (Avro + Schema Registry)

Define event structure and ensure backward compatibility through Schema Registry. Enables:
- Multiple consumers reading the same events independently
- Schema evolution without breaking consumers
- Automatic validation at serialization time
- Idempotency guarantees (built into contract)

### 3. Database Contracts (Flyway Migrations)

Version database schema changes explicitly. Enables:
- Zero-downtime schema evolution (expand/migrate/contract pattern)
- Clear audit trail of schema changes
- Backward compatibility validation
- Coordinated migrations across services

## Implementing Contract-First with Spring Boot

### Step 1: Define the REST API Contract

Create `contracts/openapi/orders-api.v1.yaml`:

```yaml
openapi: 3.2.0
info:
  title: Orders API
  version: 1.0.0
  description: Contract-first REST API enabling parallel consumer development

paths:
  /v1/orders:
    post:
      operationId: createOrder
      summary: Create order with idempotency support
      requestBody:
        required: true
        content:
          application/json:
            schema:
              $ref: '#/components/schemas/CreateOrderRequest'
      responses:
        '201':
          description: Order created
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/OrderResponse'
        '400':
          description: Validation error
        '409':
          description: Idempotency conflict

components:
  schemas:
    CreateOrderRequest:
      type: object
      required: [customerId, items]
      properties:
        customerId:
          type: string
        idempotencyKey:
          type: string
          description: Required for safe retries
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
        quantity:
          type: integer
          minimum: 1

    OrderResponse:
      type: object
      required: [orderId, customerId, status, items, timestamp]
      properties:
        orderId:
          type: string
        customerId:
          type: string
        status:
          enum: [CREATED, REJECTED]
        items:
          type: array
          items:
            $ref: '#/components/schemas/OrderItem'
        timestamp:
          type: string
          format: date-time
```

**Key contract decisions:**
- `idempotencyKey` enables safe retries (critical for distributed systems)
- Request/response schemas explicitly defined (no guessing)
- HTTP status codes documented (400, 409, etc.)
- Consumers generate clients from this contract immediately

### Step 2: Implement Spring Boot Provider

```java
@RestController
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    /**
     * Implementation conforms to contract: contracts/openapi/orders-api.v1.yaml
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        log.info("Creating order: customer={}", request.customerId());

        OrderResponse response = orderService.createOrder(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        return orderService.getOrder(orderId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }
}
```

Consumer team doesn't wait for this implementation. They generate a mock server from the contract and start developing immediately.

### Step 3: Define Event Contracts with Avro

Create `contracts/events/avro/OrderCreated.v1.avsc`:

```json
{
  "type": "record",
  "name": "OrderCreated",
  "namespace": "com.acme.events",
  "doc": "Event published when order is created. Consumers must be idempotent by eventId.",
  "fields": [
    {
      "name": "eventId",
      "type": "string",
      "doc": "Unique event ID for consumer deduplication"
    },
    {
      "name": "occurredAt",
      "type": "string",
      "doc": "ISO 8601 timestamp"
    },
    {
      "name": "orderId",
      "type": "string"
    },
    {
      "name": "customerId",
      "type": "string"
    },
    {
      "name": "items",
      "type": {
        "type": "array",
        "items": {
          "type": "record",
          "name": "OrderItem",
          "fields": [
            {"name": "sku", "type": "string"},
            {"name": "quantity", "type": "int"}
          ]
        }
      }
    },
    {
      "name": "source",
      "type": ["null", "string"],
      "default": null,
      "doc": "Order source (WEB, MOBILE, API). Nullable for backward compatibility."
    }
  ]
}
```

**Contract benefits:**
- Schema Registry validates every event at publication time
- `source` field is nullable with default—backward compatible
- `eventId` documents that consumers must deduplicate
- Multiple independent consumers can consume this event safely

### Step 4: Kafka Producer with Schema Registry Validation

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(OrderCreated event) {
        String key = event.getOrderId();

        // Schema Registry validates schema compatibility at send time
        kafkaTemplate.send("orders.order-created.v1", key, event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Published event: orderId={}, eventId={}",
                                key, event.getEventId());
                    } else {
                        log.error("Failed to publish: orderId={}", key, ex);
                    }
                });
    }
}
```

Schema Registry rejects incompatible schema changes before they reach production.

### Step 5: Kafka Consumer with Idempotency

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedListener {

    private final ProcessedEventRepository processedEventRepository;
    private final BillingService billingService;

    @KafkaListener(topics = "orders.order-created.v1", groupId = "billing-service")
    public void onOrderCreated(OrderCreated event) {
        String eventId = event.getEventId();

        // Check idempotency (at-least-once delivery requires this)
        if (processedEventRepository.existsByEventId(eventId)) {
            log.debug("Skipping duplicate: eventId={}", eventId);
            return;
        }

        // Process event
        billingService.createInvoice(
                event.getOrderId(),
                event.getCustomerId(),
                event.getItems()
        );

        // Mark processed
        processedEventRepository.save(new ProcessedEvent(eventId, Instant.now()));
    }
}
```

Multiple independent consumer services can process this event with no coordination overhead.

### Step 6: Define Database Contract with Flyway

Create `src/main/resources/db/migration/V1__create_orders_schema.sql`:

```sql
CREATE TABLE orders (
    id           VARCHAR(32) PRIMARY KEY,
    customer_id  VARCHAR(32) NOT NULL,
    status       VARCHAR(16) NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
    order_id  VARCHAR(32) NOT NULL REFERENCES orders(id),
    sku       VARCHAR(64) NOT NULL,
    quantity  INT NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (order_id, sku)
);

CREATE TABLE processed_events (
    event_id      VARCHAR(255) PRIMARY KEY,
    processed_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_orders_customer_id ON orders(customer_id);
```

Create `src/main/resources/db/migration/V2__add_order_source.sql` (zero-downtime evolution):

```sql
-- Phase 1: EXPAND (old code continues working)
ALTER TABLE orders ADD COLUMN source VARCHAR(32);

-- Phase 2: MIGRATE (backfill and new writes)
UPDATE orders SET source = 'UNKNOWN' WHERE source IS NULL;

-- Phase 3: CONTRACT (enforce after rollout)
-- ALTER TABLE orders ALTER COLUMN source SET NOT NULL;
```

Flyway validates migrations on startup. Manual schema changes are detected and rejected.

## Parallel Development in Action

### Day 1: Contract Design
**Both Teams Meet:**
1. Design OpenAPI contract for REST API
2. Design Avro schema for Kafka events
3. Design Flyway migrations for database
4. Agree on all three contracts

**Time: 2-3 hours of focused design meeting**

### Week 1: Parallel Implementation

**Provider Team (builds real API):**
```bash
# Generate server stubs from OpenAPI
openapi-generator generate -i contracts/openapi/orders-api.v1.yaml -g spring

# Implement controllers
mvn spring-boot:run
```

**Consumer Team (develops against mocks):**
```bash
# Generate mock server from OpenAPI
prism mock contracts/openapi/orders-api.v1.yaml --port 8080

# Generate client SDK
openapi-generator generate -i contracts/openapi/orders-api.v1.yaml -g java

# Develop against mock (no waiting for provider)
mvn test
```

**Both teams work simultaneously.** Zero blocking.

### Week 2: Integration Testing
```bash
# Consumer switches config: orders.api.url=https://api.production.com
# Everything works on first try (both implemented the same contract)
mvn integration-test
```

## CI/CD Enforcement

Make contracts machine-validated:

```yaml
# .github/workflows/contract-validation.yml
- name: Check API breaking changes
  run: |
    npx openapi-diff \
      main:contracts/openapi/orders-api.v1.yaml \
      HEAD:contracts/openapi/orders-api.v1.yaml \
      --fail-on-breaking

- name: Test schema compatibility
  run: mvn schema-registry:test-compatibility

- name: Validate Flyway migrations
  run: mvn flyway:validate
```

Breaking changes fail the build before merge.

## Real-World Timeline Impact

**Without Contract-First:**
- Week 1-2: Provider builds API
- Week 3: Consumer waits
- Week 4-6: Integration debugging
- Week 7: Finally working
- **Total: 7 weeks, sequential**

**With Contract-First:**
- Day 1: Design contracts
- Week 1-2: Both teams build simultaneously
- Week 3: Integration (just works)
- **Total: 3 weeks, 70% parallel**

**Time saved: 50%**

## The Technologies in Action

**OpenAPI 3.0**: REST API contract → generates stubs, mocks, docs, clients
**Avro + Schema Registry**: Event contract → validates at publish time, enables schema evolution
**Flyway**: Database contract → zero-downtime migrations, audit trail
**Spring Boot 3**: Unified implementation framework for all three
**Kafka**: Event distribution with contract-enforced compatibility

All three contract types work together to enable independent team development.

## When Contract-First Makes Sense

✅ **Use contract-first when:**
- Multiple teams integrating (reduces coordination overhead)
- Different release schedules (contracts provide stability)
- Integration timeline is critical (parallel development matters)
- Long-lived systems (schema evolution protection is valuable)

❌ **Skip when:**
- Single team owns everything (no coordination needed)
- Prototype phase (contracts slow exploration)
- Expected major pivots (contracts constrain flexibility)

## Summary

Contract-first integration with Spring Boot, Kafka, and Flyway enables:

1. **Parallel Development**: Provider and consumer work simultaneously from day one
2. **Reduced Integration Time**: 7 weeks → 3 weeks (70% parallel)
3. **Early Validation**: CI/CD catches breaking changes in PRs, not production
4. **Schema Evolution Safety**: Avro + Schema Registry prevent breaking changes
5. **Zero-Downtime Migrations**: Flyway expand/migrate/contract pattern
6. **Team Independence**: Each team moves at their own pace

The contract is the synchronization point. Both teams implement independently. CI/CD enforces alignment.

---

**Full working code**: [github.com/wallaceespindola/contract-first-integrations](https://github.com/wallaceespindola/contract-first-integrations)

**Key technologies**:
- OpenAPI 3.0+: [spec.openapis.org](https://spec.openapis.org)
- Apache Avro: [avro.apache.org](https://avro.apache.org)
- Confluent Schema Registry: [docs.confluent.io/schema-registry](https://docs.confluent.io/platform/current/schema-registry/)
- Flyway Migrations: [flywaydb.org](https://flywaydb.org)
- Spring Boot 3.4+: [spring.io](https://spring.io)
- Apache Kafka: [kafka.apache.org](https://kafka.apache.org)

---

Check out my [GitHub](https://github.com/wallaceespindola) for more examples.

Happy coding!
