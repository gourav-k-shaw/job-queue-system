package com.example.backend.dto;

public class JobSummaryResponse {

    private long pending;
    private long running;
    private long done;
    private long dlq;

    public JobSummaryResponse(long pending, long running, long done, long dlq) {
        this.pending = pending;
        this.running = running;
        this.done = done;
        this.dlq = dlq;
    }

    public long getPending() {
        return pending;
    }

    public long getRunning() {
        return running;
    }

    public long getDone() {
        return done;
    }

    public long getDlq() {
        return dlq;
    }
}
