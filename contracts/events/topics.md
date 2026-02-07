# Kafka Topics and Semantics

This document defines the topic semantics and operational characteristics for Kafka events in the contract-first integrations system.

## orders.order-created.v1

### Purpose
Emitted when an order is created successfully in the orders service.

### Message Key
- **Format**: String
- **Value**: `orderId`
- **Rationale**: Ordering guaranteed per order. All events for the same order go to the same partition.

### Delivery Semantics
- **Guarantee**: At-least-once delivery
- **Implication**: Consumers may receive duplicate messages and must implement idempotent processing

### Consumer Requirements
- **Idempotency**: REQUIRED
  - Deduplicate using `eventId` field from the message payload
  - Store processed eventIds in a database or cache
  - Skip processing if eventId has already been seen
- **Error Handling**: Retry transient errors, send poison messages to DLQ

### Schema
- **Format**: Apache Avro
- **Schema File**: `contracts/events/avro/OrderCreated.v1.avsc`
- **Namespace**: `com.acme.events`
- **Compatibility**: Backward compatible evolution required

### Dead Letter Queue (DLQ)
- **Topic Name**: `orders.order-created.v1.dlq`
- **Purpose**: Capture messages that cannot be processed after retries
- **Schema**: `DeadLetterEnvelope.v1.avsc`

### Retention
- **Policy**: Time-based retention
- **Duration**: 7 days (configurable per environment)

### Partitions
- **Count**: 3 partitions (development)
- **Production**: Scale based on throughput requirements

### Replication
- **Factor**: 1 (development)
- **Production**: 3 for high availability

---

## orders.order-created.v1.dlq

### Purpose
Dead Letter Queue for failed `orders.order-created.v1` messages that cannot be processed after configured retry attempts.

### Message Key
- **Format**: String
- **Value**: Original `orderId` from the failed message

### Schema
- **Format**: Apache Avro
- **Schema File**: `contracts/events/avro/DeadLetterEnvelope.v1.avsc`
- **Fields**: Includes original topic, partition, offset, error details, and base64-encoded payload

### Monitoring
- **Alerts**: Configure alerts when DLQ receives messages
- **Investigation**: Review DLQ messages daily for systematic issues
- **Replay**: After fixing issues, messages can be replayed from the DLQ to the original topic

### Retention
- **Duration**: 30 days (longer retention for investigation and debugging)

---

## Schema Evolution Guidelines

### Backward Compatibility Rules
1. **Adding Fields**: New fields MUST have default values
2. **Removing Fields**: Mark as deprecated first, remove in next major version
3. **Changing Types**: NOT ALLOWED without major version bump
4. **Renaming Fields**: NOT ALLOWED, treat as add + remove

### Example: Adding `source` Field
```json
{
  "name": "source",
  "type": ["null", "string"],
  "default": null,
  "doc": "Source of the order"
}
```
- Old consumers ignore the new field (forward compatible)
- New consumers handle `null` gracefully (backward compatible)

---

## Consumer Groups

### billing
- **Purpose**: Create invoices for orders
- **Idempotency**: Tracks processed eventIds in `billing_processed_events` table
- **Error Strategy**: Retry 3 times, then send to DLQ

### inventory
- **Purpose**: Reserve inventory for ordered items
- **Idempotency**: Tracks processed eventIds in `inventory_processed_events` table
- **Error Strategy**: Retry 5 times, then send to DLQ

### notifications
- **Purpose**: Send order confirmation emails/SMS
- **Idempotency**: Tracks processed eventIds in `notifications_processed_events` table
- **Error Strategy**: Retry 3 times, failures are non-critical (logged only)

---

## Operational Notes

- **Monitoring**: Track consumer lag for all consumer groups
- **Schema Registry**: All schemas registered in Confluent Schema Registry
- **Compatibility Mode**: BACKWARD (enforced by Schema Registry)
- **Key Serializer**: StringSerializer
- **Value Serializer**: KafkaAvroSerializer (with Schema Registry)
