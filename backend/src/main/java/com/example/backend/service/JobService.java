package com.example.backend.service;

import com.example.backend.dto.CreateJobRequest;
import com.example.backend.dto.CreateJobResponse;
import com.example.backend.model.JobEntity;
import com.example.backend.model.JobStatus;
import com.example.backend.repository.JobRepository;
import com.example.backend.tenant.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class JobService {

    private final JobRepository jobRepository;
    private final ObjectMapper objectMapper;

    public JobService(JobRepository jobRepository, ObjectMapper objectMapper) {
        this.jobRepository = jobRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CreateJobResponse submitJob(CreateJobRequest request) {
        String tenantId = TenantContext.getTenantId();

        String idempotencyKey = request.getIdempotencyKey();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<JobEntity> existing = jobRepository.findByTenantIdAndIdempotencyKey(tenantId, idempotencyKey);

            if (existing.isPresent()) {
                JobEntity job = existing.get();
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
}
