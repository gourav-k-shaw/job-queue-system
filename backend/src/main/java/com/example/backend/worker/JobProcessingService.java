package com.example.backend.worker;

import com.example.backend.model.JobEntity;
import com.example.backend.repository.JobRepository;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.Random;

@Service
public class JobProcessingService {

    private final JobRepository jobRepository;
    private final Random random = new Random();
    private static final Logger log = LoggerFactory.getLogger(JobProcessingService.class);

    public JobProcessingService(JobRepository jobRepository) {
        this.jobRepository = jobRepository;
    }

    public void process(JobEntity job) {
        try {
            // 1) Simulate execution time
            simulateWork(job);

            // 2) Mark DONE
            jobRepository.markJobAsDone(job.getId());

        } catch (Exception e) {
            handleFailure(job, e);
        }
    }

    private void simulateWork(JobEntity job) throws Exception {
        JsonNode payload = job.getPayload();
        log.info("Processing job jobId={} tenantId={} payload={}", job.getId(), job.getTenantId(), payload);

        // Optional: fail based on payload flag
        boolean shouldFail = payload.has("shouldFail") && payload.get("shouldFail").asBoolean(false);

        // Optional: random failure 10% if not explicitly controlled
        boolean randomFail = random.nextInt(100) < 10;

        // Simulate work time
        Thread.sleep(2000);

        log.info("Job processed jobId={} tenantId={} payload={}", job.getId(), job.getTenantId(), payload);

        if (shouldFail || randomFail) {
            throw new RuntimeException("Simulated job failure (shouldFail=" + shouldFail + ")");
        }
    }

    private void handleFailure(JobEntity job, Exception e) {
        int nextAttempts = job.getAttempts() + 1;
        String errorMsg = e.getMessage();

        boolean shouldMoveToDlq = nextAttempts >= job.getMaxAttempts();

        if (shouldMoveToDlq) {
            jobRepository.markJobAfterFailure(job.getId(), "DLQ", nextAttempts, errorMsg);
            log.error("Job moved to DLQ jobId={} after {} attempts", job.getId(), nextAttempts);
        } else {
            // You may choose to mark FAILED first, then PENDING.
            // For simplicity we go directly back to PENDING.
            jobRepository.markJobAfterFailure(job.getId(), "PENDING", nextAttempts, errorMsg);
            log.warn("Job failed jobId={} attempt={} error={}", job.getId(), nextAttempts, errorMsg);
        }
    }
}
