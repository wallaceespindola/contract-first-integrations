#!/bin/bash
set -e

echo "=================================="
echo "Docker Compose Test Script"
echo "Contract-First Integrations"
echo "=================================="
echo ""

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Check if Docker is installed
if ! command -v docker &> /dev/null; then
    echo -e "${RED}ERROR: Docker is not installed${NC}"
    echo "Please install Docker Desktop from https://www.docker.com/products/docker-desktop"
    exit 1
fi

# Check if Docker is running
if ! docker info &> /dev/null; then
    echo -e "${RED}ERROR: Docker is not running${NC}"
    echo "Please start Docker Desktop"
    exit 1
fi

echo -e "${GREEN}✓ Docker is installed and running${NC}"
echo ""

# Clean up previous containers
echo "Cleaning up previous containers..."
docker compose down -v 2>/dev/null || true
echo -e "${GREEN}✓ Cleanup complete${NC}"
echo ""

# Build the application JAR
echo "Building application JAR..."
export JAVA_HOME=/Users/wallaceespindola/Library/Java/JavaVirtualMachines/corretto-21.0.10/Contents/Home
mvn clean package -DskipTests -q
if [ $? -eq 0 ]; then
    echo -e "${GREEN}✓ JAR built successfully${NC}"
    ls -lh target/contract-first-integrations-*.jar | grep -v original
else
    echo -e "${RED}✗ JAR build failed${NC}"
    exit 1
fi
echo ""

# Start the stack
echo "Starting Docker Compose stack..."
docker compose up -d --build
echo -e "${GREEN}✓ Containers starting${NC}"
echo ""

# Wait for services to be healthy
echo "Waiting for services to be healthy..."
sleep 5

services=("postgres" "kafka" "schema-registry" "app")
for service in "${services[@]}"; do
    echo -n "Checking $service... "
    max_attempts=30
    attempt=0

    while [ $attempt -lt $max_attempts ]; do
        if docker compose ps $service | grep -q "healthy"; then
            echo -e "${GREEN}✓ healthy${NC}"
            break
        elif docker compose ps $service | grep -q "running"; then
            echo -n "."
            sleep 2
            attempt=$((attempt + 1))
        else
            echo -e "${RED}✗ not running${NC}"
            docker compose logs $service
            exit 1
        fi
    done

    if [ $attempt -eq $max_attempts ]; then
        echo -e "${YELLOW}⚠ timeout (but still running)${NC}"
    fi
done
echo ""

# Test the application endpoints
echo "Testing application endpoints..."

# Health check
echo -n "Health endpoint: "
response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/health)
if [ "$response" -eq 200 ]; then
    echo -e "${GREEN}✓ 200 OK${NC}"
    curl -s http://localhost:8080/actuator/health | python3 -m json.tool 2>/dev/null || cat
else
    echo -e "${RED}✗ $response${NC}"
fi
echo ""

# Info endpoint
echo -n "Info endpoint: "
response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/actuator/info)
if [ "$response" -eq 200 ]; then
    echo -e "${GREEN}✓ 200 OK${NC}"
    curl -s http://localhost:8080/actuator/info | python3 -m json.tool 2>/dev/null || cat
else
    echo -e "${RED}✗ $response${NC}"
fi
echo ""

# Swagger UI
echo -n "Swagger UI: "
response=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/swagger-ui.html)
if [ "$response" -eq 200 ]; then
    echo -e "${GREEN}✓ 200 OK${NC}"
else
    echo -e "${RED}✗ $response${NC}"
fi
echo ""

# Create an order via REST API
echo "Creating an order..."
create_response=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8080/v1/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": "CUST-TEST-001",
    "idempotencyKey": "test-docker-key-001",
    "items": [
      {"sku": "SKU-DOCKER-001", "quantity": 5}
    ]
  }')

http_code=$(echo "$create_response" | tail -n 1)
response_body=$(echo "$create_response" | head -n -1)

if [ "$http_code" -eq 201 ]; then
    echo -e "${GREEN}✓ Order created (201)${NC}"
    echo "$response_body" | python3 -m json.tool 2>/dev/null || echo "$response_body"

    # Extract orderId
    order_id=$(echo "$response_body" | grep -o '"orderId":"[^"]*"' | cut -d'"' -f4)
    echo ""

    if [ -n "$order_id" ]; then
        # Get the order
        echo "Fetching order $order_id..."
        get_response=$(curl -s -w "\n%{http_code}" http://localhost:8080/v1/orders/$order_id)
        get_code=$(echo "$get_response" | tail -n 1)
        get_body=$(echo "$get_response" | head -n -1)

        if [ "$get_code" -eq 200 ]; then
            echo -e "${GREEN}✓ Order retrieved (200)${NC}"
            echo "$get_body" | python3 -m json.tool 2>/dev/null || echo "$get_body"
        else
            echo -e "${RED}✗ Failed to retrieve order ($get_code)${NC}"
        fi
        echo ""

        # Test idempotency - same request should return same order
        echo "Testing idempotency (duplicate request)..."
        idempotent_response=$(curl -s -w "\n%{http_code}" -X POST http://localhost:8080/v1/orders \
          -H "Content-Type: application/json" \
          -d '{
            "customerId": "CUST-TEST-001",
            "idempotencyKey": "test-docker-key-001",
            "items": [
              {"sku": "SKU-DOCKER-001", "quantity": 5}
            ]
          }')

        idempotent_code=$(echo "$idempotent_response" | tail -n 1)
        idempotent_body=$(echo "$idempotent_response" | head -n -1)
        returned_order_id=$(echo "$idempotent_body" | grep -o '"orderId":"[^"]*"' | cut -d'"' -f4)

        if [ "$idempotent_code" -eq 201 ] && [ "$returned_order_id" = "$order_id" ]; then
            echo -e "${GREEN}✓ Idempotency working - same orderId returned${NC}"
        else
            echo -e "${RED}✗ Idempotency failed - expected orderId $order_id, got $returned_order_id (code: $idempotent_code)${NC}"
        fi
    fi
else
    echo -e "${RED}✗ Failed to create order ($http_code)${NC}"
    echo "$response_body"
fi
echo ""

# Check Kafka topics
echo "Checking Kafka topics..."
docker compose exec kafka kafka-topics --list --bootstrap-server localhost:9092 2>/dev/null || echo "Could not list topics"
echo ""

# Check Schema Registry
echo "Checking Schema Registry..."
schemas=$(curl -s http://localhost:8081/subjects 2>/dev/null)
if [ -n "$schemas" ]; then
    echo -e "${GREEN}✓ Schema Registry accessible${NC}"
    echo "Registered schemas: $schemas"
else
    echo -e "${YELLOW}⚠ No schemas registered yet${NC}"
fi
echo ""

# Check PostgreSQL
echo "Checking PostgreSQL..."
docker compose exec postgres psql -U postgres -d orders -c "\dt" 2>/dev/null || echo "Could not query database"
echo ""

# Summary
echo "=================================="
echo "Test Summary"
echo "=================================="
echo ""
echo "Services running:"
docker compose ps
echo ""
echo "Logs can be viewed with: docker compose logs -f [service]"
echo "Stop with: docker compose down"
echo "Stop and remove volumes: docker compose down -v"
echo ""
echo -e "${GREEN}✓ Docker Compose test complete${NC}"
