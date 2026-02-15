# Complete Guide to Contract-First Integration with Spring Boot

## Building REST APIs, Kafka events, and database schemas with OpenAPI, Avro, and Flyway

![Contract-First Integration Complete Guide](../images/medium-featured-contract-first.png)

Contract-first development means defining integration contracts before writing implementation code. The contract becomes the single source of truth that generates server stubs, client SDKs, mock servers, and documentation.

This guide walks through implementing contract-first integration for all three boundaries in distributed systems: REST APIs, Kafka events, and database schemas.

## Prerequisites

- Java 21+
- Spring Boot 3.4+
- Apache Kafka with Confluent Schema Registry
- PostgreSQL
- Maven 3.9+

## Part 1: REST API Contracts with OpenAPI

### Step 1: Write the OpenAPI Contract

Create `contracts/openapi/orders-api.v1.yaml`:

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
          description: Validation error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
        '409':
          description: Idempotency conflict
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'

  /v1/orders/{orderId}:
    get:
      operationId: getOrder
      summary: Retrieve an order by ID
      parameters:
        - name: orderId
          in: path
          required: true
          schema:
            type: string
      responses:
        '200':
          description: Order found
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/OrderResponse'
        '404':
          description: Order not found

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
          description: Optional key for safe retries
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

### Step 2: Implement the Spring Boot Controller

Create DTOs as Java records:

```java
// CreateOrderRequest.java
public record CreateOrderRequest(
        String customerId,
        String idempotencyKey,
        List<OrderItem> items
) {}

// OrderItem.java
public record OrderItem(
        String sku,
        Integer quantity
) {}

// OrderResponse.java
public record OrderResponse(
        String orderId,
        String customerId,
        String status,
        List<OrderItem> items,
        String timestamp
) {}

// ErrorResponse.java
public record ErrorResponse(
        String code,
        String message,
        String traceId,
        String timestamp
) {}
```

Implement the controller:

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

    /**
     * GET /v1/orders/{orderId}
     */
    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrder(@PathVariable String orderId) {
        OrderResponse response = orderService.getOrder(orderId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Order not found: " + orderId));

        return ResponseEntity.ok(response);
    }
}
```

### Step 3: Add Request Validation

Add Bean Validation annotations:

```java
public record CreateOrderRequest(
        @NotBlank(message = "customerId must not be blank")
        @Pattern(regexp = "^CUST-[0-9]{6,10}$", message = "invalid customerId format")
        String customerId,

        @Pattern(regexp = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$",
                message = "idempotencyKey must be valid UUID")
        String idempotencyKey,

        @NotEmpty(message = "items must not be empty")
        @Valid
        List<OrderItem> items
) {}

public record OrderItem(
        @NotBlank(message = "sku must not be blank")
        String sku,

        @NotNull(message = "quantity must not be null")
        @Min(value = 1, message = "quantity must be at least 1")
        Integer quantity
) {}
```

Add global exception handler:

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String traceId = extractTraceId(request);
        Map<String, List<String>> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.computeIfAbsent(error.getField(), k -> new ArrayList<>())
                        .add(error.getDefaultMessage())
        );

        log.error("Validation error: traceId={}, errors={}", traceId, errors);

        ErrorResponse response = new ErrorResponse(
                "VALIDATION_ERROR",
                "Request validation failed",
                traceId,
                Instant.now().toString()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        String traceId = extractTraceId(request);
        log.error("Resource not found: traceId={}, message={}", traceId, ex.getMessage());

        ErrorResponse response = new ErrorResponse(
                "NOT_FOUND",
                ex.getMessage(),
                traceId,
                Instant.now().toString()
        );

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    private String extractTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        return (traceId != null) ? traceId : UUID.randomUUID().toString();
    }
}
```

### Step 4: Implement the Service Layer

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        // Create order entity
        OrderEntity entity = OrderEntity.builder()
                .id(generateOrderId())
                .customerId(request.customerId())
                .status(OrderStatus.CREATED)
                .createdAt(Instant.now())
                .build();

        // Save order
        entity = orderRepository.save(entity);

        // Save order items
        request.items().forEach(item -> {
            OrderItemEntity itemEntity = OrderItemEntity.builder()
                    .orderId(entity.getId())
                    .sku(item.sku())
                    .quantity(item.quantity())
                    .build();
            orderItemRepository.save(itemEntity);
        });

        // Publish event
        OrderCreated event = OrderCreated.newBuilder()
                .setEventId(UUID.randomUUID().toString())
                .setOccurredAt(Instant.now().toString())
                .setOrderId(entity.getId())
                .setCustomerId(entity.getCustomerId())
                .setItems(convertItems(request.items()))
                .build();

        eventPublisher.publishOrderCreated(event);

        // Return response
        return new OrderResponse(
                entity.getId(),
                entity.getCustomerId(),
                entity.getStatus().name(),
                request.items(),
                entity.getCreatedAt().toString()
        );
    }

    public Optional<OrderResponse> getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .map(this::toResponse);
    }

    private String generateOrderId() {
        return "ORD-" + System.currentTimeMillis();
    }

    private OrderResponse toResponse(OrderEntity entity) {
        List<OrderItem> items = orderItemRepository.findByOrderId(entity.getId())
                .stream()
                .map(item -> new OrderItem(item.getSku(), item.getQuantity()))
                .toList();

        return new OrderResponse(
                entity.getId(),
                entity.getCustomerId(),
                entity.getStatus().name(),
                items,
                entity.getCreatedAt().toString()
        );
    }
}
```

## Part 2: Event Contracts with Kafka and Avro

### Step 1: Define the Avro Schema

Create `contracts/events/avro/OrderCreated.v1.avsc`:

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

### Step 2: Configure Avro Maven Plugin

Add to `pom.xml`:

```xml
<plugin>
    <groupId>org.apache.avro</groupId>
    <artifactId>avro-maven-plugin</artifactId>
    <version>1.12.0</version>
    <executions>
        <execution>
            <phase>generate-sources</phase>
            <goals>
                <goal>schema</goal>
            </goals>
            <configuration>
                <sourceDirectory>${project.basedir}/contracts/events/avro</sourceDirectory>
                <outputDirectory>${project.build.directory}/generated-sources/avro</outputDirectory>
            </configuration>
        </execution>
    </executions>
</plugin>
```

Run `mvn generate-sources` to generate Java classes from Avro schemas.

### Step 3: Implement Kafka Producer

Configure Kafka with Avro serialization:

```java
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        props.put("schema.registry.url", schemaRegistryUrl);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

Implement the publisher:

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

### Step 4: Implement Kafka Consumer

Configure Kafka consumer with Avro deserialization:

```java
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.properties.schema.registry.url}")
    private String schemaRegistryUrl;

    @Bean
    public ConsumerFactory<String, OrderCreated> consumerFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put("specific.avro.reader", true);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, OrderCreated>
    kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, OrderCreated> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
```

Implement idempotent consumer:

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

        log.debug("Received OrderCreated: eventId={}, orderId={}",
                eventId, event.getOrderId());

        // Check if already processed
        if (processedEventRepository.existsByEventId(eventId)) {
            log.debug("Skipping duplicate event: {}", eventId);
            return;
        }

        // Process event
        billingService.createInvoice(
                event.getOrderId(),
                event.getCustomerId(),
                event.getItems()
        );

        // Mark as processed
        processedEventRepository.save(
                new ProcessedEvent(eventId, Instant.now())
        );

        log.info("Processed OrderCreated: eventId={}, orderId={}",
                eventId, event.getOrderId());
    }
}
```

## Part 3: Database Contracts with Flyway

### Step 1: Create Initial Migration

Create `src/main/resources/db/migration/V1__create_orders_schema.sql`:

```sql
-- Orders table
CREATE TABLE orders (
    id           VARCHAR(32) PRIMARY KEY,
    customer_id  VARCHAR(32) NOT NULL,
    status       VARCHAR(16) NOT NULL,
    created_at   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Order items table
CREATE TABLE order_items (
    order_id  VARCHAR(32) NOT NULL,
    sku       VARCHAR(64) NOT NULL,
    quantity  INT NOT NULL CHECK (quantity > 0),
    PRIMARY KEY (order_id, sku),
    FOREIGN KEY (order_id) REFERENCES orders(id) ON DELETE CASCADE
);

-- Processed events table (for Kafka idempotency)
CREATE TABLE processed_events (
    event_id      VARCHAR(255) PRIMARY KEY,
    processed_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes
CREATE INDEX idx_orders_customer_id ON orders(customer_id);
CREATE INDEX idx_order_items_order_id ON order_items(order_id);
```

### Step 2: Configure Flyway

Add to `application.yml`:

```yaml
spring:
  flyway:
    enabled: true
    locations: classpath:db/migration
    baseline-on-migrate: false
    validate-on-migrate: true
```

### Step 3: Implement Repository Layer

```java
@Repository
public interface OrderRepository extends JpaRepository<OrderEntity, String> {
    List<OrderEntity> findByCustomerId(String customerId);
}

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItemEntity, OrderItemId> {
    List<OrderItemEntity> findByOrderId(String orderId);
}

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEventEntity, String> {
    boolean existsByEventId(String eventId);
}
```

Define JPA entities:

```java
@Entity
@Table(name = "orders")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderEntity {

    @Id
    private String id;

    @Column(name = "customer_id", nullable = false)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}

@Entity
@Table(name = "order_items")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@IdClass(OrderItemId.class)
public class OrderItemEntity {

    @Id
    @Column(name = "order_id")
    private String orderId;

    @Id
    private String sku;

    @Column(nullable = false)
    private Integer quantity;
}

@Entity
@Table(name = "processed_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEventEntity {

    @Id
    @Column(name = "event_id")
    private String eventId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
```

## Part 4: CI/CD Enforcement

### Step 1: OpenAPI Breaking Change Detection

Create `.github/workflows/contract-validation.yml`:

```yaml
name: Contract Validation

on:
  pull_request:
    paths:
      - 'contracts/**'

jobs:
  openapi-validation:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
        with:
          fetch-depth: 0

      - name: Check for API breaking changes
        run: |
          npx openapi-diff \
            origin/main:contracts/openapi/orders-api.v1.yaml \
            HEAD:contracts/openapi/orders-api.v1.yaml \
            --fail-on-breaking
```

### Step 2: Schema Registry Compatibility

Add to `pom.xml`:

```xml
<plugin>
    <groupId>io.confluent</groupId>
    <artifactId>kafka-schema-registry-maven-plugin</artifactId>
    <version>7.8.1</version>
    <configuration>
        <schemaRegistryUrls>
            <param>http://schema-registry:8081</param>
        </schemaRegistryUrls>
        <subjects>
            <orders.order-created.v1-value>
                contracts/events/avro/OrderCreated.v1.avsc
            </orders.order-created.v1-value>
        </subjects>
        <compatibilityLevel>BACKWARD</compatibilityLevel>
    </configuration>
    <executions>
        <execution>
            <phase>test</phase>
            <goals>
                <goal>test-compatibility</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

### Step 3: Flyway Validation

Flyway validates automatically on startup with:

```properties
spring.flyway.validate-on-migrate=true
```

If someone manually modifies the database, Flyway detects the mismatch and fails the deployment.

## Testing the Complete Flow

### Integration Test

```java
@SpringBootTest
@TestContainers
class OrderIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16");

    @Container
    static KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.8.0"));

    @Autowired
    private OrderController orderController;

    @Autowired
    private ProcessedEventRepository processedEventRepository;

    @Test
    void shouldCreateOrderAndPublishEvent() throws Exception {
        // Create order
        CreateOrderRequest request = new CreateOrderRequest(
                "CUST-123456",
                UUID.randomUUID().toString(),
                List.of(new OrderItem("SKU-001", 2))
        );

        ResponseEntity<OrderResponse> response =
                orderController.createOrder(request);

        // Verify response
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().orderId()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo("CREATED");

        // Wait for event processing
        await().atMost(5, TimeUnit.SECONDS).until(() ->
                processedEventRepository.count() > 0
        );

        // Verify event was processed
        assertThat(processedEventRepository.count()).isEqualTo(1);
    }
}
```

## Summary

I want to emphasize the core insight: **The contract is the synchronization point.** When you define REST APIs (OpenAPI), Kafka events (Avro), and database schemas (Flyway) upfront, both provider and consumer teams can work independently and in parallel from day one.

This isn't just theoretical. The technical patterns shown here—mock server generation, automatic client SDK creation, idempotent consumer design, backward-compatible migrations—all work together to make integration fast and predictable.

## Ready to Build Faster?

Contract-first integration has transformed how distributed teams work together. Whether you're building microservices, event-driven systems, or multi-service architectures, these patterns help you move faster while maintaining quality.

**The full working code is available here**: [github.com/wallaceespindola/contract-first-integrations](https://github.com/wallaceespindola/contract-first-integrations)

All the examples in this guide—OpenAPI contracts, Avro schemas, Flyway migrations, Spring Boot implementation, and end-to-end integration tests—are available in the repository.

**Key technologies covered**:
- **OpenAPI 3.2** — REST API contract definition and mock server generation
- **Apache Avro 1.12** — Event schema definition with backward compatibility
- **Confluent Schema Registry 7.8** — Automatic schema compatibility validation
- **Flyway 10.x** — Versioned database migrations with zero-downtime deployments
- **Spring Boot 3.4** — Unified implementation framework for providers and consumers

---

## Let's Connect

Have you experienced the pain of sequential team dependencies? How has contract-first development changed your integration velocity? **I'd love to hear your thoughts in the comments below.** 👇

Follow me for more on distributed systems architecture, API design patterns, and practical Spring Boot implementation. If this helped you, please give it some claps—it means a lot and helps other engineers discover these patterns.

**#SpringBoot #Kafka #APIDevelopment #SoftwareArchitecture #Microservices**
