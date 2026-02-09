# 🚀 Postman Quick Start Guide

Get up and running with the Contract-First Integrations API in 60 seconds!

## ⚡ Quick Setup (3 Steps)

### 1️⃣ Import Collection & Environment

```bash
# In Postman Desktop App:
1. Click "Import" button
2. Drag and drop these 2 files:
   - Contract-First-Integrations.postman_collection.json
   - Contract-First-Integrations.postman_environment.json
3. Done! ✅
```

### 2️⃣ Start the Application

```bash
# Terminal 1: Start the app
cd /path/to/contract-first-integrations
export JAVA_HOME=/path/to/java-21
mvn spring-boot:run

# Wait for: "Started Application in X seconds"
```

### 3️⃣ Run Tests

```bash
# In Postman:
1. Select environment: "Contract-First Integrations - Local"
2. Click collection → "Run" button
3. Watch tests pass! 🎉
```

## 📊 What You Get

✅ **15 API requests** ready to use
✅ **Automated tests** for each endpoint
✅ **Auto-generated** idempotency keys
✅ **Smart workflows** (create → retrieve)
✅ **Error handling** examples

## 🎯 Essential Requests

### Must-Try First:

```
1. Health Check         → Is the app running?
2. Create Order         → Make a new order
3. Get Order           → Retrieve that order
4. Idempotency Test    → Send same request twice
```

### All Folders:

| Folder | Requests | What It Tests |
|--------|----------|--------------|
| 📦 Orders API | 7 | CRUD + validation + idempotency |
| 💚 Actuator | 5 | Health, info, metrics |
| 📄 OpenAPI | 2 | API docs & Swagger UI |
| 🧪 Integration | 1 | End-to-end workflow |

## 🔧 Environment Variables

Everything auto-configures! But you can change:

```javascript
baseUrl = "http://localhost:8080"  // ← Change this for Docker/Production
```

## 🐛 Common Issues

### "Could not send request"
→ **Fix:** Make sure app is running on port 8080

```bash
curl http://localhost:8080/actuator/health
# Should return: {"status":"UP"}
```

### Tests are red
→ **Fix:** Check environment is selected (top-right dropdown)

### "orderId is undefined"
→ **Fix:** Run "Create Order - Success" first (it saves the orderId)

## 📖 Next Steps

- 📚 Full docs → `README.md` (in this folder)
- 🌐 API reference → `../docs/API_ENDPOINTS.md`
- 🧪 Test examples → `../src/test/java/`

## 💡 Pro Tips

### Run Specific Folder
Right-click folder → "Run" → Only those requests execute

### View Console Output
Bottom-left → "Console" → See all request/response details

### Generate HTML Report
Use Newman CLI:
```bash
npm install -g newman
newman run Contract-First-Integrations.postman_collection.json \
  --environment Contract-First-Integrations.postman_environment.json \
  --reporters html \
  --reporter-html-export report.html
```

### Test Different Environments
Duplicate environment → Change `baseUrl`:
- Local: `http://localhost:8080`
- Docker: `http://localhost:8080`
- Staging: `https://staging.example.com`
- Production: `https://api.example.com`

## 🎓 Example Workflow

**Goal:** Create an order and verify it exists

```
Step 1: Click "Create Order - Success" → Send
Result: 201 Created, orderId = "ORD-ABC123" (saved automatically)

Step 2: Click "Get Order - Success" → Send
Result: 200 OK, returns order with orderId = "ORD-ABC123"

Step 3: Click "Create Order - Idempotency (Same Request)" → Send
Result: 200 OK, same orderId returned (idempotency working!)
```

**That's it!** You've tested:
- ✅ Order creation
- ✅ Order retrieval
- ✅ Idempotency protection

## 📞 Need Help?

- 📖 Read full README: `README.md`
- 🔍 Check API docs: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- 💬 Issues: [GitHub Issues](https://github.com/wallaceespindola/contract-first-integrations/issues)

---

**Quick Start Version:** 1.0.0
**Last Updated:** 2026-02-09
**Author:** Wallace Espindola
