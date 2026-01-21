# Job Queue System (Spring Boot + Postgres + Redis + WebSocket + React)

A multi-tenant background job processing system (DB-backed queue) with:
- Job submission + idempotency
- Distributed-safe workers (lease + SKIP LOCKED claim)
- Retries + Dead Letter Queue (DLQ)
- Per-tenant quotas + rate limiting (Redis)
- Live dashboard updates via WebSocket (STOMP)
- Simple React (Vite) admin dashboard
- Docker Compose to run everything

---

## ✅ Tech Stack

**Backend**
- Java 17 + Spring Boot
- Spring Web (REST APIs)
- Spring Data JPA + Flyway
- Postgres (jobs storage + queue)
- Redis (rate limit counters)
- Spring WebSocket (STOMP)
- Spring Actuator + Micrometer (metrics + health)

**Frontend**
- React + Vite
- STOMP WebSocket client (`@stomp/stompjs`)

**Infra**
- Docker Compose (Postgres + Redis + Backend + Frontend)

---

## ✅ Features

### Job Queue Core
- Job states: `PENDING → RUNNING → DONE`
- Failure path: `RUNNING → PENDING (retry)` and eventually `DLQ`

### Safety / Reliability
- Exactly-one worker claim using Postgres:
  `SELECT ... FOR UPDATE SKIP LOCKED`
- Lease mechanism:
  - worker sets `lease_until = now + 30s`
  - expired lease jobs become reclaimable

### Multi-Tenant Support
- Tenant identified using request header:
  `X-Tenant-Id: user1`
- Jobs are isolated per tenant in API + UI

### Rate Limits / Quotas (per tenant)
- Concurrency limit: max **5 RUNNING jobs** per tenant
- Rate limiting: max **10 job submissions / minute** per tenant (Redis)

### DLQ (Dead Letter Queue)
- After `maxAttempts` failures, job goes to `DLQ`
- DLQ jobs are visible in dashboard

### Live Dashboard
- WebSocket topics per tenant:
  - `/topic/tenant/{tenantId}/jobs`
  - `/topic/tenant/{tenantId}/summary`
- UI updates live without polling

---

## ✅ Project Structure

```
job-queue-system/
├── backend/              # Spring Boot
├── frontend/             # React Vite
├── docker-compose.yml
└── .env
```

---

## ✅ Setup & Run Locally

### 1) Clone the repo
```bash
git clone <YOUR_REPO_URL>
cd job-queue-system
```

### 2) Start everything with Docker
```bash
docker compose up --build
```

**✅ Services:**
- **Backend:** http://localhost:8080
- **Frontend:** http://localhost:5173
- **Postgres:** localhost:5432
- **Redis:** localhost:6379

### 3) Verify health endpoints
```bash
curl http://localhost:8080/actuator/health
curl http://localhost:8080/actuator/health/readiness
curl http://localhost:8080/actuator/health/liveness
```

---

## ✅ Backend REST APIs
### Submit Job
**POST** `/api/jobs`

**Headers:**
```
X-Tenant-Id: user1
```

**Body:**
```json
{
  "payload": { "type": "demo", "shouldFail": false },
  "idempotencyKey": "abc-123"
}
```

### Get Job By ID
**GET** `/api/jobs/{jobId}`

### List Jobs (paged)
**GET** `/api/jobs?page=0&size=20`

**Optional filter:**
```
GET /api/jobs?status=DONE&page=0&size=20
```

### DLQ Jobs (paged)
**GET** `/api/jobs?status=DLQ&page=0&size=20`

### Summary
**GET** `/api/jobs/summary`

---

## ✅ Demo Checklist (Full End-to-End)
**Copy paste these commands in order.**  
**Open frontend dashboard first:** http://localhost:5173

### ✅ Demo 1: Basic job success flow (PENDING → RUNNING → DONE)

**📌 Submit a successful job**
```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: user1" \
  -d '{"payload":{"type":"demo","shouldFail":false},"idempotencyKey":"success-1"}'
```

**📌 Observe dashboard live updates:**
- Pending increments
- Running increments
- Done increments

**📌 Confirm summary via REST**
```bash
curl -H "X-Tenant-Id: user1" http://localhost:8080/api/jobs/summary
```

### ✅ Demo 2: Retry + DLQ flow (RUNNING → PENDING → ... → DLQ)
**📌 Submit a job that always fails**
```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: user1" \
  -d '{"payload":{"type":"demo","shouldFail":true},"idempotencyKey":"fail-1"}'
```

**📌 Watch retries happen and finally DLQ in dashboard**

**📌 List DLQ jobs from API**
```bash
curl -H "X-Tenant-Id: user1" "http://localhost:8080/api/jobs?status=DLQ&page=0&size=20"
```

### ✅ Demo 3: Idempotency key deduplication
**📌 Submit same job twice with same idempotencyKey**
```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: user1" \
  -d '{"payload":{"type":"demo","shouldFail":false},"idempotencyKey":"idem-1"}'

curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: user1" \
  -d '{"payload":{"type":"demo","shouldFail":false},"idempotencyKey":"idem-1"}'
```

**📌 Verify the same jobId is returned and deduplicated=true**

### ✅ Demo 4: Rate limiting (10 jobs/minute per tenant)
**📌 Submit 11 jobs quickly (should block at 11)**
```bash
for i in {1..11}; do
  curl -s -X POST http://localhost:8080/api/jobs \
    -H "Content-Type: application/json" \
    -H "X-Tenant-Id: user1" \
    -d "{\"payload\":{\"type\":\"demo\",\"i\":$i},\"idempotencyKey\":\"rate-$i\"}" \
    && echo ""
done
```

**📌 Expect HTTP 429 RATE_LIMIT_EXCEEDED for the last one**

### ✅ Demo 5: Concurrency quota (max 5 RUNNING per tenant)
**📌 Submit many jobs for same tenant**
```bash
for i in {1..20}; do
  curl -s -X POST http://localhost:8080/api/jobs \
    -H "Content-Type: application/json" \
    -H "X-Tenant-Id: user1" \
    -d "{\"payload\":{\"type\":\"demo\"},\"idempotencyKey\":\"conc-$i\"}" \
    && echo ""
done
```

**📌 Observe:**
- At most 5 jobs show as RUNNING at any time
- Remaining jobs stay PENDING until slots free up

### ✅ Demo 6: Multi-tenant isolation
**📌 Submit jobs for tenant user2**
```bash
curl -X POST http://localhost:8080/api/jobs \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: user2" \
  -d '{"payload":{"type":"demo","shouldFail":false},"idempotencyKey":"u2-1"}'
```

**📌 In frontend, change tenant input to user2**

**📌 Confirm user2 sees their own summary/jobs only**

### ✅ Demo 7: Metrics + Health checks
**📌 Health endpoint**
```bash
curl http://localhost:8080/actuator/health
```

**📌 Metrics list**
```bash
curl http://localhost:8080/actuator/metrics
```

**📌 Check job submission count**
```bash
curl http://localhost:8080/actuator/metrics/jobs_submitted_total
```

---

## ✅ Design Choices & Tradeoffs
### 1) Queue implementation: Postgres table instead of Kafka/SQS

**✅ Pros:**
- Simple setup (only Postgres needed)
- Durable persistence
- Easy to inspect/debug via SQL
- Works well for assignment scale

**⚠️ Cons:**
- Higher DB load for large throughput systems
- Not as scalable as dedicated queue systems

### 2) Job claiming: FOR UPDATE SKIP LOCKED

**✅ Pros:**
- Correct distributed processing (multiple workers safe)
- No duplicate processing
- Standard Postgres queue pattern

**⚠️ Cons:**
- Requires careful indexing and transaction handling
- DB becomes bottleneck at very high throughput

### 3) Lease-based execution

**✅ Pros:**
- Jobs recover if worker crashes
- Avoids permanent stuck RUNNING jobs

**⚠️ Cons:**
- Needs proper lease duration tuning
- Reclaiming might re-run a job (at-least-once semantics)

### 4) Retry + DLQ

**✅ Pros:**
- Transient failures automatically recover
- Permanent failures don't block the queue
- DLQ enables manual inspection/debugging

**⚠️ Cons:**
- Without exponential backoff, retries can happen fast (can be added as enhancement)

### 5) Rate limiting via Redis fixed-window counter

**✅ Pros:**
- Very fast
- Works across multiple backend instances
- Simple to implement

**⚠️ Cons:**
- Fixed window boundary issue (burst at minute boundary)

### 6) Live updates via WebSocket (STOMP)

**✅ Pros:**
- Near real-time UI updates
- Less overhead compared to polling

**⚠️ Cons:**
- More moving parts than polling
- Needs tenant-topic separation to avoid leaking data

---

## ✅ Improvements / Future Enhancements
- Exponential backoff retries
- Separate worker service container (true microservice)
- Better pagination + search on backend
- Job cancellation endpoint
- Persistent job logs table (job_events)
- Prometheus + Grafana dashboard

---

## ✅ Stop Everything

```bash
docker compose down
```

**Reset DB:**
```bash
docker compose down -v
```
