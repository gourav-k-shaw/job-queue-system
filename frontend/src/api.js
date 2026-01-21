const BACKEND_URL = "http://localhost:8080";

export async function fetchSummary(tenantId) {
    const res = await fetch(`${BACKEND_URL}/api/jobs/summary`, {
        headers: {
            "X-Tenant-Id": tenantId,
        },
    });

    if (!res.ok) throw new Error(await res.text());
    return res.json();
}

export async function fetchJobs(tenantId, { status, page = 0, size = 20 } = {}) {
    const params = new URLSearchParams();
    if (status) params.append("status", status);
    params.append("page", String(page));
    params.append("size", String(size));

    const res = await fetch(`${BACKEND_URL}/api/jobs?${params.toString()}`, {
        headers: {
            "X-Tenant-Id": tenantId,
        },
    });

    if (!res.ok) throw new Error(await res.text());
    return res.json(); // Spring Page object
}

export async function submitJob(tenantId, payload, idempotencyKey) {
    const res = await fetch(`${BACKEND_URL}/api/jobs`, {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
            "X-Tenant-Id": tenantId,
        },
        body: JSON.stringify({
            payload,
            idempotencyKey,
        }),
    });

    if (!res.ok) throw new Error(await res.text());
    return res.json();
}
