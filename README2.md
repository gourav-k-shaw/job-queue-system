# Docker Commands Reference Guide

## 1️⃣ Core Docker Lifecycle (Must-Know)

### 🔹 Check Docker is running
```bash
docker info
docker version
```

---

## 2️⃣ Images & Containers (Inspection Commands)

### 🔹 List images
```bash
docker images
```

### 🔹 List running containers
```bash
docker ps
```

### 🔹 List all containers (including stopped)
```bash
docker ps -a
```

### 🔹 Inspect a container (env vars, volumes, ports)
```bash
docker inspect <container_name_or_id>
```

---

## 3️⃣ PostgreSQL in Docker (Verification & Debugging)

### 🔹 Pull Postgres image
```bash
docker pull postgres:15
```

### 🔹 Run Postgres manually
```bash
docker run -d \
  --name postgres-db \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -e POSTGRES_DB=job_queue \
  -p 5432:5432 \
  postgres:15
```

### 🔹 Verify Postgres container is running
```bash
docker ps | grep postgres
```

### 🔹 Check Postgres logs (🔥 VERY IMPORTANT)
```bash
docker logs postgres-db
```

**Follow logs (live):**
```bash
docker logs -f postgres-db
```

**You should see:**
```
database system is ready to accept connections
```

### 🔹 Enter Postgres container
```bash
docker exec -it postgres-db bash
```

### 🔹 Connect to Postgres CLI
```bash
psql -U postgres -d job_queue
```

### 🔹 Verify DB & tables
```sql
\l              -- list databases
\dt             -- list tables
SELECT * FROM jobs;
```

**Exit:**
```sql
\q
```

---

## 4️⃣ Docker Compose (Most Important for Assignments)

### 🔹 Start everything
```bash
docker compose up
```

### 🔹 Start in detached mode
```bash
docker compose up -d
```

### 🔹 Rebuild images (after code changes)
```bash
docker compose up --build
```

### 🔹 Stop services (graceful)
```bash
docker compose stop
```

### 🔹 Stop & remove containers
```bash
docker compose down
```

### 🔹 Stop + remove volumes (⚠️ deletes DB)
```bash
docker compose down -v
```

---

## 5️⃣ Logs (Job Queue Debugging Gold)

### 🔹 All services logs
```bash
docker compose logs
```

### 🔹 Follow logs (live)
```bash
docker compose logs -f
```

### 🔹 Logs for a single service
```bash
docker compose logs job-worker
docker compose logs api
docker compose logs postgres
```

### 🔹 Tail last N lines
```bash
docker compose logs --tail=100 job-worker
```

---

## 6️⃣ Container Shell Access (Debugging Workers)

### 🔹 Enter worker container
```bash
docker exec -it job-worker bash
```

**or (alpine images):**
```bash
docker exec -it job-worker sh
```

### 🔹 Check running processes inside container
```bash
ps aux
```

---

## 7️⃣ Health & Connectivity Checks

### 🔹 Check exposed ports
```bash
docker port postgres-db
```

### 🔹 Test Postgres connection from host
```bash
psql -h localhost -p 5432 -U postgres job_queue
```

### 🔹 Test DB from worker container
```bash
docker exec -it job-worker psql -h postgres -U postgres job_queue
```

> [!NOTE]
> **Interview note:** "Inside Docker Compose, services talk using service names as hostnames."

---

## 8️⃣ Restarting Things (Very Common)

### 🔹 Restart a container
```bash
docker restart job-worker
docker restart postgres-db
```

### 🔹 Restart a compose service
```bash
docker compose restart job-worker
```

---

## 9️⃣ Cleanup Commands (When Things Go Sideways 😅)

### 🔹 Stop all containers
```bash
docker stop $(docker ps -q)
```

### 🔹 Remove all containers
```bash
docker rm $(docker ps -aq)
```

### 🔹 Remove unused images
```bash
docker image prune
```

### 🔹 Full cleanup (⚠️ nuclear option)
```bash
docker system prune -a
```

---

## 🔟 Volumes (Postgres Persistence)

### 🔹 List volumes
```bash
docker volume ls
```

### 🔹 Inspect volume
```bash
docker volume inspect jobqueue_postgres_data
```

### 🔹 Remove volume
```bash
docker volume rm jobqueue_postgres_data
```

---

## 1️⃣1️⃣ Common Interview-Style Verifications

### ✅ "How do you know Postgres is ready?"
- Logs show: `ready to accept connections`
- `psql` works
- Healthcheck passes

### ✅ "How do you debug stuck jobs?"
- Worker logs
- DB state (jobs table)
- Retry count / status column
- Dead workers (no heartbeat)

### ✅ "How do you restart only the worker?"
```bash
docker compose restart job-worker
```