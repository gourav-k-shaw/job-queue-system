package com.example.backend.ws;

import java.time.Instant;

public class SummaryUpdatedEvent {

    private WsEventType eventType = WsEventType.SUMMARY_UPDATED;

    private String tenantId;

    private long pending;
    private long running;
    private long done;
    private long dlq;

    private Instant timestamp;

    public SummaryUpdatedEvent() {
    }

    public SummaryUpdatedEvent(String tenantId, long pending, long running, long done, long dlq) {
        this.tenantId = tenantId;
        this.pending = pending;
        this.running = running;
        this.done = done;
        this.dlq = dlq;
        this.timestamp = Instant.now();
    }

    public WsEventType getEventType() {
        return eventType;
    }

    public String getTenantId() {
        return tenantId;
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

    public Instant getTimestamp() {
        return timestamp;
    }
}
