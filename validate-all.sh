#!/bin/bash
set -e

echo "=========================================="
echo "Contract-First Integrations"
echo "Comprehensive Validation Script"
echo "=========================================="
echo ""

# Colors
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

FAILED=0
PASSED=0

check_pass() {
    echo -e "${GREEN}✓ $1${NC}"
    ((PASSED++))
}

check_fail() {
    echo -e "${RED}✗ $1${NC}"
    ((FAILED++))
}

check_warn() {
    echo -e "${YELLOW}⚠ $1${NC}"
}

check_info() {
    echo -e "${BLUE}ℹ $1${NC}"
}

echo "=== 1. Environment Check ==="
echo ""

# Java version
if [ -n "$JAVA_HOME" ]; then
    JAVA_VERSION=$($JAVA_HOME/bin/java -version 2>&1 | head -1 | cut -d'"' -f2)
    if [[ $JAVA_VERSION == 21.* ]]; then
        check_pass "Java 21 detected: $JAVA_VERSION"
    else
        check_fail "Java version mismatch: $JAVA_VERSION (expected 21.x)"
    fi
else
    check_fail "JAVA_HOME not set"
fi

# Maven
if command -v mvn &> /dev/null; then
    MVN_VERSION=$(mvn -version 2>&1 | head -1 | cut -d' ' -f3)
    check_pass "Maven installed: $MVN_VERSION"
else
    check_fail "Maven not installed"
fi

echo ""
echo "=== 2. Unit Tests ==="
echo ""

export JAVA_HOME=/Users/wallaceespindola/Library/Java/JavaVirtualMachines/corretto-21.0.10/Contents/Home

TEST_OUTPUT=$(mvn test -q 2>&1 | grep "Tests run:")
if echo "$TEST_OUTPUT" | grep -q "Failures: 0, Errors: 0"; then
    check_pass "All unit tests passing: $TEST_OUTPUT"
else
    check_fail "Some tests failing: $TEST_OUTPUT"
fi

echo ""
echo "=== 3. Build Validation ==="
echo ""

if mvn clean package -DskipTests -q; then
    JAR_FILE="target/contract-first-integrations-1.0.0.jar"
    if [ -f "$JAR_FILE" ]; then
        JAR_SIZE=$(ls -lh "$JAR_FILE" | awk '{print $5}')
        check_pass "JAR built successfully: $JAR_SIZE"
    else
        check_fail "JAR file not found"
    fi
else
    check_fail "Build failed"
fi

echo ""
echo "=== 4. Configuration Files ==="
echo ""

# Check critical files
files=(
    "pom.xml:Maven configuration"
    "Dockerfile:Docker build config"
    "docker-compose.yml:Docker Compose stack"
    "src/main/resources/application.yml:Application config"
    "test-docker.sh:Docker test script"
)

for file_desc in "${files[@]}"; do
    IFS=':' read -r file desc <<< "$file_desc"
    if [ -f "$file" ]; then
        check_pass "$desc exists ($file)"
    else
        check_fail "$desc missing ($file)"
    fi
done

echo ""
echo "=== 5. Docker Configuration ==="
echo ""

# Dockerfile validation
if grep -q "FROM eclipse-temurin:21" Dockerfile; then
    check_pass "Dockerfile uses Java 21 base image"
else
    check_fail "Dockerfile missing Java 21 image"
fi

if grep -q "EXPOSE 8080" Dockerfile; then
    check_pass "Dockerfile exposes port 8080"
else
    check_fail "Dockerfile missing EXPOSE directive"
fi

# docker-compose.yml validation
SERVICES=(postgres zookeeper kafka schema-registry app)
for service in "${SERVICES[@]}"; do
    if grep -q "$service:" docker-compose.yml; then
        check_pass "Docker Compose: $service service defined"
    else
        check_fail "Docker Compose: $service service missing"
    fi
done

# Check health checks
if grep -q "healthcheck:" docker-compose.yml; then
    check_pass "Docker Compose: Health checks configured"
else
    check_warn "Docker Compose: No health checks found"
fi

echo ""
echo "=== 6. Application Endpoints Configuration ==="
echo ""

# Check controller mappings
if grep -q '@RequestMapping("/v1/orders")' src/main/java/com/example/contractfirst/controller/OrderController.java; then
    check_pass "OrderController: /v1/orders endpoint configured"
else
    check_fail "OrderController: Missing endpoint mapping"
fi

if grep -q '@PostMapping' src/main/java/com/example/contractfirst/controller/OrderController.java; then
    check_pass "OrderController: POST endpoint defined"
else
    check_fail "OrderController: POST endpoint missing"
fi

if grep -q '@GetMapping' src/main/java/com/example/contractfirst/controller/OrderController.java; then
    check_pass "OrderController: GET endpoint defined"
else
    check_fail "OrderController: GET endpoint missing"
fi

# Check application.yml
if grep -q "port: 8080" src/main/resources/application.yml; then
    check_pass "Application: Server port 8080 configured"
else
    check_fail "Application: Server port not configured"
fi

if grep -q "springdoc:" src/main/resources/application.yml; then
    check_pass "Application: Swagger UI configured"
else
    check_warn "Application: Swagger UI not configured"
fi

if grep -q "management:" src/main/resources/application.yml; then
    check_pass "Application: Actuator endpoints configured"
else
    check_fail "Application: Actuator not configured"
fi

echo ""
echo "=== 7. Expected URLs (When Running) ==="
echo ""

check_info "REST API Endpoints:"
echo "  - POST   http://localhost:8080/v1/orders"
echo "  - GET    http://localhost:8080/v1/orders/{orderId}"

check_info "Actuator Endpoints:"
echo "  - GET    http://localhost:8080/actuator/health"
echo "  - GET    http://localhost:8080/actuator/info"
echo "  - GET    http://localhost:8080/actuator/metrics"

check_info "Swagger UI:"
echo "  - GET    http://localhost:8080/swagger-ui.html"
echo "  - GET    http://localhost:8080/v3/api-docs"

echo ""
echo "=== 8. Contract Files ==="
echo ""

# OpenAPI contract
if [ -f "contracts/openapi/orders-api.v1.yaml" ]; then
    if grep -q "openapi: 3" contracts/openapi/orders-api.v1.yaml; then
        check_pass "OpenAPI contract: Valid v3 specification"
    else
        check_warn "OpenAPI contract: Version unknown"
    fi
else
    check_fail "OpenAPI contract missing"
fi

# Avro schema
if [ -f "contracts/events/avro/OrderCreated.v1.avsc" ]; then
    if grep -q '"type": "record"' contracts/events/avro/OrderCreated.v1.avsc; then
        check_pass "Avro schema: OrderCreated defined"
    else
        check_fail "Avro schema: Invalid format"
    fi
else
    check_fail "Avro schema missing"
fi

# Flyway migrations
if [ -d "src/main/resources/db/migration" ]; then
    MIGRATION_COUNT=$(ls -1 src/main/resources/db/migration/*.sql 2>/dev/null | wc -l)
    if [ $MIGRATION_COUNT -gt 0 ]; then
        check_pass "Database migrations: $MIGRATION_COUNT SQL files found"
    else
        check_fail "Database migrations: No SQL files"
    fi
else
    check_fail "Database migration directory missing"
fi

echo ""
echo "=== 9. Test Coverage ==="
echo ""

if [ -f "target/site/jacoco/index.html" ]; then
    check_pass "JaCoCo coverage report generated"
    echo "  Report: target/site/jacoco/index.html"
else
    check_warn "Coverage report not generated (run: make test-cov)"
fi

echo ""
echo "=== 10. Docker Availability ==="
echo ""

if command -v docker &> /dev/null; then
    DOCKER_VERSION=$(docker --version | cut -d' ' -f3 | tr -d ',')
    check_pass "Docker installed: $DOCKER_VERSION"

    if docker info &> /dev/null; then
        check_pass "Docker daemon running"
        check_info "Ready to test with: make test-docker"
    else
        check_warn "Docker daemon not running (start Docker Desktop)"
    fi
else
    check_warn "Docker not installed (endpoints can't be tested)"
    check_info "Install: brew install --cask docker"
fi

echo ""
echo "=========================================="
echo "Validation Summary"
echo "=========================================="
echo ""
echo -e "${GREEN}Passed: $PASSED${NC}"
if [ $FAILED -gt 0 ]; then
    echo -e "${RED}Failed: $FAILED${NC}"
fi
echo ""

if [ $FAILED -eq 0 ]; then
    echo -e "${GREEN}✓ All validations passed!${NC}"
    echo ""
    echo "Next steps:"
    echo "  1. Install Docker Desktop (if not installed)"
    echo "  2. Run: make test-docker"
    echo "  3. Test endpoints: see API_ENDPOINTS.md"
    exit 0
else
    echo -e "${RED}✗ Some validations failed${NC}"
    echo "Please fix the issues above before proceeding."
    exit 1
fi
