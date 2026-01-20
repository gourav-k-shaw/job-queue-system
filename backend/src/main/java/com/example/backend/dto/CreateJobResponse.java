package com.example.backend.dto;

import com.example.backend.model.JobStatus;

import java.util.UUID;

public class CreateJobResponse {

    private UUID jobId;
    private JobStatus status;
    private boolean deduplicated;

    public CreateJobResponse(UUID jobId, JobStatus status, boolean deduplicated) {
        this.jobId = jobId;
        this.status = status;
        this.deduplicated = deduplicated;
    }

    public UUID getJobId() {
        return jobId;
    }

    public JobStatus getStatus() {
        return status;
    }

    public boolean isDeduplicated() {
        return deduplicated;
    }
}
