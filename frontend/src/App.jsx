import { useEffect, useMemo, useState } from "react";
import { fetchJobs, fetchSummary, submitJob } from "./api";
import { useJobWebSocket } from "./useJobWebSocket";

const STATUSES = ["PENDING", "RUNNING", "DONE", "DLQ"];

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

  // UI state
  const [activeTab, setActiveTab] = useState("ALL"); // ALL | DLQ
  const [statusFilter, setStatusFilter] = useState(""); // "" = all statuses (ALL tab only)
  const [search, setSearch] = useState("");
  const [newJobShouldFail, setNewJobShouldFail] = useState(false);

  // Pagination state
  const [page, setPage] = useState(0);
  const [size] = useState(20);

  const [totalPages, setTotalPages] = useState(0);
  const [totalElements, setTotalElements] = useState(0);

  // ✅ Fetch summary
  async function loadSummary() {
    const s = await fetchSummary(tenantId);
    setSummary(s);
  }

  // ✅ Fetch jobs (server-side)
  async function loadJobs() {
    setLoading(true);
    try {
      let responsePage;

      if (activeTab === "DLQ") {
        // DLQ uses dedicated endpoint
        responsePage = await fetchJobs(tenantId, { status: "DLQ", page, size });
      } else {
        // ALL tab
        responsePage = await fetchJobs(tenantId, {
          status: statusFilter || undefined,
          page,
          size,
        });
      }

      setJobs(responsePage.content || []);
      setTotalPages(responsePage.totalPages ?? 0);
      setTotalElements(responsePage.totalElements ?? 0);
    } catch (e) {
      console.error(e);
      alert("Failed to load jobs: " + e.message);
    } finally {
      setLoading(false);
    }
  }

  // ✅ On tenant change reset everything
  useEffect(() => {
    setPage(0);
    setStatusFilter("");
    setSearch("");
  }, [tenantId]);

  // ✅ Load summary once + on tenant change
  useEffect(() => {
    loadSummary().catch((e) => console.error(e));
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tenantId]);

  // ✅ Load jobs whenever any list dependency changes
  useEffect(() => {
    loadJobs();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tenantId, activeTab, statusFilter, page, size]);

  // ✅ WebSocket live updates
  useJobWebSocket({
    tenantId,
    onJobEvent: (event) => {
      if (event.eventType !== "JOB_UPDATED") return;

      // update only if job exists in the current loaded page
      setJobs((prev) => {
        const idx = prev.findIndex((j) => j.id === event.jobId);
        if (idx === -1) return prev; // ignore if not in current page

        const copy = [...prev];
        copy[idx] = {
          ...copy[idx],
          status: event.status,
          attempts: event.attempts,
          maxAttempts: event.maxAttempts,
          updatedAt: event.updatedAt,
        };
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

      // optional: refresh page 0 after creating a job
      setPage(0);
      await loadJobs();
      await loadSummary();
    } catch (e) {
      console.error(e);
      alert("Job submission failed: " + e.message);
    }
  }

  // ✅ Search filter is client-side (only filters current page)
  const visibleJobs = useMemo(() => {
    if (!search.trim()) return jobs;

    const s = search.trim().toLowerCase();
    return jobs.filter((j) => (j.id || "").toLowerCase().includes(s));
  }, [jobs, search]);

  const canGoPrev = page > 0;
  const canGoNext = page + 1 < totalPages;

  return (
    <div style={{ fontFamily: "sans-serif", padding: 20, maxWidth: 1200, margin: "0 auto" }}>
      <h2 style={{ marginBottom: 8 }}>Job Queue Dashboard</h2>
      <p style={{ marginTop: 0, color: "#666", fontSize: 12 }}>
        Server-side filtering + pagination • WebSocket live summary updates
      </p>

      {/* Controls */}
      <div style={{ display: "flex", gap: 12, alignItems: "center", flexWrap: "wrap", marginBottom: 16 }}>
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

        <button onClick={handleSubmitJob} style={btnStyle}>
          Submit Job
        </button>

        <button
          onClick={() => {
            loadJobs();
            loadSummary();
          }}
          style={btnStyle}
        >
          Refresh
        </button>

        {loading && <span style={{ fontSize: 12, color: "#666" }}>Loading...</span>}
      </div>

      {/* Summary Cards */}
      <div style={{ display: "flex", gap: 12, marginBottom: 16 }}>
        <Card title="Pending" value={summary.pending} />
        <Card title="Running" value={summary.running} />
        <Card title="Done" value={summary.done} />
        <Card title="DLQ" value={summary.dlq} />
      </div>

      {/* Tabs */}
      <div style={{ display: "flex", gap: 8, marginBottom: 12 }}>
        <TabButton
          active={activeTab === "ALL"}
          onClick={() => {
            setActiveTab("ALL");
            setPage(0);
          }}
        >
          All Jobs
        </TabButton>
        <TabButton
          active={activeTab === "DLQ"}
          onClick={() => {
            setActiveTab("DLQ");
            setPage(0);
          }}
        >
          DLQ
        </TabButton>
      </div>

      {/* Filters */}
      <div style={{ display: "flex", gap: 12, alignItems: "center", marginBottom: 12, flexWrap: "wrap" }}>
        <label>
          Status:
          <select
            value={activeTab === "DLQ" ? "DLQ" : statusFilter}
            onChange={(e) => {
              setStatusFilter(e.target.value);
              setPage(0);
            }}
            style={{ marginLeft: 8, padding: 6 }}
            disabled={activeTab === "DLQ"} // DLQ tab fixed
          >
            <option value="">All</option>
            {STATUSES.map((s) => (
              <option key={s} value={s}>
                {s}
              </option>
            ))}
          </select>
        </label>

        <label style={{ flex: 1, minWidth: 250 }}>
          Search Job ID (current page):
          <input
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="e.g. 3f2a..."
            style={{ marginLeft: 8, padding: 6, width: "100%" }}
          />
        </label>

        <div style={{ fontSize: 12, color: "#666" }}>
          Total: <b>{totalElements}</b> jobs • Page: <b>{page + 1}</b> / <b>{Math.max(totalPages, 1)}</b>
        </div>
      </div>

      {/* Pagination controls */}
      <div style={{ display: "flex", gap: 8, alignItems: "center", marginBottom: 12 }}>
        <button
          onClick={() => setPage((p) => Math.max(0, p - 1))}
          disabled={!canGoPrev}
          style={{ ...btnStyle, opacity: canGoPrev ? 1 : 0.5 }}
        >
          Prev
        </button>

        <button
          onClick={() => setPage((p) => p + 1)}
          disabled={!canGoNext}
          style={{ ...btnStyle, opacity: canGoNext ? 1 : 0.5 }}
        >
          Next
        </button>
      </div>

      {/* Jobs Table */}
      <div style={{ border: "1px solid #ddd", borderRadius: 10, overflow: "hidden" }}>
        <div style={{ padding: 12, fontWeight: "bold", background: "#f8f8f8" }}>
          {activeTab === "DLQ" ? "Dead Letter Queue Jobs" : "Jobs"}
        </div>

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
            {visibleJobs.map((j) => (
              <tr key={j.id} style={{ borderTop: "1px solid #eee" }}>
                <td style={{ fontFamily: "monospace", fontSize: 12 }}>{j.id}</td>
                <td>
                  <StatusPill status={j.status} />
                </td>
                <td>
                  {j.attempts}/{j.maxAttempts}
                </td>
                <td style={{ fontFamily: "monospace", fontSize: 12 }}>
                  {j.updatedAt ? new Date(j.updatedAt).toLocaleString() : "-"}
                </td>
              </tr>
            ))}

            {visibleJobs.length === 0 && (
              <tr>
                <td colSpan="4" style={{ padding: 12, color: "#666" }}>
                  No jobs on this page.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      <p style={{ marginTop: 12, fontSize: 12, color: "#666" }}>
        Tip: WebSocket updates <b>summary</b> live. For full list sync across pages, use <b>Refresh</b>.
      </p>
    </div>
  );
}

/* ---------------- UI Components ---------------- */

function Card({ title, value }) {
  return (
    <div style={{ flex: 1, border: "1px solid #ddd", borderRadius: 10, padding: 12 }}>
      <div style={{ fontSize: 12, color: "#666" }}>{title}</div>
      <div style={{ fontSize: 24, fontWeight: "bold" }}>{value}</div>
    </div>
  );
}

function TabButton({ active, children, onClick }) {
  return (
    <button
      onClick={onClick}
      style={{
        ...btnStyle,
        background: active ? "#111" : "#fff",
        color: active ? "#fff" : "#111",
        border: "1px solid #111",
      }}
    >
      {children}
    </button>
  );
}

function StatusPill({ status }) {
  let bg = "#eee";
  let fg = "#111";

  if (status === "PENDING") bg = "#fff3cd";
  if (status === "RUNNING") bg = "#d1ecf1";
  if (status === "DONE") bg = "#d4edda";
  if (status === "DLQ") bg = "#f8d7da";

  return (
    <span
      style={{
        padding: "4px 10px",
        borderRadius: 999,
        background: bg,
        color: fg,
        fontSize: 12,
        fontWeight: "bold",
      }}
    >
      {status}
    </span>
  );
}

const btnStyle = {
  padding: "6px 12px",
  cursor: "pointer",
  borderRadius: 8,
  border: "1px solid #ddd",
  background: "#fff",
  color: "#111",
  fontWeight: 600,
};
