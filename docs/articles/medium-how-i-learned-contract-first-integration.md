# How I Learned Contract-First Integration the Hard Way (So You Don't Have To)

## The costly integration failure that taught me why contracts matter more than code

![Contract-First Integration Journey](../images/medium-featured-contract-first.png)

I was integrating our order management system with a new billing service. Tight deadlines. I made a call that would cost us weeks.

"We don't need formal API contracts. We'll just talk through the integration and start coding. Contracts are bureaucracy."

I was wrong.

## The Day Everything Broke

Both services were ready. We deployed to staging, confident this would be a quick integration test.

The first API call failed.

Not a timeout. Not a 500 error. A silent failure. The order service sent a request. The billing service received it, processed it, and returned 200 OK. But no invoice was created.

After debugging, we found the problem. Our order service sent this payload:

```json
{
  "customerId": "CUST-123",
  "items": [
    {"sku": "SKU-001", "quantity": 2}
  ]
}
```

The billing service expected this:

```json
{
  "customer_id": "CUST-123",
  "order_items": [
    {"product_sku": "SKU-001", "qty": 2}
  ]
}
```

Same data. Different field names. The billing service's validator failed silently and returned 200 anyway (another bug). Two weeks debugging field name mismatches and validation logic.

That's when I learned my first lesson about contract-first integration: **When teams make assumptions instead of agreements, those assumptions diverge**.

## What Contract-First Actually Means

After that disaster, I researched how companies like Netflix, Uber, and Amazon handle integration. They all use contract-first development, but it took me a while to understand what that really means.

Here's the simple version: **You define the contract before you write any code, and the contract becomes the single source of truth.**

Not documentation that gets generated from code annotations. Not a Slack conversation about "yeah, just send the customer ID and items." A formal, machine-readable contract that both teams validate against.

For REST APIs, that's OpenAPI 3.0+ specs. For event-driven systems, that's Avro or Protobuf schemas with Schema Registry. For databases, that's Flyway migrations that version your schema changes.

The mental shift is this: The contract isn't documentation. **The contract is the design specification that generates your code, mocks, tests, and docs.**

## My First Real OpenAPI Contract

After that disaster, I convinced my team to try contract-first. We were building an order creation API that would publish events to Kafka.

I started with the OpenAPI spec. Not code. Just the contract.

Writing a good contract forces you to think through edge cases upfront. Invalid customer ID? Empty items array? Duplicate retries? You can't wave your hands and say "we'll handle it later."

Here's the contract I wrote (simplified):

```yaml
openapi: 3.2.0
info:
  title: Orders API
  version: 1.0.0
  description: Contract-first REST API for order management

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
          description: Order created successfully
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/OrderResponse'
        '400':
          description: Validation error - invalid request
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '409':
          description: Conflict - idempotency key reused with different payload
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
          description: The customer's unique identifier
          example: CUST-123
        idempotencyKey:
          type: string
          description: Optional key for safe retries
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

    OrderResponse:
      type: object
      required: [orderId, customerId, status, items, timestamp]
      properties:
        orderId:
          type: string
        customerId:
          type: string
        status:
          type: string
          enum: [CREATED, REJECTED]
        items:
          type: array
          items:
            $ref: '#/components/schemas/OrderItem'
        timestamp:
          type: string
          format: date-time

    ErrorResponse:
      type: object
      required: [code, message, traceId, timestamp]
      properties:
        code:
          type: string
          enum: [VALIDATION_ERROR, NOT_FOUND, CONFLICT, INTERNAL_ERROR]
        message:
          type: string
        traceId:
          type: string
        timestamp:
          type: string
          format: date-time
```

Three lessons from writing this contract first:

**Idempotency is a design requirement, not a feature**

The `idempotencyKey` field came from thinking through: what if the client's network fails after we process the order but before they receive the response? They retry. Without idempotency, we create duplicate orders. The contract forced me to design for this upfront, not discover it in production.

**Error responses need structure**

Standardized `ErrorResponse` with machine-readable `code` and `traceId`. Every error has the same shape. Clients can write reliable error handling. Before this, our errors were inconsistent: sometimes strings, sometimes objects, sometimes no trace IDs.

**Examples are documentation**

Every field has an example value. New developers look at the spec and immediately understand valid data.

## Implementing Against the Contract

With the contract done, implementation was straightforward:

```java
@RestController
@RequestMapping("/v1/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    /**
     * POST /v1/orders
     * Contract: contracts/openapi/orders-api.v1.yaml
     */
    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request) {

        log.info("Creating order for customer: {}", request.customerId());

        OrderResponse response = orderService.createOrder(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        OrderResponse response = orderService.getOrder(orderId)
                .orElseThrow(() ->
                    new ResourceNotFoundException("Order not found: " + orderId));

        return ResponseEntity.ok(response);
    }
}
```

The key difference: I validated my implementation against the contract. Contract validation tools ensured my DTOs matched the OpenAPI schemas exactly. No more field name mismatches.

## The Moment I Understood the Real Value

The billing team needed to start integration work. My order service wasn't finished.

In the old world, they would've waited. But with the contract, they didn't have to.

They:
1. Generated a Java client from our OpenAPI spec
2. Spun up a mock server that returns valid responses based on the contract
3. Wrote their integration code against the mock
4. Tested everything end-to-end without my service running

When I deployed the real service, they switched from mock to real endpoint. One config change. Everything worked first try.

**Contract-first isn't about preventing bugs. It's about enabling parallel development.**

Multiple teams working simultaneously instead of sequentially.

## Moving to Event-Driven: Kafka and Avro Schemas

Next, I applied contract-first to our event-driven architecture. Publishing order creation events to Kafka for multiple consumers.

Event contracts have two layers:

**Layer 1: Topic semantics** (operational contract)
**Layer 2: Schema definition** (data contract)

Topic semantics:

```markdown
## Topic: orders.order-created.v1

- Purpose: Emitted when an order is created successfully
- Key: orderId (ensures ordering per customer)
- Delivery: At-least-once (consumers MUST be idempotent)
- Consumer requirement: Deduplicate by eventId
- Retry policy: Consumer retries transient errors
- DLQ: orders.order-created.v1.dlq for poison messages
- Compatibility: Backward compatible schema evolution required
```

The Avro schema is the data contract:

```json
{
  "type": "record",
  "name": "OrderCreated",
  "namespace": "com.acme.events",
  "doc": "Event emitted when an order is successfully created",
  "fields": [
    {
      "name": "eventId",
      "type": "string",
      "doc": "Unique event ID for idempotent processing"
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
      "name": "source",
      "type": ["null", "string"],
      "default": null,
      "doc": "Order source (WEB, MOBILE, API). Nullable for backward compatibility."
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
    }
  ]
}
```

## The Schema Evolution Lesson

Product wanted a new feature: track where orders came from (web, mobile, API).

I needed to add a `source` field. My first instinct: required string field.

**This would've broken all existing consumers.** They'd receive unexpected fields and potentially crash.

The right approach: Make it nullable with a default value.

```json
{
  "name": "source",
  "type": ["null", "string"],
  "default": null
}
```

This is backward compatible:
- Old consumers reading new events: They ignore the `source` field (or get null)
- New consumers reading old events: They get null for `source`
- Nobody breaks

Schema Registry validated backward compatibility. If I'd tried a required field, it would've rejected it.

**Lesson: Design for schema evolution from day one.**

## The Kafka Producer Implementation

Production-safe event publishing:

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(OrderCreated event) {
        String key = event.getOrderId();

        log.debug("Publishing OrderCreated: orderId={}, eventId={}",
                key, event.getEventId());

        CompletableFuture<SendResult<String, Object>> future =
                kafkaTemplate.send("orders.order-created.v1", key, event);

        future.whenComplete((result, ex) -> {
            if (ex == null) {
                log.info("Published OrderCreated: orderId={}, partition={}, offset={}",
                        key,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            } else {
                log.error("Failed to publish OrderCreated: orderId={}", key, ex);
            }
        });
    }
}
```

Three production patterns:

**Key-based partitioning**: `orderId` as message key ensures all events for the same order go to the same partition, preserving ordering.

**Async with explicit error handling**: `CompletableFuture` with callbacks instead of blocking. Keeps the API responsive when Kafka is slow.

**Structured logging**: Log partition and offset on every publish. Critical for debugging production issues.

## The Idempotency Pattern That Saved Us

Kafka guarantees at-least-once delivery, not exactly-once. Events can arrive twice.

Consumers must be idempotent:

```java
@KafkaListener(topics = "orders.order-created.v1", groupId = "billing-service")
public void onOrderCreated(OrderCreated event) {
    // Step 1: Check if we've already processed this event
    if (processedEventsRepository.existsByEventId(event.getEventId())) {
        log.debug("Skipping duplicate event: {}", event.getEventId());
        return;
    }

    // Step 2: Process the event
    billingService.createInvoice(
            event.getOrderId(),
            event.getCustomerId(),
            event.getItems()
    );

    // Step 3: Mark as processed
    processedEventsRepository.save(
            new ProcessedEvent(event.getEventId(), Instant.now())
    );
}
```

The `eventId` in the schema isn't just metadata. It's the deduplication key. Check before processing, store after.

I learned this when we found duplicate invoices in production. Kafka redelivered events during a rebalance. Without idempotency checking, we billed customers twice.

## Making Contracts Enforceable With CI/CD

Contracts are useless without enforcement. Three CI gates:

### Gate 1: OpenAPI Breaking Change Detection

```yaml
# .github/workflows/api-contract-check.yml
- name: Check for API breaking changes
  run: |
    npx openapi-diff \
      main:contracts/openapi/orders-api.v1.yaml \
      HEAD:contracts/openapi/orders-api.v1.yaml \
      --fail-on-breaking
```

Runs on every PR. Remove a required field? Change a response structure? Build fails before merge.

### Gate 2: Schema Registry Compatibility Check

Schema Registry enforces backward compatibility:

```properties
spring.kafka.producer.properties.auto.register.schemas=true
spring.kafka.producer.properties.use.latest.version=true
```

Producer tries to register the schema at startup. Incompatible schema? Registration fails. Service won't start. This caught three breaking changes during development.

### Gate 3: Flyway Migration Validation

Flyway with strict validation:

```properties
spring.flyway.validate-on-migrate=true
spring.flyway.baseline-on-migrate=false
```

Manual schema modification without migration? Flyway detects the mismatch and fails deployment.

## The Results After Six Months

After implementing contract-first across order, billing, and inventory services:

**Integration Quality:**
- Integration bugs reaching production dropped significantly
- Most remaining bugs were business logic issues, not contract mismatches
- Clear contracts eliminated the "what did you expect?" debugging sessions

**Development Speed:**
- Consumer teams started work immediately instead of waiting for providers
- Mock servers enabled realistic testing without coordination overhead
- Integration cycles measured in weeks instead of months

**Operational Reliability:**
- CI caught potential breaking changes during PR reviews
- No breaking changes reached production after implementing automated validation
- Deployment confidence increased with contractual guarantees

**Documentation Accuracy:**
- The OpenAPI spec generates the Swagger UI, so docs stay synchronized with implementation by design
- No manual documentation maintenance required

**Developer Experience:**
- "I don't have to guess what the API returns anymore"
- "I can start integration work the same day the contract is done"
- "Debugging is dramatically faster because the contract tells me what's expected"

## What I'd Do Differently

If I could go back and start over, here's what I'd change:

**1. Start with contracts from day one**

I wish I'd learned this lesson before the costly billing integration failure. Writing contracts first feels slow at first, but it's way faster than debugging mismatched assumptions in production.

**2. Invest in better tooling earlier**

I spent too long manually validating contracts. Now we have automated tools (openapi-generator, Schema Registry, Flyway) that do this in CI. I should've set these up from the start.

**3. Document operational behavior in contracts**

The Avro schema tells you the data shape, but it doesn't tell you that consumers must be idempotent or that messages are keyed by orderId for ordering guarantees. I now include operational contracts (like the topic semantics doc) alongside data contracts.

**4. Test schema evolution scenarios**

I didn't test backward compatibility until it became a problem. Now I have integration tests that load old schema versions and ensure new code can handle them.

## When Contract-First Doesn't Make Sense

Contract-first isn't always the answer. I skip it when:

- **I'm prototyping**: If I expect major pivots, formal contracts add friction
- **I'm building internal tools for myself**: Coordination overhead is zero
- **The team is tiny**: If three people sit next to each other, they can align without contracts

But for distributed systems with multiple teams, different release schedules, and external consumers, contract-first is the only approach I've seen that scales.

## What I Learned About Organizations

The technical lessons were valuable, but I learned something deeper: **Contract-first is as much about people as it is about code.**

When two teams integrate without a contract, they're playing telephone. One team says "send me customer data." The other team assumes "customer data" means name and email. The first team expected name, email, and address. Both teams think they communicated clearly.

Contracts force teams to have precise conversations upfront. Instead of "send me customer data," you write an OpenAPI schema that explicitly lists every required field.

These conversations are sometimes uncomfortable. People have to admit they don't know exactly what they need. They have to make decisions before they've written code. But these conversations prevent weeks of rework later.

**Contract-first trades upfront alignment cost for massive coordination savings later.**

## The Simple Framework I Use Now

Every time I start a new integration, I follow this process:

**Day 1: Contract design**
- Write OpenAPI spec (for REST) or Avro schema (for events)
- Include examples for every field
- Document error cases and edge cases
- Get both teams to review and approve

**Day 2: Validation setup**
- Set up CI checks for breaking changes
- Configure Schema Registry for compatibility validation
- Create mock server for consumers to develop against

**Day 3+: Parallel development**
- Provider team implements against contract
- Consumer team develops against mocks
- Both teams validate their code against the contract

**Integration day: Switch from mock to real**
- Consumer changes one config value (mock URL → real URL)
- Run integration tests
- Fix any issues (usually business logic, not contracts)

This framework has worked for REST APIs, Kafka events, gRPC services, and even database integrations with Flyway.

## What's Next for You?

If you're dealing with painful cross-team integrations, here's how to start:

**Pick one integration**: your most painful one. The one where teams are constantly miscommunicating.

**Write the contract first**: before any implementation. OpenAPI for REST, Avro for events.

**Set up CI validation**: automated breaking change detection.

**Measure the difference**: track integration bugs, coordination overhead, time to integration.

**Expand gradually**: once you've proven the pattern, apply it to other integrations.

Don't try to do everything at once. Start small, prove value, expand.

## Final Thought

Three years ago, I thought contracts were bureaucratic overhead. "Just talk it through and start coding," I said.

I was measuring the wrong thing. I focused on time-to-first-line-of-code when I should've focused on time-to-working-integration.

Contract-first feels slower upfront. You're writing YAML before you write Java. You're having design conversations before you spin up your IDE. But it's dramatically faster overall because you avoid the painful debugging cycles where teams discover their assumptions didn't match.

The lesson I learned: In distributed systems, alignment is more expensive than you think, and misalignment is more costly than you can afford.

Contracts are how you scale alignment.

---

**Full source code**: [github.com/wallaceespindola/contract-first-integrations](https://github.com/wallaceespindola/contract-first-integrations)

**Want to dive deeper?**
- Read the complete contract-first guide in the repo's `/docs` folder
- Check out the working Spring Boot + Kafka + OpenAPI implementation
- See the CI/CD setup that enforces contracts automatically

---

Thanks for reading! If this helped you understand contract-first integration, give it a clap 👏 and follow for more software architecture stories.

**What's been your worst integration disaster?** Share in the comments. I'd love to hear your stories.

---

Need more tech insights?

Check out my [GitHub](https://github.com/wallaceespindola), [LinkedIn](https://www.linkedin.com/in/wallaceespindola/), and [Speaker Deck](https://speakerdeck.com/wallacese).

Happy coding!
