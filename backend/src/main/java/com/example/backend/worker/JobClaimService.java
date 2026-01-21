package com.example.backend.worker;

import com.example.backend.model.JobEntity;
import com.example.backend.repository.JobRepository;
import com.example.backend.service.JobSummaryService;
import com.example.backend.ws.WsPublisher;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class JobClaimService {

    private static final Logger log = LoggerFactory.getLogger(JobClaimService.class);

    private static final int MAX_RUNNING_PER_TENANT = 5;

    private final JobRepository jobRepository;
    private final WorkerProperties workerProperties;
    private final WsPublisher wsPublisher;
    private final JobSummaryService jobSummaryService;

    // simple worker id for logs (per app instance)
    private final String workerId = UUID.randomUUID().toString();

    public JobClaimService(
            JobRepository jobRepository,
            WorkerProperties workerProperties,
            WsPublisher wsPublisher,
            JobSummaryService jobSummaryService) {
        this.jobRepository = jobRepository;
        this.workerProperties = workerProperties;
        this.wsPublisher = wsPublisher;
        this.jobSummaryService = jobSummaryService;
    }

    @Transactional
    public Optional<JobEntity> claimNextJob() {

        Optional<JobEntity> jobOpt = jobRepository
                .findOneAvailableJobForUpdateSkipLockedWithTenantQuota(MAX_RUNNING_PER_TENANT);

        if (jobOpt.isEmpty()) {
            return Optional.empty();
        }

        JobEntity job = jobOpt.get();

        // Step 2: assign lease and mark RUNNING
        Instant leaseUntil = Instant.now().plusSeconds(workerProperties.getLeaseSeconds());
        jobRepository.markJobAsRunning(job.getId(), leaseUntil);

        // Refresh in-memory object
        job.setLeaseUntil(leaseUntil);
        job.setStatus(com.example.backend.model.JobStatus.RUNNING);

        log.info("event=JOB_CLAIMED worker={} tenant={} jobId={} leaseUntil={}",
                workerId, job.getTenantId(), job.getId(), leaseUntil);

        wsPublisher.publishJobUpdate(job);
        wsPublisher.publishSummaryUpdate(
                job.getTenantId(),
                jobSummaryService.getSummaryForTenant(job.getTenantId()));

        return Optional.of(job);
    }
}
