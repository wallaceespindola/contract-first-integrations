# Postman Collection - Contract-First Integrations API

This directory contains the Postman collection and environment for testing the Contract-First Integrations REST API.

## 📦 Files

- **`Contract-First-Integrations.postman_collection.json`** - Complete API collection with all endpoints and tests
- **`Contract-First-Integrations.postman_environment.json`** - Environment variables for local development
- **`README.md`** - This file

## 🚀 Quick Start

### 1. Import into Postman

#### Option A: Using Postman Desktop App
1. Open Postman
2. Click **Import** button (top left)
3. Drag and drop both JSON files or click **Upload Files**
4. Select both files:
   - `Contract-First-Integrations.postman_collection.json`
   - `Contract-First-Integrations.postman_environment.json`
5. Click **Import**

#### Option B: Using Postman CLI
```bash
# Import collection
postman collection import Contract-First-Integrations.postman_collection.json

# Import environment
postman environment import Contract-First-Integrations.postman_environment.json
```

### 2. Set Environment

1. In Postman, click the environment dropdown (top right)
2. Select **Contract-First Integrations - Local**
3. Verify `baseUrl` is set to [http://localhost:8080](http://localhost:8080)

### 3. Start the Application

```bash
# Make sure the application is running
cd /path/to/contract-first-integrations

# Using Maven
export JAVA_HOME=/path/to/java-21
mvn spring-boot:run

# Or using Docker
docker-compose up
```

### 4. Run the Collection

1. Select the collection in Postman
2. Click **Run** button (or press ⌘R / Ctrl+R)
3. Click **Run Contract-First Integrations API**
4. Watch the tests execute!

## 📋 Collection Structure

### 1. Orders API (7 requests)
- ✅ **Create Order - Success** - Creates a new order with idempotency key
- 🔄 **Create Order - Idempotency (Same Request)** - Tests idempotency (returns same order)
- ⚠️ **Create Order - Idempotency Conflict** - Tests conflict (same key, different payload)
- ❌ **Create Order - Validation Error (Missing CustomerId)** - Tests validation
- ❌ **Create Order - Validation Error (Empty Items)** - Tests validation
- ✅ **Get Order - Success** - Retrieves order by ID
- 🔍 **Get Order - Not Found** - Tests 404 response

### 2. Actuator Endpoints (5 requests)
- 💚 **Health Check** - Application health status
- ℹ️ **Application Info** - App metadata
- 📊 **Metrics - List All** - All available metrics
- 📈 **Metrics - HTTP Server Requests** - HTTP request metrics
- 💾 **Metrics - JVM Memory Used** - JVM memory metrics

### 3. OpenAPI Documentation (2 requests)
- 📄 **OpenAPI JSON Specification** - OpenAPI 3.0 spec
- 🌐 **Swagger UI (HTML)** - Interactive API documentation

### 4. Integration Tests (1 request)
- 🧪 **Full Workflow Test** - End-to-end test (health → create → retrieve)

**Total: 15 requests with automated tests**

## 🧪 Automated Tests

Each request includes automated tests that verify:

### Response Status Codes
- ✅ 200 OK for successful GET requests
- ✅ 201 Created for successful POST requests
- ✅ 400 Bad Request for validation errors
- ✅ 404 Not Found for non-existent resources
- ✅ 409 Conflict for idempotency violations

### Response Structure
- Required fields are present (`orderId`, `customerId`, `status`, etc.)
- Data types are correct
- Timestamps are in ISO-8601 format

### Business Logic
- Idempotency works correctly (same key returns same order)
- Idempotency conflicts are detected (same key, different payload)
- Validation errors provide meaningful messages
- Order IDs can be retrieved after creation

### Variables & State
- `orderId` is automatically saved from Create Order response
- `idempotencyKey` is auto-generated per request
- Integration tests chain requests (create → retrieve)

## 🔧 Environment Variables

The environment includes these variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `baseUrl` | API base URL | `http://localhost:8080` |
| `orderId` | Saved from Create Order response | (auto-set) |
| `idempotencyKey` | Auto-generated unique key | (auto-set) |
| `integrationTestKey` | Key for integration tests | (auto-set) |
| `integrationTestOrderId` | Order ID from integration test | (auto-set) |

### Customizing the Environment

To test against different environments:

1. Duplicate the environment
2. Rename it (e.g., "Contract-First Integrations - Docker", "Contract-First Integrations - Production")
3. Update the `baseUrl`:
   - **Docker**: `http://localhost:8080`
   - **Production**: `https://your-domain.com`

## 📝 Example Workflow

### Recommended Testing Order:

1. **Health Check** - Verify application is running
2. **Application Info** - Check application details
3. **Create Order - Success** - Create a new order (saves `orderId`)
4. **Get Order - Success** - Retrieve the created order
5. **Create Order - Idempotency (Same Request)** - Verify idempotency
6. **Create Order - Idempotency Conflict** - Test conflict handling
7. **Create Order - Validation Errors** - Test validation
8. **Get Order - Not Found** - Test 404 handling
9. **Metrics & OpenAPI** - Explore monitoring and documentation

### Running Individual Requests

Click any request and click **Send** to execute it individually.

### Running the Entire Collection

1. Click the collection name
2. Click **Run**
3. Select all requests or specific folders
4. Click **Run Contract-First Integrations API**
5. View test results in real-time

## 🔍 Understanding Test Results

### ✅ Green Tests = Passing
All assertions passed, endpoint behaves as expected.

### ❌ Red Tests = Failing
One or more assertions failed. Check:
1. Is the application running?
2. Is the `baseUrl` correct in the environment?
3. Is the database accessible?
4. Check Postman Console for detailed error messages

### Test Script Console

View console output:
1. Click **Console** (bottom left in Postman)
2. See detailed request/response data
3. See `console.log()` messages from test scripts

## 📊 Collection Runner

Run the entire collection and generate reports:

1. Click **Run** on the collection
2. Select all folders
3. Set iterations (default: 1)
4. Click **Run Contract-First Integrations API**
5. View summary report:
   - Total requests executed
   - Tests passed/failed
   - Response times
   - Response sizes

## 🔗 Integration with CI/CD

### Using Newman (Postman CLI)

Install Newman:
```bash
npm install -g newman
```

Run the collection:
```bash
newman run Contract-First-Integrations.postman_collection.json \
  --environment Contract-First-Integrations.postman_environment.json \
  --reporters cli,json,html \
  --reporter-html-export newman-report.html
```

### GitHub Actions Example

```yaml
name: API Tests

on: [push, pull_request]

jobs:
  api-tests:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Start application
        run: docker-compose up -d

      - name: Wait for application
        run: |
          until curl -f http://localhost:8080/actuator/health; do
            sleep 2
          done

      - name: Install Newman
        run: npm install -g newman

      - name: Run Postman tests
        run: |
          newman run postman/Contract-First-Integrations.postman_collection.json \
            --environment postman/Contract-First-Integrations.postman_environment.json \
            --reporters cli,junit \
            --reporter-junit-export results.xml

      - name: Publish test results
        uses: EnricoMi/publish-unit-test-result-action@v2
        if: always()
        with:
          files: results.xml
```

## 🐛 Troubleshooting

### Connection Error: "Could not send request"

**Problem:** Cannot connect to [http://localhost:8080](http://localhost:8080)

**Solutions:**
1. Verify application is running: `curl http://localhost:8080/actuator/health`
2. Check correct port: `server.port` in `application.yml`
3. Check Docker container status: `docker-compose ps`
4. Verify `baseUrl` in environment matches your setup

### Test Failure: "Status code is not 201"

**Problem:** Expected 201, got 500 (Internal Server Error)

**Solutions:**
1. Check application logs for stack traces
2. Verify database is running and accessible
3. Check schema migrations ran successfully (Flyway)
4. Verify Kafka is running (if testing event publishing)

### Test Failure: "Cannot read property 'orderId' of undefined"

**Problem:** Response body is empty or malformed

**Solutions:**
1. Check response in Postman Console
2. Verify request body is valid JSON
3. Check application logs for validation errors
4. Verify Content-Type header is `application/json`

### Idempotency Tests Failing

**Problem:** Same key returns different order ID

**Solutions:**
1. Verify idempotency service is working (check logs)
2. Check database persistence
3. Ensure `idempotencyKey` variable is being reused correctly

## 📚 Additional Resources

### Documentation
- **API Endpoints Reference**: `../docs/API_ENDPOINTS.md`
- **OpenAPI Spec**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)
- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

### Contract Specifications
- **OpenAPI Spec**: `../contracts/openapi/orders-api.v1.yaml`
- **Avro Schemas**: `../contracts/events/avro/`
- **Database Migrations**: `../src/main/resources/db/migration/`

### Testing
- **Java Tests**: `../src/test/java/`
- **Docker Testing**: `../docs/DOCKER_TESTING.md`
- **Test Results**: `../docs/TEST_RESULTS.md`

## 🤝 Contributing

When adding new endpoints:

1. Add request to appropriate folder in collection
2. Include automated tests (status code, structure, values)
3. Add pre-request scripts for dynamic data
4. Update this README with new request details
5. Test thoroughly before committing

## 👤 Author

- **Name:** Wallace Espindola - Software Engineer Sr. / Solution Architect / Java & Python Dev
- **Contact:** [wallace.espindola@gmail.com](mailto:wallace.espindola@gmail.com)
- **LinkedIn:** [linkedin.com/in/wallaceespindola](https://www.linkedin.com/in/wallaceespindola/)
- **GitHub:** [github.com/wallaceespindola](https://github.com/wallaceespindola/)
- **Speaker Deck:** [speakerdeck.com/wallacese](https://speakerdeck.com/wallacese)

## 📄 License

Apache License 2.0 - See [LICENSE](../LICENSE) for details

---

**Version:** 1.0.0
**Last Updated:** 2026-02-09
**Postman Collection Version:** v2.1.0
