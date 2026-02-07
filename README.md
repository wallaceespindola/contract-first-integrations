# Contract-First Integrations

[![CI](https://github.com/wallaceespindola/contract-first-integrations/actions/workflows/ci.yml/badge.svg)](https://github.com/wallaceespindola/contract-first-integrations/actions/workflows/ci.yml)
[![CodeQL](https://github.com/wallaceespindola/contract-first-integrations/actions/workflows/codeql.yml/badge.svg)](https://github.com/wallaceespindola/contract-first-integrations/actions/workflows/codeql.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Reference implementation demonstrating **contract-first** (API-first / schema-first) development patterns for systems integration.

## 🎯 What is Contract-First?

Contract-first is an approach where you **define the integration boundary first** (the contract), then implement code that conforms to it. This repository demonstrates three types of contracts:

1. **REST API Contracts** (OpenAPI 3.x)
2. **Kafka Event Contracts** (Apache Avro + Schema Registry)
3. **Database Contracts** (Flyway migrations)

**Key Principle:** The contract is the single source of truth. Code, documentation, SDKs, mocks, and tests are all derived from contracts.

## 🚀 Quick Start

### Prerequisites

- Java 21+
- Maven 3.8+
- Docker & Docker Compose

### Run with Docker Compose

```bash
# Start full stack (PostgreSQL, Kafka, Zookeeper, Schema Registry, App)
make compose

# Access the application
open http://localhost:8080
open http://localhost:8080/swagger-ui.html
```

### Local Development

```bash
# 1. Install dependencies and generate Avro classes
make setup

# 2. Start infrastructure (PostgreSQL, Kafka, Schema Registry)
docker compose up postgres kafka schema-registry -d

# 3. Run application
make dev

# 4. Test the API
curl -X POST http://localhost:8080/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-123",
    "idempotencyKey": "test-key-001",
    "items": [{"sku": "SKU-001", "quantity": 2}]
  }'
```

## 📋 Features

### ✅ REST API (OpenAPI Contracts)

- **POST /v1/orders** - Create order with idempotency support
- **GET /v1/orders/{orderId}** - Retrieve order by ID
- **Idempotency**: Safe request retries using `idempotencyKey`
- **Error Handling**: Standardized `ErrorResponse` with correlation IDs
- **Swagger UI**: Interactive API documentation

### ✅ Kafka Events (Avro Schemas)

- **OrderCreated** events with backward-compatible schema evolution
- **Schema Registry**: Confluent Schema Registry for schema governance
- **Idempotent Consumers**: Event deduplication using `eventId`
- **Dead Letter Queue**: Failed message handling with debugging info

### ✅ Database (Flyway Migrations)

- **Versioned migrations**: V1 (initial schema), V2 (add source column)
- **Schema evolution**: Expand/migrate/contract pattern
- **Idempotency tracking**: Tables for REST and Kafka idempotency

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────────────┐
│                     Client/Consumer                     │
└────────────┬────────────────────────────┬───────────────┘
             │                            │
   ┌─────────▼──────────┐       ┌─────────▼─────────────┐
   │ REST API (OpenAPI) │       │    Kafka Consumer     │
   │  POST /v1/orders   │       │  OrderCreatedListener │
   └─────────┬──────────┘       └─────────▲─────────────┘
             │                            │
   ┌─────────▼────────────────────────────┴─────────┐
   │         Spring Boot Application                │
   │  ┌──────────────────────────────────────────┐  │
   │  │ OrderService (Business Logic)            │  │
   │  │  - Idempotency checking                  │  │
   │  │  - Order creation                        │  │
   │  │  - Event publishing                      │  │
   │  └──────────┬─────────────────┬─────────────┘  │
   │             │                 │                │
   │   ┌─────────▼──────┐    ┌─────▼──────────┐     │
   │   │ PostgreSQL     │    │  Kafka Broker  │     │
   │   │  - orders      │    │  + Schema      │     │
   │   │  - order_items │    │    Registry    │     │
   │   │  - idempotency │    │                │     │
   │   └────────────────┘    └────────────────┘     │
   └────────────────────────────────────────────────┘
```

## 📁 Project Structure

```
contract-first-integrations/
├── contracts/                    # First-class contract artifacts
│   ├── openapi/
│   │   └── orders-api.v1.yaml   # REST API contract
│   ├── events/
│   │   ├── avro/
│   │   │   ├── OrderCreated.v1.avsc
│   │   │   └── DeadLetterEnvelope.v1.avsc
│   │   ├── topics.md            # Topic semantics
│   │   └── asyncapi.yaml        # AsyncAPI documentation
│   └── db/
│       └── flyway/
│           ├── V1__create_orders.sql
│           └── V2__add_order_source.sql
├── src/main/java/
│   └── com/example/contractfirst/
│       ├── config/              # Spring configuration
│       ├── controller/          # REST controllers
│       ├── service/             # Business logic
│       ├── repository/          # Data access
│       ├── entity/              # JPA entities
│       ├── dto/                 # Java Record DTOs
│       ├── kafka/               # Kafka producers/consumers
│       ├── mapper/              # Entity/DTO mappers
│       └── exception/           # Custom exceptions
├── docker-compose.yml           # Full stack infrastructure
├── Makefile                     # Build commands
└── README.md
```

## 🛠️ Makefile Commands

```bash
make setup       # Install dependencies and generate Avro sources
make contracts   # Generate Java classes from Avro schemas
make dev         # Run application locally
make test        # Run tests
make test-cov    # Run tests with coverage report
make build       # Build JAR file
make docker      # Build Docker image
make compose     # Start full stack with docker-compose
make down        # Stop docker-compose stack
make clean       # Clean build artifacts
```

### Development Helpers

```bash
make logs              # View application logs
make kafka-topics      # List Kafka topics
make kafka-consume     # Consume OrderCreated events
make db-connect        # Connect to PostgreSQL
make db-migrate        # Run Flyway migrations
make db-info           # Show migration status
```

## 🧪 Testing

### Run Tests

```bash
# Run all tests
make test

# Run with coverage
make test-cov
open target/site/jacoco/index.html
```

### Test the REST API

```bash
# Create order
curl -X POST http://localhost:8080/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-123",
    "idempotencyKey": "unique-key-001",
    "items": [
      {"sku": "SKU-001", "quantity": 2},
      {"sku": "SKU-002", "quantity": 1}
    ]
  }'

# Get order
curl http://localhost:8080/v1/orders/ORD-XXXXX

# Test idempotency (same key, same payload → returns cached result)
curl -X POST http://localhost:8080/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-123",
    "idempotencyKey": "unique-key-001",
    "items": [
      {"sku": "SKU-001", "quantity": 2},
      {"sku": "SKU-002", "quantity": 1}
    ]
  }'

# Test idempotency conflict (same key, different payload → 409 Conflict)
curl -X POST http://localhost:8080/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-999",
    "idempotencyKey": "unique-key-001",
    "items": [{"sku": "SKU-999", "quantity": 5}]
  }'
```

### Verify Kafka Events

```bash
# List topics
make kafka-topics

# Consume OrderCreated events
make kafka-consume

# Check Schema Registry
curl http://localhost:8081/subjects
curl http://localhost:8081/subjects/orders.order-created.v1-value/versions
```

### Verify Database

```bash
# Connect to PostgreSQL
make db-connect

# Query tables
SELECT * FROM orders;
SELECT * FROM order_items;
SELECT * FROM idempotency_keys;
SELECT * FROM processed_events;
```

## 🔑 Key Patterns Demonstrated

### 1. REST API Idempotency

- Client sends `idempotencyKey` with POST requests
- Service hashes request body and stores with key
- Duplicate key + same hash → return cached response (safe retry)
- Duplicate key + different hash → return 409 Conflict

### 2. Kafka Consumer Idempotency

- Events include `eventId` (UUID)
- Consumer checks `processed_events` table before processing
- Already processed → skip (prevents duplicate billing, etc.)

### 3. Schema Evolution (Backward Compatible)

- Avro field `source` is nullable with default
- Old consumers ignore new field (forward compatible)
- New consumers handle missing field (backward compatible)
- Schema Registry enforces compatibility

### 4. Error Handling

- All `ErrorResponse` includes `traceId` for correlation
- Standardized error codes: `VALIDATION_ERROR`, `NOT_FOUND`, `CONFLICT`, `INTERNAL_ERROR`
- Dead Letter Queue for poison messages

## 📚 API Documentation

- **Swagger UI**: http://localhost:8080/swagger-ui.html
- **OpenAPI Spec**: http://localhost:8080/v3/api-docs
- **Health Check**: http://localhost:8080/actuator/health
- **Info**: http://localhost:8080/actuator/info
- **Static Page**: http://localhost:8080/

## 🔧 Technology Stack

- **Java 21** (Stable LTS)
- **Spring Boot 3.4.2** (Web, Data JPA, Actuator, Kafka)
- **PostgreSQL 17** (Database)
- **Apache Kafka 7.8.1** (Event streaming)
- **Apache Avro 1.12.0** (Event serialization)
- **Confluent Schema Registry 7.8.1** (Schema governance)
- **Flyway** (Database migrations)
- **Lombok** (Boilerplate reduction)
- **springdoc-openapi** (Swagger UI)
- **TestContainers** (Integration testing)

## 📖 Further Reading

- [OpenAPI Specification](https://spec.openapis.org/oas/v3.1.0)
- [Apache Avro Documentation](https://avro.apache.org/docs/current/)
- [Flyway Migrations](https://flywaydb.org/documentation/)
- [Kafka Idempotent Producer](https://kafka.apache.org/documentation/#producerconfigs_enable.idempotence)
- [Schema Registry](https://docs.confluent.io/platform/current/schema-registry/index.html)

## 📄 License

This project is licensed under the Apache License 2.0 - see the [LICENSE](LICENSE) file for details.

## 👤 Author

**Wallace Espindola**

- Email: wallace.espindola@gmail.com
- LinkedIn: [wallaceespindola](https://www.linkedin.com/in/wallaceespindola/)
- GitHub: [@wallaceespindola](https://github.com/wallaceespindola)

## 🤝 Contributing

Contributions, issues, and feature requests are welcome!

## ⭐ Show your support

Give a ⭐️ if this project helped you understand contract-first development!
