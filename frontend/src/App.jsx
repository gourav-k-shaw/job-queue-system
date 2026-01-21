import { useEffect, useMemo, useState } from "react";
import { fetchJobs, fetchSummary, submitJob } from "./api";
import { useJobWebSocket } from "./useJobWebSocket";

export default function App() {
  const [tenantId, setTenantId] = useState("user1");

  const [summary, setSummary] = useState({
    pending: 0,
    running: 0,
    done: 0,
    dlq: 0,
  });

  const [jobs, setJobs] = useState([]);
  const [loading, setLoading] = useState(false);

  const [newJobShouldFail, setNewJobShouldFail] = useState(false);

  // ✅ Load initial snapshot via REST
  useEffect(() => {
    let cancelled = false;

    async function loadInitial() {
      try {
        setLoading(true);
        const s = await fetchSummary(tenantId);
        const page = await fetchJobs(tenantId, { page: 0, size: 20 });

        if (cancelled) return;
        setSummary(s);
        setJobs(page.content || []);
      } catch (e) {
        console.error(e);
        alert("Failed to load initial data: " + e.message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    loadInitial();

    return () => {
      cancelled = true;
    };
  }, [tenantId]);

  // ✅ Live updates via WebSocket
  useJobWebSocket({
    tenantId,
    onJobEvent: (event) => {
      if (event.eventType !== "JOB_UPDATED") return;

      setJobs((prev) => {
        const idx = prev.findIndex((j) => j.id === event.jobId);

        const updated = {
          ...prev[idx],
          id: event.jobId,
          tenantId: event.tenantId,
          status: event.status,
          attempts: event.attempts,
          maxAttempts: event.maxAttempts,
          updatedAt: event.updatedAt,
        };

        // If job isn't in list (new pending job), add it on top
        if (idx === -1) return [updated, ...prev];

        // Replace
        const copy = [...prev];
        copy[idx] = { ...copy[idx], ...updated };
        return copy;
      });
    },
    onSummaryEvent: (event) => {
      if (event.eventType !== "SUMMARY_UPDATED") return;

      setSummary({
        pending: event.pending,
        running: event.running,
        done: event.done,
        dlq: event.dlq,
      });
    },
  });

  async function handleSubmitJob() {
    try {
      const payload = {
        type: "demo",
        shouldFail: newJobShouldFail,
      };

      const idempotencyKey = `ui-${Date.now()}`;

      await submitJob(tenantId, payload, idempotencyKey);
      // ✅ no need to fetch again; websocket will update
    } catch (e) {
      console.error(e);
      alert("Job submission failed: " + e.message);
    }
  }

  const rows = useMemo(() => jobs.slice(0, 20), [jobs]);

  return (
    <div style={{ fontFamily: "sans-serif", padding: 20, maxWidth: 1100, margin: "0 auto" }}>
      <h2>Job Queue Dashboard</h2>

      <div style={{ display: "flex", gap: 12, alignItems: "center", marginBottom: 16 }}>
        <label>
          Tenant:
          <input
            value={tenantId}
            onChange={(e) => setTenantId(e.target.value)}
            style={{ marginLeft: 8, padding: 6 }}
          />
        </label>

        <label style={{ display: "flex", alignItems: "center", gap: 8 }}>
          <input
            type="checkbox"
            checked={newJobShouldFail}
            onChange={(e) => setNewJobShouldFail(e.target.checked)}
          />
          shouldFail
        </label>

        <button onClick={handleSubmitJob} style={{ padding: "6px 12px", cursor: "pointer" }}>
          Submit Job
        </button>

        {loading && <span>Loading...</span>}
      </div>

      {/* Summary Cards */}
      <div style={{ display: "flex", gap: 12, marginBottom: 16 }}>
        <Card title="Pending" value={summary.pending} />
        <Card title="Running" value={summary.running} />
        <Card title="Done" value={summary.done} />
        <Card title="DLQ" value={summary.dlq} />
      </div>

      {/* Jobs Table */}
      <div style={{ border: "1px solid #ddd", borderRadius: 8, overflow: "hidden" }}>
        <div style={{ padding: 12, fontWeight: "bold", background: "#f8f8f8" }}>Latest Jobs</div>
        <table width="100%" cellPadding="10" style={{ borderCollapse: "collapse" }}>
          <thead style={{ background: "#fafafa" }}>
            <tr>
              <th align="left">Job ID</th>
              <th align="left">Status</th>
              <th align="left">Attempts</th>
              <th align="left">Updated At</th>
            </tr>
          </thead>
          <tbody>
            {rows.map((j) => (
              <tr key={j.id} style={{ borderTop: "1px solid #eee" }}>
                <td style={{ fontFamily: "monospace", fontSize: 12 }}>{j.id}</td>
                <td>{j.status}</td>
                <td>
                  {j.attempts}/{j.maxAttempts}
                </td>
                <td style={{ fontFamily: "monospace", fontSize: 12 }}>
                  {j.updatedAt ? new Date(j.updatedAt).toLocaleString() : "-"}
                </td>
              </tr>
            ))}
            {rows.length === 0 && (
              <tr>
                <td colSpan="4" style={{ padding: 12 }}>
                  No jobs yet
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <p style={{ marginTop: 12, fontSize: 12, color: "#666" }}>
        Tip: Submit jobs and watch them move PENDING → RUNNING → DONE (or DLQ if shouldFail=true).
      </p>
    </div>
  );
}

function Card({ title, value }) {
  return (
    <div style={{ flex: 1, border: "1px solid #ddd", borderRadius: 8, padding: 12 }}>
      <div style={{ fontSize: 12, color: "#666" }}>{title}</div>
      <div style={{ fontSize: 24, fontWeight: "bold" }}>{value}</div>
    </div>
  );
}
