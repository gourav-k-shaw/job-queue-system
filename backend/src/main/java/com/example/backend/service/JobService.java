package com.example.backend.service;

import com.example.backend.dto.CreateJobRequest;
import com.example.backend.dto.CreateJobResponse;
import com.example.backend.dto.JobResponse;
import com.example.backend.dto.JobSummaryResponse;
import com.example.backend.exception.QuotaExceededException;
import com.example.backend.metrics.JobMetrics;
import com.example.backend.model.JobEntity;
import com.example.backend.model.JobStatus;
import com.example.backend.repository.JobRepository;
import com.example.backend.tenant.TenantContext;
import com.example.backend.ws.WsPublisher;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.example.backend.ratelimit.RateLimitService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.backend.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.UUID;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper;
    private static final int MAX_CONCURRENT_RUNNING_JOBS = 5;
    private final RateLimitService rateLimitService;
    private final JobSummaryService jobSummaryService;
    private final WsPublisher wsPublisher;
    private static final Logger log = LoggerFactory.getLogger(JobService.class);
    private final JobMetrics jobMetrics;

    public JobService(JobRepository jobRepository, ObjectMapper objectMapper, RateLimitService rateLimitService,
            JobSummaryService jobSummaryService, WsPublisher wsPublisher, JobMetrics jobMetrics) {
        this.jobRepository = jobRepository;
        this.objectMapper = objectMapper;
        this.rateLimitService = rateLimitService;
        this.jobSummaryService = jobSummaryService;
        this.wsPublisher = wsPublisher;
        this.jobMetrics = jobMetrics;
    }

    @Transactional
    public CreateJobResponse submitJob(CreateJobRequest request) {
        String tenantId = TenantContext.getTenantId();
        log.info("event=JOB_SUBMIT_REQUEST tenant={} idempotencyKey={}",
                tenantId, request.getIdempotencyKey());

        rateLimitService.checkAndConsumeJobCreateAllowance(tenantId);

        long runningCount = jobRepository.countByTenantIdAndStatus(tenantId, JobStatus.RUNNING);
        if (runningCount >= MAX_CONCURRENT_RUNNING_JOBS) {
            jobMetrics.incQuotaBlocked();
            throw new QuotaExceededException(
                    "Max concurrent running jobs reached (" + MAX_CONCURRENT_RUNNING_JOBS + ") for tenant: "
                            + tenantId);
        }

        String idempotencyKey = request.getIdempotencyKey();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<JobEntity> existing = jobRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey);

            if (existing.isPresent()) {
                JobEntity job = existing.get();
                log.info("event=JOB_DEDUPLICATED tenant={} jobId={} idempotencyKey={}",
                        tenantId, job.getId(), idempotencyKey);
                return new CreateJobResponse(job.getId(), job.getStatus(), true);
            }
        }

        // Create fresh job
        JobEntity job = new JobEntity();
        job.setId(UUID.randomUUID());
        job.setTenantId(tenantId);
        job.setStatus(JobStatus.PENDING);
        job.setAttempts(0);
        job.setMaxAttempts(3);
        job.setIdempotencyKey(idempotencyKey);

        try {
            job.setPayload(objectMapper.valueToTree(request.getPayload()));
        } catch (Exception e) {
            throw new RuntimeException("Invalid payload JSON", e);
        }

        try {
            JobEntity saved = jobRepository.save(job);
            wsPublisher.publishJobUpdate(saved);
            wsPublisher.publishSummaryUpdate(
                    tenantId,
                    jobSummaryService.getSummaryForTenant(tenantId));
            log.info("event=JOB_SUBMITTED tenant={} jobId={} status={}",
                    tenantId, saved.getId(), saved.getStatus());
            jobMetrics.incSubmitted();
            return new CreateJobResponse(saved.getId(), saved.getStatus(), false);

        } catch (DataIntegrityViolationException ex) {
            // 3) Race-condition scenario:
            // another request inserted same (tenant_id, idempotency_key) before this insert
            // DB unique index blocks this insert -> so we fetch and return existing job

            if (idempotencyKey != null && !idempotencyKey.isBlank()) {
                Optional<JobEntity> existing = jobRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey);

                if (existing.isPresent()) {
                    JobEntity existingJob = existing.get();
                    return new CreateJobResponse(existingJob.getId(), existingJob.getStatus(), true);
                }
            }

            // If no idempotency key OR still not found, rethrow
            throw ex;
        }
    }

    private JobResponse toResponse(JobEntity job) {
        JobResponse res = new JobResponse();
        res.setId(job.getId());
        res.setTenantId(job.getTenantId());
        res.setStatus(job.getStatus());
        res.setAttempts(job.getAttempts());
        res.setMaxAttempts(job.getMaxAttempts());
        res.setIdempotencyKey(job.getIdempotencyKey());
        res.setLeaseUntil(job.getLeaseUntil());
        res.setLastError(job.getLastError());
        res.setCreatedAt(job.getCreatedAt());
        res.setUpdatedAt(job.getUpdatedAt());
        res.setPayload(job.getPayload());
        return res;
    }

    public JobResponse getJobById(UUID jobId) {
        String tenantId = TenantContext.getTenantId();

        JobEntity job = jobRepository.findById(jobId)
                .orElseThrow(() -> new NotFoundException("Job not found: " + jobId));

        // Tenant isolation check
        if (!job.getTenantId().equals(tenantId)) {
            throw new NotFoundException("Job not found: " + jobId);
        }

        return toResponse(job);
    }

    public Page<JobResponse> listJobs(String status, int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size);

        Page<JobEntity> jobsPage;

        if (status == null || status.isBlank()) {
            jobsPage = jobRepository.findAllByTenantIdOrderByCreatedAtDesc(tenantId, pageable);
        } else {
            jobsPage = jobRepository.findAllByTenantIdAndStatusOrderByCreatedAtDesc(
                    tenantId,
                    com.example.backend.model.JobStatus.valueOf(status),
                    pageable);
        }

        return jobsPage.map(this::toResponse);
    }

    public Page<JobResponse> listDlqJobs(int page, int size) {
        String tenantId = TenantContext.getTenantId();
        Pageable pageable = PageRequest.of(page, size);

        return jobRepository
                .findAllByTenantIdAndStatusOrderByCreatedAtDesc(
                        tenantId,
                        com.example.backend.model.JobStatus.DLQ,
                        pageable)
                .map(this::toResponse);
    }

    public JobSummaryResponse getSummary() {
        String tenantId = TenantContext.getTenantId();

        long pending = jobRepository.countByTenantIdAndStatus(tenantId, JobStatus.PENDING);
        long running = jobRepository.countActiveRunningJobs(tenantId);
        long done = jobRepository.countByTenantIdAndStatus(tenantId, JobStatus.DONE);
        long dlq = jobRepository.countByTenantIdAndStatus(tenantId, JobStatus.DLQ);

        return new JobSummaryResponse(pending, running, done, dlq);
    }

}
