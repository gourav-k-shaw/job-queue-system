package com.example.backend.service;

import com.example.backend.dto.JobSummaryResponse;
import com.example.backend.model.JobStatus;
import com.example.backend.repository.JobRepository;
import org.springframework.stereotype.Service;

@Service
public class JobSummaryService {

    private final JobRepository jobRepository;

    public JobSummaryService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public JobSummaryResponse getSummaryForTenant(String tenantId) {
        long pending = jobRepository.countByTenantIdAndStatus(tenantId, JobStatus.PENDING);
        long running = jobRepository.countActiveRunningJobs(tenantId);
        long done = jobRepository.countByTenantIdAndStatus(tenantId, JobStatus.DONE);
        long dlq = jobRepository.countByTenantIdAndStatus(tenantId, JobStatus.DLQ);

        return new JobSummaryResponse(pending, running, done, dlq);
    }
}
