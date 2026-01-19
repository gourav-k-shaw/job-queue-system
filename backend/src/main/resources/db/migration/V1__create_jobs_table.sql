CREATE TABLE IF NOT EXISTS jobs (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(100) NOT NULL,
    payload JSONB NOT NULL,

    status VARCHAR(20) NOT NULL,
    attempts INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 3,

    idempotency_key VARCHAR(200),

    lease_until TIMESTAMPTZ,

    last_error TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- Unique idempotency per tenant (only if idempotency_key is provided)
CREATE UNIQUE INDEX IF NOT EXISTS uq_jobs_tenant_idempotency
ON jobs(tenant_id, idempotency_key)
WHERE idempotency_key IS NOT NULL;

-- Useful indexes for dashboard + API queries
CREATE INDEX IF NOT EXISTS idx_jobs_tenant_id
ON jobs(tenant_id);

CREATE INDEX IF NOT EXISTS idx_jobs_status
ON jobs(status);

-- Useful for worker picking + lease expiry recovery
CREATE INDEX IF NOT EXISTS idx_jobs_status_lease
ON jobs(status, lease_until);
