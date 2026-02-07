# API Endpoints Reference

**Base URL:** `http://localhost:8080`
**Application:** Contract-First Integrations
**Version:** 1.0.0

---

## REST API Endpoints

### Orders API

Base path: `/v1/orders`

#### 1. Create Order

**Endpoint:** `POST /v1/orders`

**Description:** Creates a new order with idempotency support

**Request Headers:**
```
Content-Type: application/json
```

**Request Body:**
```json
{
  "customerId": "CUST-123",
  "idempotencyKey": "unique-key-001",  // Optional, for idempotency
  "items": [
    {
      "sku": "SKU-001",
      "quantity": 2
    }
  ]
}
```

**Responses:**

- **201 Created** - Order created successfully
  ```json
  {
    "orderId": "ORD-12A4B5C6",
    "customerId": "CUST-123",
    "status": "CREATED",
    "items": [
      {
        "sku": "SKU-001",
        "quantity": 2
      }
    ],
    "timestamp": "2026-02-07T12:00:00.000Z"
  }
  ```

- **400 Bad Request** - Invalid request (validation error)
  ```json
  {
    "code": "VALIDATION_ERROR",
    "message": "Validation failed",
    "details": {
      "customerId": "customerId is required"
    },
    "traceId": "abc-123-def",
    "timestamp": "2026-02-07T12:00:00.000Z"
  }
  ```

- **409 Conflict** - Idempotency key conflict (same key, different payload)
  ```json
  {
    "code": "CONFLICT",
    "message": "Idempotency key already used with a different request payload",
    "traceId": "abc-123-def",
    "timestamp": "2026-02-07T12:00:00.000Z"
  }
  ```

**cURL Example:**
```bash
curl -X POST http://localhost:8080/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-123",
    "idempotencyKey": "test-key-001",
    "items": [
      {"sku": "SKU-001", "quantity": 2}
    ]
  }'
```

---

#### 2. Get Order

**Endpoint:** `GET /v1/orders/{orderId}`

**Description:** Retrieves an order by its ID

**Path Parameters:**
- `orderId` (string, required) - The order ID

**Responses:**

- **200 OK** - Order found
  ```json
  {
    "orderId": "ORD-12A4B5C6",
    "customerId": "CUST-123",
    "status": "CREATED",
    "items": [
      {
        "sku": "SKU-001",
        "quantity": 2
      }
    ],
    "timestamp": "2026-02-07T12:00:00.000Z"
  }
  ```

- **404 Not Found** - Order not found
  ```json
  {
    "code": "NOT_FOUND",
    "message": "Order not found: ORD-99999",
    "traceId": "abc-123-def",
    "timestamp": "2026-02-07T12:00:00.000Z"
  }
  ```

**cURL Example:**
```bash
curl http://localhost:8080/v1/orders/ORD-12A4B5C6
```

---

## Actuator Endpoints

Spring Boot Actuator endpoints for monitoring and management.

Base path: `/actuator`

### 1. Health Endpoint

**Endpoint:** `GET /actuator/health`

**Description:** Shows application health status

**Response:**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 500000000000,
        "free": 250000000000,
        "threshold": 10485760,
        "path": "/app",
        "exists": true
      }
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

**cURL Example:**
```bash
curl http://localhost:8080/actuator/health
```

---

### 2. Info Endpoint

**Endpoint:** `GET /actuator/info`

**Description:** Displays application information

**Response:**
```json
{
  "app": {
    "name": "Contract-First Integrations",
    "version": "1.0.0",
    "description": "Reference implementation demonstrating contract-first patterns with REST API (OpenAPI), Kafka Events (Avro), and Database (Flyway)"
  },
  "author": {
    "name": "Wallace Espindola",
    "email": "wallace.espindola@gmail.com",
    "linkedin": "https://www.linkedin.com/in/wallaceespindola/",
    "github": "https://github.com/wallaceespindola"
  }
}
```

**cURL Example:**
```bash
curl http://localhost:8080/actuator/info
```

---

### 3. Metrics Endpoint

**Endpoint:** `GET /actuator/metrics`

**Description:** Shows available metrics

**Response:**
```json
{
  "names": [
    "jvm.memory.used",
    "jvm.memory.max",
    "http.server.requests",
    "system.cpu.usage",
    "process.uptime",
    ...
  ]
}
```

**Specific Metric:**

`GET /actuator/metrics/{metricName}`

**Example:**
```bash
curl http://localhost:8080/actuator/metrics/http.server.requests
```

---

## OpenAPI / Swagger Documentation

### 1. Swagger UI

**Endpoint:** `GET /swagger-ui.html`

**Description:** Interactive API documentation

**Features:**
- View all endpoints
- Try out API calls
- See request/response schemas
- Download OpenAPI spec

**Access:**
```
Open in browser: http://localhost:8080/swagger-ui.html
```

---

### 2. OpenAPI JSON Specification

**Endpoint:** `GET /v3/api-docs`

**Description:** OpenAPI 3.0 specification in JSON format

**Response:** Complete API specification

**cURL Example:**
```bash
curl http://localhost:8080/v3/api-docs
```

---

## Static Content

### Welcome Page

**Endpoint:** `GET /`

**Description:** Static welcome page (if exists in src/main/resources/static/)

**Access:**
```
http://localhost:8080/
```

---

## Error Responses

All errors follow a standard format:

```json
{
  "code": "ERROR_CODE",
  "message": "Human-readable error message",
  "details": {},  // Optional, for validation errors
  "traceId": "unique-trace-id",
  "timestamp": "2026-02-07T12:00:00.000Z"
}
```

### Error Codes

| Code | HTTP Status | Description |
|------|-------------|-------------|
| `VALIDATION_ERROR` | 400 | Request validation failed |
| `NOT_FOUND` | 404 | Resource not found |
| `CONFLICT` | 409 | Idempotency key conflict |
| `INTERNAL_ERROR` | 500 | Internal server error |

---

## Kafka Topics (Event Streaming)

While not HTTP endpoints, the application also produces/consumes Kafka events:

### Published Events

**Topic:** `orders.order-created.v1`

**Event Schema:** OrderCreated (Avro)
```json
{
  "eventId": "uuid",
  "occurredAt": "ISO-8601 timestamp",
  "orderId": "ORD-12A4B5C6",
  "customerId": "CUST-123",
  "source": null,
  "items": [
    {
      "sku": "SKU-001",
      "quantity": 2
    }
  ]
}
```

**Trigger:** Fired when a new order is created via POST /v1/orders

---

### Consumed Events

**Topic:** `orders.order-created.v1`

**Consumer Group:** `billing` (or other consumers)

**Purpose:** Process order events for billing, inventory, etc.

---

### Dead Letter Queue

**Topic:** `orders.dlq`

**Purpose:** Failed events that cannot be processed

---

## Testing Endpoints

### Quick Health Check

```bash
# Check if application is running
curl -f http://localhost:8080/actuator/health || echo "Application not running"
```

### Full Endpoint Test

```bash
# 1. Check health
curl http://localhost:8080/actuator/health

# 2. Check info
curl http://localhost:8080/actuator/info

# 3. Create order
ORDER_RESPONSE=$(curl -s -X POST http://localhost:8080/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-TEST",
    "idempotencyKey": "test-001",
    "items": [{"sku": "SKU-001", "quantity": 1}]
  }')

echo $ORDER_RESPONSE

# 4. Extract orderId
ORDER_ID=$(echo $ORDER_RESPONSE | grep -o '"orderId":"[^"]*"' | cut -d'"' -f4)

# 5. Get order
curl http://localhost:8080/v1/orders/$ORDER_ID

# 6. Test idempotency (same request, should return same order)
curl -X POST http://localhost:8080/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-TEST",
    "idempotencyKey": "test-001",
    "items": [{"sku": "SKU-001", "quantity": 1}]
  }'

# 7. Test conflict (same key, different payload, should return 409)
curl -X POST http://localhost:8080/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-DIFFERENT",
    "idempotencyKey": "test-001",
    "items": [{"sku": "SKU-999", "quantity": 99}]
  }'
```

---

## URL Summary

| Endpoint | Method | Purpose | Auth Required |
|----------|--------|---------|---------------|
| `/v1/orders` | POST | Create order | No |
| `/v1/orders/{orderId}` | GET | Get order | No |
| `/actuator/health` | GET | Health check | No |
| `/actuator/info` | GET | App info | No |
| `/actuator/metrics` | GET | Metrics | No |
| `/swagger-ui.html` | GET | API docs | No |
| `/v3/api-docs` | GET | OpenAPI spec | No |

**Total Endpoints:** 7 main endpoints + Swagger UI

---

## Port Configuration

- **Default:** 8080
- **Docker:** 8080 (mapped from container)
- **Change:** Set `server.port` in `application.yml`

---

## Environment-Specific URLs

### Local Development
```
Base URL: http://localhost:8080
```

### Docker Compose
```
Base URL: http://localhost:8080
```

### Production
```
Base URL: https://your-domain.com
```

(Update `SPRING_PROFILES_ACTIVE` environment variable)

---

## Schema Registry (for Avro schemas)

**URL:** `http://localhost:8081`

**Endpoints:**
- `GET /subjects` - List all schemas
- `GET /subjects/{subject}/versions` - List schema versions
- `GET /subjects/{subject}/versions/{version}` - Get specific schema

**Example:**
```bash
# List schemas
curl http://localhost:8081/subjects

# Get OrderCreated schema
curl http://localhost:8081/subjects/orders.order-created.v1-value/versions/1
```

---

## Notes

- All timestamps are in ISO-8601 format (UTC)
- All requests/responses use JSON format
- Idempotency keys are optional but recommended for POST requests
- All error responses include a `traceId` for correlation
- The application follows REST best practices (proper HTTP verbs, status codes)
- OpenAPI 3.0 specification available at `/v3/api-docs`
