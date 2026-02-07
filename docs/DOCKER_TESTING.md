# Docker Compose Testing Guide

## Status Summary

### ✅ Completed Tests

1. **Unit Tests** - All 16 tests passing
   - OrderControllerTest: 5 tests ✓
   - OrderServiceTest: 5 tests ✓
   - IdempotencyServiceTest: 6 tests ✓

2. **Application Build**
   - JAR file: `target/contract-first-integrations-1.0.0.jar` (84MB) ✓
   - Build command: `mvn clean package -DskipTests` ✓
   - Java version: Corretto 21.0.10 ✓

3. **Configuration Verification**
   - Dockerfile syntax: Valid ✓
   - docker-compose.yml syntax: Valid ✓
   - Environment variables: Properly configured ✓
   - Health checks: Defined for all services ✓

4. **Code Quality**
   - Deprecation warnings: Fixed (@MockBean → @MockitoBean) ✓
   - Test coverage: 42% overall, 90%+ on core business logic ✓

### ⏳ Pending Tests (Requires Docker Installation)

Docker is **not installed** on this system. The following tests need to be executed once Docker Desktop is installed:

1. **Docker Compose Stack Startup**
2. **Service Health Checks**
3. **REST API Integration Tests**
4. **Kafka Event Publishing**
5. **Database Migrations**
6. **Schema Registry**

---

## Prerequisites

### Install Docker Desktop

**macOS:**
```bash
# Download from official site
open https://www.docker.com/products/docker-desktop

# Or install via Homebrew
brew install --cask docker
```

After installation, start Docker Desktop from Applications.

### Verify Installation

```bash
docker --version
docker compose version
docker info
```

---

## Running Docker Compose Tests

### Option 1: Automated Test Script

Run the comprehensive test script:

```bash
./test-docker.sh
```

This script will:
- ✓ Check Docker is installed and running
- ✓ Clean up previous containers
- ✓ Build the application JAR
- ✓ Start all services (Postgres, Zookeeper, Kafka, Schema Registry, App)
- ✓ Wait for services to become healthy
- ✓ Test health and info endpoints
- ✓ Test Swagger UI
- ✓ Create an order via REST API
- ✓ Retrieve the order
- ✓ Test idempotency (duplicate request)
- ✓ Check Kafka topics
- ✓ Verify Schema Registry
- ✓ Query PostgreSQL tables

### Option 2: Manual Testing

**1. Build and Start Services**

```bash
# Build JAR (with Java 21)
export JAVA_HOME=/Users/wallaceespindola/Library/Java/JavaVirtualMachines/corretto-21.0.10/Contents/Home
mvn clean package -DskipTests

# Start all services
docker compose up -d --build

# Check status
docker compose ps
```

**2. Wait for Services to be Healthy**

```bash
# Watch logs
docker compose logs -f app

# Check health status
docker compose ps
```

All services should show `healthy` status:
- postgres
- kafka
- schema-registry
- app

**3. Test Application Endpoints**

```bash
# Health check
curl http://localhost:8080/actuator/health | jq

# Info endpoint
curl http://localhost:8080/actuator/info | jq

# Swagger UI (open in browser)
open http://localhost:8080/swagger-ui.html
```

**4. Test REST API**

```bash
# Create an order
curl -X POST http://localhost:8080/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-123",
    "idempotencyKey": "test-key-001",
    "items": [
      {"sku": "SKU-001", "quantity": 2}
    ]
  }' | jq

# Get order (replace ORDER_ID with actual ID from response)
curl http://localhost:8080/v1/orders/ORDER_ID | jq

# Test idempotency - same request should return same order
curl -X POST http://localhost:8080/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-123",
    "idempotencyKey": "test-key-001",
    "items": [
      {"sku": "SKU-001", "quantity": 2}
    ]
  }' | jq

# Test idempotency conflict - same key, different payload (should return 409)
curl -X POST http://localhost:8080/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-999",
    "idempotencyKey": "test-key-001",
    "items": [
      {"sku": "SKU-999", "quantity": 10}
    ]
  }' | jq
```

**5. Verify Kafka Integration**

```bash
# List Kafka topics
docker compose exec kafka kafka-topics \
  --list \
  --bootstrap-server localhost:9092

# Consume events from beginning
docker compose exec kafka kafka-console-consumer \
  --topic orders.order-created.v1 \
  --from-beginning \
  --bootstrap-server localhost:9092

# Check Schema Registry
curl http://localhost:8081/subjects | jq

# Get schema for OrderCreated
curl http://localhost:8081/subjects/orders.order-created.v1-value/versions | jq
```

**6. Verify Database**

```bash
# Connect to PostgreSQL
docker compose exec postgres psql -U postgres -d orders

# Run SQL queries
\dt                           # List tables
SELECT * FROM orders;         # View orders
SELECT * FROM order_items;    # View order items
SELECT * FROM idempotency_keys;  # View idempotency keys
SELECT * FROM flyway_schema_history;  # View migrations
\q                            # Exit
```

**7. View Logs**

```bash
# All services
docker compose logs -f

# Specific service
docker compose logs -f app
docker compose logs -f kafka
docker compose logs -f postgres
```

**8. Stop Services**

```bash
# Stop but keep data
docker compose down

# Stop and remove all volumes (clean slate)
docker compose down -v
```

---

## Expected Results

### Service Health Checks

All services should be `healthy`:

```
NAME                              STATUS       PORTS
contract-first-app                Up (healthy) 0.0.0.0:8080->8080/tcp
contract-first-kafka              Up (healthy) 0.0.0.0:9092-9093->9092-9093/tcp
contract-first-postgres           Up (healthy) 0.0.0.0:5432->5432/tcp
contract-first-schema-registry    Up (healthy) 0.0.0.0:8081->8081/tcp
contract-first-zookeeper          Up           0.0.0.0:2181->2181/tcp
```

### Health Endpoint Response

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
      "status": "UP"
    },
    "ping": {
      "status": "UP"
    }
  }
}
```

### Info Endpoint Response

```json
{
  "app": {
    "name": "Contract-First Integrations",
    "version": "1.0.0",
    "description": "Reference implementation demonstrating contract-first patterns..."
  },
  "author": {
    "name": "Wallace Espindola",
    "email": "wallace.espindola@gmail.com",
    "linkedin": "https://www.linkedin.com/in/wallaceespindola/",
    "github": "https://github.com/wallaceespindola"
  }
}
```

### Create Order Response (201)

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
  "timestamp": "2026-02-07T00:00:00.000Z"
}
```

### Kafka Topics

Expected topics:
- `orders.order-created.v1` - Order creation events
- `orders.dlq` - Dead letter queue

### Database Tables

Expected tables:
- `orders` - Order entities
- `order_items` - Order item entities
- `idempotency_keys` - Idempotency tracking
- `flyway_schema_history` - Migration history

### Schema Registry

Expected schemas:
- `orders.order-created.v1-value` - OrderCreated Avro schema

---

## Troubleshooting

### Port Already in Use

If ports 5432, 9092, or 8080 are in use:

```bash
# Find process using port
lsof -i :8080
lsof -i :5432
lsof -i :9092

# Kill process
kill -9 <PID>

# Or change ports in docker-compose.yml
```

### Container Won't Start

```bash
# Check logs
docker compose logs <service-name>

# Remove all containers and volumes
docker compose down -v

# Rebuild from scratch
docker compose build --no-cache
docker compose up -d
```

### Kafka Connection Issues

```bash
# Ensure Kafka is healthy before app starts
docker compose up -d postgres zookeeper kafka schema-registry
docker compose logs -f kafka

# Wait for "Kafka Server started" message
# Then start app
docker compose up -d app
```

### Database Migration Fails

```bash
# Check Flyway logs
docker compose logs app | grep -i flyway

# Connect to database and check manually
docker compose exec postgres psql -U postgres -d orders
SELECT * FROM flyway_schema_history;
```

---

## Configuration Details

### Docker Compose Services

| Service | Image | Port | Purpose |
|---------|-------|------|---------|
| postgres | postgres:17-alpine | 5432 | Database |
| zookeeper | confluentinc/cp-zookeeper:7.8.1 | 2181 | Kafka coordination |
| kafka | confluentinc/cp-kafka:7.8.1 | 9092, 9093 | Event streaming |
| schema-registry | confluentinc/cp-schema-registry:7.8.1 | 8081 | Avro schema management |
| app | Built from Dockerfile | 8080 | Spring Boot application |

### Environment Variables Override

The application uses these environment variables in Docker:

```yaml
SPRING_PROFILES_ACTIVE: dev
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/orders
SPRING_DATASOURCE_USERNAME: postgres
SPRING_DATASOURCE_PASSWORD: postgres
SPRING_KAFKA_BOOTSTRAP_SERVERS: kafka:9093
SPRING_KAFKA_PROPERTIES_SCHEMA_REGISTRY_URL: http://schema-registry:8081
```

These override the default values in `application.yml`.

---

## Next Steps After Docker Testing

Once Docker Compose tests pass:

1. ✓ Verify all services start correctly
2. ✓ Confirm REST API endpoints work
3. ✓ Validate Kafka events are published
4. ✓ Check database migrations execute
5. ✓ Test idempotency patterns
6. ✓ Review Schema Registry integration

Then proceed to:

- Integration tests with TestContainers
- Load testing with k6 or Gatling
- CI/CD pipeline setup
- Deployment to production environment

---

## References

- [Docker Compose Documentation](https://docs.docker.com/compose/)
- [Spring Boot Docker Guide](https://spring.io/guides/gs/spring-boot-docker/)
- [Confluent Platform Docker](https://docs.confluent.io/platform/current/platform-quickstart.html#docker)
- [PostgreSQL Docker Hub](https://hub.docker.com/_/postgres)
