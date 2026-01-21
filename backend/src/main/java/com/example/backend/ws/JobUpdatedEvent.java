package com.example.backend.ws;

import com.example.backend.model.JobStatus;

import java.time.Instant;
import java.util.UUID;

public class JobUpdatedEvent {

    private WsEventType eventType = WsEventType.JOB_UPDATED;

    private UUID jobId;
    private String tenantId;
    private JobStatus status;

    private int attempts;
    private int maxAttempts;

    private Instant updatedAt;

    public JobUpdatedEvent() {
    }

    public JobUpdatedEvent(UUID jobId, String tenantId, JobStatus status, int attempts, int maxAttempts,
            Instant updatedAt) {
        this.jobId = jobId;
        this.tenantId = tenantId;
        this.status = status;
        this.attempts = attempts;
        this.maxAttempts = maxAttempts;
        this.updatedAt = updatedAt;
    }

    public WsEventType getEventType() {
        return eventType;
    }

    public UUID getJobId() {
        return jobId;
    }

    public String getTenantId() {
        return tenantId;
    }

    public JobStatus getStatus() {
        return status;
    }

    public int getAttempts() {
        return attempts;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
