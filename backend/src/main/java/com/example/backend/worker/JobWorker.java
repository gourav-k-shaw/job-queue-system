package com.example.backend.worker;

import com.example.backend.model.JobEntity;
import com.example.backend.repository.JobRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import java.util.UUID;
import java.util.Optional;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class JobWorker {

    private static final Logger log = LoggerFactory.getLogger(JobWorker.class);
    private final String workerId = UUID.randomUUID().toString().substring(0, 8);

    private final WorkerProperties workerProperties;
    private final JobClaimService jobClaimService;
    private final JobRepository jobRepository;
    private final JobProcessingService jobProcessingService;

    private ScheduledExecutorService scheduler;

    public JobWorker(WorkerProperties workerProperties, JobRepository jobRepository, JobClaimService jobClaimService,
            JobProcessingService jobProcessingService) {
        this.workerProperties = workerProperties;
        this.jobRepository = jobRepository;
        this.jobClaimService = jobClaimService;
        this.jobProcessingService = jobProcessingService;
    }

    @PostConstruct
    public void start() {
        if (!workerProperties.isEnabled()) {
            log.info("Worker is disabled (jobs.worker.enabled=false)");
            return;
        }

        int concurrency = workerProperties.getConcurrency();
        this.scheduler = Executors.newScheduledThreadPool(concurrency);

        log.info("Worker [{}] started with concurrency={}, poll interval={}ms",
                workerId, concurrency, workerProperties.getPollIntervalMs());

        // Schedule N independent polling tasks for true concurrency
        for (int i = 0; i < concurrency; i++) {
            final int threadIndex = i;
            scheduler.scheduleWithFixedDelay(
                    () -> pollOnceSafely(threadIndex),
                    i * 100L, // Stagger start times to avoid thundering herd
                    workerProperties.getPollIntervalMs(),
                    TimeUnit.MILLISECONDS);
        }
    }

    private void pollOnceSafely(int threadIndex) {
        try {
            pollOnce(threadIndex);
        } catch (Exception e) {
            log.error("Worker [{}] thread-{} poll failed: {}", workerId, threadIndex, e.getMessage(), e);
        }
    }

    private void pollOnce(int threadIndex) {
        Optional<JobEntity> claimed = jobClaimService.claimNextJob();

        if (claimed.isEmpty()) {
            // No work found (normal)
            return;
        }

        JobEntity job = claimed.get();
        log.info("Worker [{}] thread-{} claimed jobId={} tenant={} status={}",
                workerId, threadIndex, job.getId(), job.getTenantId(), job.getStatus());

        jobProcessingService.process(job);
        log.info("Worker [{}] thread-{} finished processing jobId={}", workerId, threadIndex, job.getId());

    }
}
