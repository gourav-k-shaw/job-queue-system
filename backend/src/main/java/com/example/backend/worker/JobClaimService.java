package com.example.backend.worker;

import com.example.backend.model.JobEntity;
import com.example.backend.repository.JobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class JobClaimService {

    private static final int MAX_RUNNING_PER_TENANT = 5;

    private final JobRepository jobRepository;
    private final WorkerProperties workerProperties;

    public JobClaimService(JobRepository jobRepository, WorkerProperties workerProperties) {
        this.jobRepository = jobRepository;
        this.workerProperties = workerProperties;
    }

    @Transactional
    public Optional<JobEntity> claimNextJob() {
        // Step 1: lock one job row (or skip if locked by other worker)
        // Optional<JobEntity> jobOpt =
        // jobRepository.findOneAvailableJobForUpdateSkipLocked();
        Optional<JobEntity> jobOpt = jobRepository
                .findOneAvailableJobForUpdateSkipLockedWithTenantQuota(MAX_RUNNING_PER_TENANT);

        if (jobOpt.isEmpty())
            return Optional.empty();

        JobEntity job = jobOpt.get();

        // Step 2: assign lease and mark RUNNING
        Instant leaseUntil = Instant.now().plusSeconds(workerProperties.getLeaseSeconds());
        jobRepository.markJobAsRunning(job.getId(), leaseUntil);

        // Refresh in-memory object
        job.setLeaseUntil(leaseUntil);
        job.setStatus(com.example.backend.model.JobStatus.RUNNING);

        return Optional.of(job);
    }
}
