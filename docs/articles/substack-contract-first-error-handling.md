# Contract-First Error Handling: Standardized Responses and Dead-Letter Queues

## Building robust error handling into your contracts for REST APIs and Kafka events

![Contract-First Error Handling](../images/substack-featured-contract-first.png)

Error handling in distributed systems is where theoretical contracts meet production reality. This article covers practical patterns for building error handling into your REST API and Kafka contracts—patterns that make debugging failures faster and recovery automatic.

We'll implement standardized error responses for REST APIs and dead-letter queue patterns for Kafka consumers, both defined in the contract upfront.

## The Error Handling Problem

Integration contracts typically focus on the happy path: What request format produces what response? But production systems spend most of their time handling failures:

- Invalid request payloads
- Missing required fields
- Downstream service timeouts
- Database connection failures
- Schema deserialization errors
- Poison messages in Kafka

Without standardized error handling in the contract, each failure mode produces ad-hoc error responses that consumers struggle to parse and handle.

## Pattern 1: Standardized REST API Error Responses

Define error responses in the OpenAPI contract with machine-readable codes and structured details.

### The Error Response Contract

```yaml
openapi: 3.2.0
info:
  title: Orders API
  version: 1.0.0

paths:
  /v1/orders:
    post:
      operationId: createOrder
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
          description: Validation error - invalid request payload
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              examples:
                validation_error:
                  summary: Validation failure example
                  value:
                    code: "VALIDATION_ERROR"
                    message: "Request validation failed"
                    traceId: "6b22c6b3c2c14c9aa9bafdf5c3f1c4f1"
                    timestamp: "2024-02-06T10:30:00.000Z"
                    details:
                      customerId:
                        - "customerId must match pattern ^CUST-[0-9]{6,10}$"
                      items:
                        - "items must contain at least one item"
        '409':
          description: Idempotency conflict
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              examples:
                idempotency_conflict:
                  summary: Idempotency key reused with different payload
                  value:
                    code: "CONFLICT"
                    message: "Idempotency key reused with different payload"
                    traceId: "a3d8e9f1b4c24d9ea8bafdf5c3f1c4f2"
                    timestamp: "2024-02-06T10:30:00.000Z"
        '500':
          description: Internal server error
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              examples:
                internal_error:
                  summary: Unexpected server error
                  value:
                    code: "INTERNAL_ERROR"
                    message: "An unexpected error occurred"
                    traceId: "d5e7f8a2c3b14d9ea9bafdf5c3f1c4f3"
                    timestamp: "2024-02-06T10:30:00.000Z"
        '503':
          description: Service temporarily unavailable
          content:
            application/json:
              schema:
                $ref: '#/components/schemas/ErrorResponse'
              examples:
                service_unavailable:
                  summary: Downstream dependency unavailable
                  value:
                    code: "SERVICE_UNAVAILABLE"
                    message: "Database connection pool exhausted"
                    traceId: "e6f8a9b3d4c25d9ea0cafdf5c3f1c4f4"
                    timestamp: "2024-02-06T10:30:00.000Z"

components:
  schemas:
    ErrorResponse:
      type: object
      required: [code, message, traceId, timestamp]
      properties:
        code:
          type: string
          description: Machine-readable error code for client error handling
          enum:
            - VALIDATION_ERROR
            - NOT_FOUND
            - CONFLICT
            - INTERNAL_ERROR
            - SERVICE_UNAVAILABLE
            - RATE_LIMIT_EXCEEDED
        message:
          type: string
          description: Human-readable error message
        traceId:
          type: string
          description: Distributed tracing correlation ID for log correlation
        timestamp:
          type: string
          format: date-time
          description: Error occurrence timestamp
        details:
          type: object
          description: Optional field-level validation errors
          additionalProperties:
            type: array
            items:
              type: string
```

**Key contract elements:**

1. **Machine-readable `code` enum**: Enables programmatic error handling
2. **Structured `details` object**: Provides field-level validation errors
3. **`traceId` for correlation**: Links errors to distributed traces
4. **Examples for every error type**: Documents what clients should expect

### Implementing Standardized Error Responses

Global exception handler:

```java
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * Handle validation errors (400 Bad Request)
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {

        String traceId = extractTraceId(request);
        Map<String, List<String>> details = extractValidationErrors(ex);

        log.error("Validation error: traceId={}, errors={}", traceId, details);

        ErrorResponse response = ErrorResponse.builder()
                .code("VALIDATION_ERROR")
                .message("Request validation failed")
                .traceId(traceId)
                .timestamp(Instant.now().toString())
                .details(details)
                .build();

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(response);
    }

    /**
     * Handle not found errors (404 Not Found)
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(
            ResourceNotFoundException ex,
            HttpServletRequest request) {

        String traceId = extractTraceId(request);
        log.error("Resource not found: traceId={}, message={}", traceId, ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .code("NOT_FOUND")
                .message(ex.getMessage())
                .traceId(traceId)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(response);
    }

    /**
     * Handle idempotency conflicts (409 Conflict)
     */
    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<ErrorResponse> handleConflictException(
            ConflictException ex,
            HttpServletRequest request) {

        String traceId = extractTraceId(request);
        log.error("Conflict: traceId={}, message={}", traceId, ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .code("CONFLICT")
                .message(ex.getMessage())
                .traceId(traceId)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(response);
    }

    /**
     * Handle downstream service failures (503 Service Unavailable)
     */
    @ExceptionHandler(ServiceUnavailableException.class)
    public ResponseEntity<ErrorResponse> handleServiceUnavailableException(
            ServiceUnavailableException ex,
            HttpServletRequest request) {

        String traceId = extractTraceId(request);
        log.error("Service unavailable: traceId={}, message={}", traceId, ex.getMessage());

        ErrorResponse response = ErrorResponse.builder()
                .code("SERVICE_UNAVAILABLE")
                .message(ex.getMessage())
                .traceId(traceId)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity
                .status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("Retry-After", "60")  // Suggest retry after 60 seconds
                .body(response);
    }

    /**
     * Handle unexpected errors (500 Internal Server Error)
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception ex,
            HttpServletRequest request) {

        String traceId = extractTraceId(request);
        log.error("Unexpected error: traceId={}", traceId, ex);

        ErrorResponse response = ErrorResponse.builder()
                .code("INTERNAL_ERROR")
                .message("An unexpected error occurred")
                .traceId(traceId)
                .timestamp(Instant.now().toString())
                .build();

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(response);
    }

    private String extractTraceId(HttpServletRequest request) {
        String traceId = request.getHeader("X-Trace-Id");
        return (traceId != null) ? traceId : UUID.randomUUID().toString();
    }

    private Map<String, List<String>> extractValidationErrors(
            MethodArgumentNotValidException ex) {

        Map<String, List<String>> errors = new HashMap<>();

        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.computeIfAbsent(error.getField(), k -> new ArrayList<>())
                        .add(error.getDefaultMessage())
        );

        return errors;
    }
}
```

### Client-Side Error Handling

Consumers can now handle errors programmatically:

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class OrdersApiClient {

    private final RestTemplate restTemplate;
    private final RetryTemplate retryTemplate;

    public OrderResponse createOrder(CreateOrderRequest request) {
        try {
            return retryTemplate.execute(context -> {
                ResponseEntity<OrderResponse> response = restTemplate.postForEntity(
                        "/v1/orders",
                        request,
                        OrderResponse.class
                );
                return response.getBody();
            });

        } catch (HttpClientErrorException.BadRequest e) {
            // Parse standardized error response
            ErrorResponse error = parseErrorResponse(e.getResponseBodyAsString());

            if ("VALIDATION_ERROR".equals(error.code())) {
                log.error("Validation failed: details={}", error.details());
                throw new ValidationException("Request validation failed", error.details());
            }

            throw new OrderApiException("Order creation failed", error);

        } catch (HttpClientErrorException.Conflict e) {
            ErrorResponse error = parseErrorResponse(e.getResponseBodyAsString());

            if ("CONFLICT".equals(error.code())) {
                log.warn("Idempotency conflict: message={}", error.message());
                throw new IdempotencyConflictException(error.message());
            }

            throw new OrderApiException("Order creation failed", error);

        } catch (HttpServerErrorException.ServiceUnavailable e) {
            ErrorResponse error = parseErrorResponse(e.getResponseBodyAsString());
            log.error("Service unavailable: message={}, retrying...", error.message());

            // Retry with backoff (handled by RetryTemplate)
            throw new ServiceUnavailableException(error.message());
        }
    }

    private ErrorResponse parseErrorResponse(String json) {
        try {
            return objectMapper.readValue(json, ErrorResponse.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse error response: {}", json, e);
            return ErrorResponse.builder()
                    .code("INTERNAL_ERROR")
                    .message("Failed to parse error response")
                    .build();
        }
    }
}
```

**Benefits of standardized errors:**
- Programmatic error handling based on `code` enum
- Field-level validation details enable precise error messages
- `traceId` links client errors to server logs
- Retry logic based on error codes (retry 503, don't retry 400)

## Pattern 2: Kafka Dead-Letter Queue with Error Context

When Kafka consumers fail to process events, route them to a dead-letter queue with full error context.

### Dead-Letter Queue Contract

Define DLQ schema in Avro:

```json
{
  "type": "record",
  "name": "DeadLetterEnvelope",
  "namespace": "com.acme.events.dlq",
  "doc": "Envelope for failed events routed to dead-letter queue",
  "fields": [
    {
      "name": "originalTopic",
      "type": "string",
      "doc": "Original Kafka topic where event was published"
    },
    {
      "name": "originalKey",
      "type": "string",
      "doc": "Original message key"
    },
    {
      "name": "partition",
      "type": "int",
      "doc": "Original partition"
    },
    {
      "name": "offset",
      "type": "long",
      "doc": "Original offset"
    },
    {
      "name": "consumerGroup",
      "type": "string",
      "doc": "Consumer group that failed to process"
    },
    {
      "name": "errorClass",
      "type": "string",
      "doc": "Exception class name"
    },
    {
      "name": "errorMessage",
      "type": "string",
      "doc": "Exception message"
    },
    {
      "name": "stackTrace",
      "type": ["null", "string"],
      "default": null,
      "doc": "Optional full stack trace for debugging"
    },
    {
      "name": "failedAt",
      "type": "string",
      "doc": "ISO 8601 timestamp when failure occurred"
    },
    {
      "name": "retryCount",
      "type": "int",
      "default": 0,
      "doc": "Number of retry attempts before DLQ"
    },
    {
      "name": "payloadBase64",
      "type": "string",
      "doc": "Original event payload encoded as Base64"
    },
    {
      "name": "payloadSchema",
      "type": "string",
      "doc": "Original event schema name and version"
    }
  ]
}
```

### Implementing DLQ Routing

Kafka consumer with error handling:

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCreatedListener {

    private final ProcessedEventRepository processedEventRepository;
    private final BillingService billingService;
    private final KafkaTemplate<String, Object> dlqTemplate;
    private final MeterRegistry meterRegistry;

    private static final String DLQ_TOPIC = "orders.order-created.v1.dlq";

    @KafkaListener(
            topics = "orders.order-created.v1",
            groupId = "billing-service",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void onOrderCreated(
            @Payload OrderCreated event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.RECEIVED_OFFSET) long offset) {

        String eventId = event.getEventId();

        log.debug("Received OrderCreated: eventId={}, orderId={}",
                eventId, event.getOrderId());

        try {
            // Idempotency check
            if (processedEventRepository.existsByEventId(eventId)) {
                log.debug("Skipping duplicate: eventId={}", eventId);
                meterRegistry.counter("kafka.events.duplicate").increment();
                return;
            }

            // Process event
            billingService.createInvoice(
                    event.getOrderId(),
                    event.getCustomerId(),
                    event.getItems()
            );

            // Mark processed
            processedEventRepository.save(
                    new ProcessedEvent(eventId, Instant.now())
            );

            log.info("Processed OrderCreated: eventId={}", eventId);
            meterRegistry.counter("kafka.events.processed", "status", "success").increment();

        } catch (ValidationException e) {
            // Business validation failure - route to DLQ
            log.error("Validation failed: eventId={}, error={}", eventId, e.getMessage());
            routeToDLQ(event, key, partition, offset, e, 0);
            meterRegistry.counter("kafka.events.dlq", "reason", "validation").increment();

        } catch (SQLException e) {
            // Database error - might be transient, retry
            log.error("Database error: eventId={}, error={}", eventId, e.getMessage(), e);

            // Throw exception to trigger Kafka consumer retry
            // After max retries, Kafka will route to DLQ via error handler
            throw new RetryableException("Database error", e);

        } catch (Exception e) {
            // Unexpected error - route to DLQ
            log.error("Unexpected error: eventId={}, error={}", eventId, e.getMessage(), e);
            routeToDLQ(event, key, partition, offset, e, 0);
            meterRegistry.counter("kafka.events.dlq", "reason", "unexpected").increment();
        }
    }

    private void routeToDLQ(
            OrderCreated event,
            String key,
            int partition,
            long offset,
            Exception error,
            int retryCount) {

        // Build DLQ envelope with full error context
        DeadLetterEnvelope dlqEnvelope = DeadLetterEnvelope.newBuilder()
                .setOriginalTopic("orders.order-created.v1")
                .setOriginalKey(key)
                .setPartition(partition)
                .setOffset(offset)
                .setConsumerGroup("billing-service")
                .setErrorClass(error.getClass().getName())
                .setErrorMessage(error.getMessage())
                .setStackTrace(getStackTrace(error))
                .setFailedAt(Instant.now().toString())
                .setRetryCount(retryCount)
                .setPayloadBase64(encodeEventToBase64(event))
                .setPayloadSchema("OrderCreated.v1")
                .build();

        // Publish to DLQ
        dlqTemplate.send(DLQ_TOPIC, key, dlqEnvelope)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        log.info("Routed to DLQ: eventId={}, dlqTopic={}",
                                event.getEventId(), DLQ_TOPIC);
                    } else {
                        log.error("Failed to route to DLQ: eventId={}",
                                event.getEventId(), ex);
                    }
                });
    }

    private String getStackTrace(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    private String encodeEventToBase64(OrderCreated event) {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            DatumWriter<OrderCreated> writer =
                    new SpecificDatumWriter<>(OrderCreated.class);
            Encoder encoder = EncoderFactory.get().binaryEncoder(output, null);
            writer.write(event, encoder);
            encoder.flush();
            return Base64.getEncoder().encodeToString(output.toByteArray());
        } catch (IOException e) {
            log.error("Failed to encode event to Base64", e);
            return event.toString();
        }
    }
}
```

### DLQ Consumer for Manual Intervention

Separate consumer for monitoring and replaying DLQ events:

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class DeadLetterQueueListener {

    private final DeadLetterRepository deadLetterRepository;
    private final AlertService alertService;

    @KafkaListener(
            topics = "orders.order-created.v1.dlq",
            groupId = "dlq-monitor",
            containerFactory = "dlqListenerContainerFactory"
    )
    public void onDeadLetterEvent(
            @Payload DeadLetterEnvelope envelope,
            @Header(KafkaHeaders.RECEIVED_KEY) String key) {

        log.error("DLQ event received: originalTopic={}, offset={}, error={}",
                envelope.getOriginalTopic(),
                envelope.getOffset(),
                envelope.getErrorMessage());

        // Store in database for investigation
        DeadLetterEntity entity = DeadLetterEntity.builder()
                .id(UUID.randomUUID().toString())
                .originalTopic(envelope.getOriginalTopic())
                .originalKey(key)
                .partition(envelope.getPartition())
                .offset(envelope.getOffset())
                .consumerGroup(envelope.getConsumerGroup())
                .errorClass(envelope.getErrorClass())
                .errorMessage(envelope.getErrorMessage())
                .stackTrace(envelope.getStackTrace())
                .failedAt(Instant.parse(envelope.getFailedAt()))
                .retryCount(envelope.getRetryCount())
                .payloadBase64(envelope.getPayloadBase64())
                .payloadSchema(envelope.getPayloadSchema())
                .status("PENDING_REVIEW")
                .createdAt(Instant.now())
                .build();

        deadLetterRepository.save(entity);

        // Send alert for manual investigation
        alertService.sendDLQAlert(envelope);

        log.info("DLQ event stored: id={}, originalTopic={}",
                entity.getId(), envelope.getOriginalTopic());
    }
}
```

### DLQ Replay Service

After fixing the root cause, replay failed events:

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class DeadLetterReplayService {

    private final DeadLetterRepository deadLetterRepository;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Replay a failed event back to original topic after manual fix.
     */
    public void replayEvent(String deadLetterId) {
        DeadLetterEntity dlq = deadLetterRepository.findById(deadLetterId)
                .orElseThrow(() -> new ResourceNotFoundException("DLQ event not found"));

        log.info("Replaying event: id={}, originalTopic={}",
                deadLetterId, dlq.getOriginalTopic());

        // Decode original payload
        byte[] payloadBytes = Base64.getDecoder().decode(dlq.getPayloadBase64());

        // Deserialize based on schema
        Object event = deserializeEvent(payloadBytes, dlq.getPayloadSchema());

        // Republish to original topic
        kafkaTemplate.send(dlq.getOriginalTopic(), dlq.getOriginalKey(), event)
                .whenComplete((result, ex) -> {
                    if (ex == null) {
                        // Mark as replayed
                        dlq.setStatus("REPLAYED");
                        dlq.setReplayedAt(Instant.now());
                        deadLetterRepository.save(dlq);

                        log.info("Event replayed successfully: id={}", deadLetterId);
                    } else {
                        log.error("Failed to replay event: id={}", deadLetterId, ex);
                    }
                });
    }

    private Object deserializeEvent(byte[] bytes, String schema) {
        // Deserialize based on schema name
        if ("OrderCreated.v1".equals(schema)) {
            try {
                ByteArrayInputStream input = new ByteArrayInputStream(bytes);
                DatumReader<OrderCreated> reader =
                        new SpecificDatumReader<>(OrderCreated.class);
                Decoder decoder = DecoderFactory.get().binaryDecoder(input, null);
                return reader.read(null, decoder);
            } catch (IOException e) {
                throw new RuntimeException("Failed to deserialize event", e);
            }
        }
        throw new IllegalArgumentException("Unknown schema: " + schema);
    }
}
```

## Observability: Error Metrics and Alerting

Track error patterns with Micrometer:

```java
@Component
@RequiredArgsConstructor
public class ErrorMetrics {

    private final MeterRegistry meterRegistry;

    public void recordApiError(String errorCode, String endpoint) {
        meterRegistry.counter("api.errors",
                "code", errorCode,
                "endpoint", endpoint).increment();
    }

    public void recordKafkaError(String topic, String errorType) {
        meterRegistry.counter("kafka.errors",
                "topic", topic,
                "type", errorType).increment();
    }

    public void recordDLQEvent(String topic, String reason) {
        meterRegistry.counter("kafka.dlq",
                "topic", topic,
                "reason", reason).increment();
    }
}
```

Grafana dashboard queries:

```promql
# API error rate by code
rate(api_errors_total[5m])

# Kafka DLQ events by topic
increase(kafka_dlq_total[1h])

# Validation error details
api_errors_total{code="VALIDATION_ERROR"}
```

Set up alerts:

```yaml
# alerts.yml
groups:
  - name: error_handling
    rules:
      - alert: HighAPIErrorRate
        expr: rate(api_errors_total[5m]) > 10
        for: 5m
        annotations:
          summary: "High API error rate detected"

      - alert: DLQEventsAccumulating
        expr: increase(kafka_dlq_total[1h]) > 100
        for: 10m
        annotations:
          summary: "Many events in DLQ - investigate"
```

## The Difference Error Contracts Make

I've learned this lesson the hard way. Early in my career, error handling was an afterthought—we'd define success paths beautifully, then scramble when things broke. The logs would be inconsistent. Field-level validation errors would come back with cryptic messages. Debugging would take hours.

Contract-first error handling flips this mindset. When you define `ErrorResponse` schemas, `DeadLetterEnvelope` structures, and DLQ routing patterns upfront, the entire team understands failure modes from day one. Logs correlate. Metrics make sense. Operational runbooks can be automated.

**REST APIs** need contracts for errors:
1. Define `ErrorResponse` schema with machine-readable codes
2. Include field-level validation details
3. Add `traceId` for log correlation
4. Provide examples for every error type

**Kafka consumers** need resilience contracts:
1. Define `DeadLetterEnvelope` schema with full error context
2. Route unprocessable events to DLQ
3. Store DLQ events for investigation and replay
4. Build replay mechanism for manual fixes

The result: Faster incident response, easier debugging, and teams that understand failure before production forces them to.

---

## Get the Full Implementation

I've published the complete working code with all patterns shown here:

**[github.com/wallaceespindola/contract-first-integrations](https://github.com/wallaceespindola/contract-first-integrations)**

You'll find OpenAPI error schemas, Avro DLQ envelope definitions, Spring Boot exception handlers, Kafka DLQ routing, and complete Micrometer monitoring setup—production-ready examples.

---

## Stay in the Conversation

I write about distributed systems architecture, contract-first patterns, and building reliable integrations. If you found this useful, **subscribe** to stay updated on future articles about systems design and practical Spring Boot patterns.

What's your experience with error handling in distributed systems? Have contract-first error schemas helped your team respond faster to failures? I'd love to hear your stories in the replies below.

---

*[GitHub](https://github.com/wallaceespindola) • [LinkedIn](https://www.linkedin.com/in/wallaceespindola/) • [Speaker Deck](https://speakerdeck.com/wallacese)*
