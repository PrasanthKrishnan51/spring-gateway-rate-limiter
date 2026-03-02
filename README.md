# 🚀 Spring Boot — API Gateway + Token Bucket rate limiting

Multi-module Maven project with Spring Cloud Gateway (Token Bucket rate limiting via Redis) fronting a Product CRUD microservice backed by MongoDB.

---

## 🏛 Architecture

```
                        ┌─────────────────────────────────────────┐
                        │           API Gateway  :8080            │
  Client ──────────────►│                                         │
                        │  ┌──────────────────────────────────┐   │
                        │  │     Token Bucket Rate Limiter    │   │
                        │  │  (Spring Cloud Gateway + Redis)  │   │
                        │  │                                  │   │
                        │  │ GET  → 10 req/s  burst 20        │   │
                        │  │ POST/PUT/DELETE → 5 req/s burst10│   │
                        │  └──────────────┬───────────────────┘   │
                        │                 │  passes through       │
                        └─────────────────┼───────────────────────┘
                                          │
                                          ▼
                        ┌─────────────────────────────────────────┐
                        │       Product Service  :8081            │
                        │                                         │
                        │  Controller → Service → Repository      │
                        │         ↕ Redis Cache (10min TTL)       │
                        │         ↕ MongoDB                       │
                        └─────────────────────────────────────────┘

  Redis :6379  ─── shared by Gateway (rate limit buckets) + Product Service (response cache)
  MongoDB :27017
```

---

## 📁 Project Structure

```
springboot-gateway-project/
├── pom.xml                          ← Parent POM (multi-module)
├── docker-compose.yml               ← Full stack: MongoDB + Redis + both services
│
├── api-gateway/                     ← Spring Cloud Gateway module
│   ├── pom.xml
│   └── src/main/java/com/example/gateway/
│       ├── GatewayApplication.java
│       ├── config/
│       │   └── RateLimiterConfig.java      ← Token bucket beans (replenish, burst rates)
│       │   
│       ├── filter/
│       │   ├── RequestLoggingFilter.java   ← Global request/response logger
│       │   └── RateLimitResponseFilter.java ← Enriches 429 with Retry-After headers
│       └── ratelimit/
│           ├── RateLimitInfo.java          ← DTO record
│           └── RateLimitStatusController.java ← /gateway/rate-limit/status endpoint
│
└── product-service/                 ← Product CRUD microservice module
    ├── pom.xml
    └── src/main/java/com/example/app/
        ├── Application.java
        ├── config/    (RedisConfig, OpenApiConfig)
        ├── controller/ (ProductController)
        ├── dto/        (ProductDto — records)
        ├── exception/  (ProductException, GlobalExceptionHandler)
        ├── model/      (Product — MongoDB document)
        ├── repository/ (ProductRepository)
        └── service/    (ProductService — @Cacheable/@CacheEvict)
```

---

## 🚀 Quick Start

### Docker (recommended — starts everything)
```bash
docker-compose up -d
```

### Local Development
```bash
# Start infra
docker run -d -p 27017:27017 mongo:7.0
docker run -d -p 6379:6379 redis:7.2-alpine

# Terminal 1 — Product Service (port 8081)
cd product-service
mvn spring-boot:run

# Terminal 2 — API Gateway (port 8080)
cd api-gateway
mvn spring-boot:run
```

### Build all modules
```bash
mvn clean package -DskipTests
```

### Run all tests
```bash
mvn test
```

---

## 🌐 API via Gateway (:8080)

All requests go through the gateway on **port 8080**.

| Method | Endpoint | Rate Limit | Description |
|--------|----------|------------|-------------|
| GET | `/api/v1/products` | 10/s burst 20 | Get all products |
| GET | `/api/v1/products/{id}` | 10/s burst 20 | Get by ID |
| GET | `/api/v1/products/category/{cat}` | 10/s burst 20 | Filter by category |
| GET | `/api/v1/products/in-stock` | 10/s burst 20 | In-stock products |
| GET | `/api/v1/products/search?name=` | 10/s burst 20 | Search by name |
| POST | `/api/v1/products` | 5/s burst 10 | Create product |
| PUT | `/api/v1/products/{id}` | 5/s burst 10 | Update product |
| DELETE | `/api/v1/products/{id}` | 5/s burst 10 | Delete product |

---

## 🪣 Token Bucket Rate Limiting

Spring Cloud Gateway's `RedisRateLimiter` implements the **Token Bucket** algorithm:

```
┌──────────────────────────────────────────────────────┐
│  TOKEN BUCKET                                        │
│                                                      │
│  replenishRate   → tokens added per second           │
│  burstCapacity   → max tokens the bucket can hold    │
│  requestedTokens → tokens consumed per request (=1)  │
│                                                      │
│  Read routes:  replenish=10  burst=20                │
│  Write routes: replenish=5   burst=10                │
└──────────────────────────────────────────────────────┘
```

**Key resolver strategies:**
- Read endpoints → keyed by `X-API-Key` header (falls back to IP)
- Write endpoints → keyed by client IP address

**Redis keys created per client:**
```
request_rate_limiter.{key}.tokens      ← current token count
request_rate_limiter.{key}.timestamp   ← last refill time
```

**Response when rate limited (429):**
```
HTTP/1.1 429 Too Many Requests
X-RateLimit-Retry-After: 1
X-RateLimit-Message: Too many requests. Token bucket exhausted. Retry after 1 second.
```

---

## 🔍 Rate Limit Management Endpoints

```bash
# Check token bucket status for your IP
curl "http://localhost:8080/gateway/rate-limit/status?key=127.0.0.1"

# Check by API key
curl "http://localhost:8080/gateway/rate-limit/status?key=api-key:my-key-123"

# Reset bucket (useful for testing)
curl -X DELETE "http://localhost:8080/gateway/rate-limit/reset?key=127.0.0.1"
```

---

## 🔬 Test Rate Limiting Manually

```bash
# Fire 25 rapid GET requests — first 20 pass, rest get 429
for i in $(seq 1 25); do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/v1/products)
  echo "Request $i: $STATUS"
done

# Test write throttling — fire 12 POSTs rapidly (first 10 pass, rest get 429)
for i in $(seq 1 12); do
  STATUS=$(curl -s -o /dev/null -w "%{http_code}" -X POST http://localhost:8080/api/v1/products \
    -H "Content-Type: application/json" \
    -d '{"name":"Test","sku":"SKU-'$i'","price":9.99,"category":"Test","stock":1}')
  echo "POST $i: $STATUS"
done

# Test per-client isolation using X-API-Key header
curl -H "X-API-Key: client-A" http://localhost:8080/api/v1/products
curl -H "X-API-Key: client-B" http://localhost:8080/api/v1/products
# Each client has its own bucket — one exhausted doesn't affect the other
```

---

## 📊 Useful Endpoints Summary

| URL | Description |
|-----|-------------|
| `http://localhost:8080/api/v1/products` | Products API (via gateway) |
| `http://localhost:8080/gateway/rate-limit/status` | Token bucket status |
| `http://localhost:8080/actuator/gateway/routes` | View all gateway routes |
| `http://localhost:8080/actuator/health` | Gateway health |
| `http://localhost:8081/swagger-ui.html` | Swagger UI (direct to service) |
| `http://localhost:8081/actuator/health` | Product service health |
| `http://localhost:8080/service/actuator/health` | Product service health (via gateway) |

---

## ⚙️ Configuration Reference

### Rate Limit Tuning (`api-gateway/src/main/java/.../config/RateLimiterConfig.java`)
```java
// Read: 10 req/s sustained, burst up to 20
new RedisRateLimiter(10, 20, 1)

// Write: 5 req/s sustained, burst up to 10
new RedisRateLimiter(5, 10, 1)
```

### Environment Variables
| Variable | Default | Used By |
|----------|---------|---------|
| `REDIS_HOST` | `localhost` | Both services |
| `REDIS_PORT` | `6379` | Both services |
| `MONGO_URI` | `mongodb://localhost:27017/productstoredb` | Product Service |
| `PRODUCT_SERVICE_URL` | `http://localhost:8081` | API Gateway |
