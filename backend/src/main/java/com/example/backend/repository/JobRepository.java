package com.example.backend.repository;

import com.example.backend.model.JobEntity;
import com.example.backend.model.JobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<JobEntity, UUID> {

    Optional<JobEntity> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);

    long countByTenantIdAndStatus(String tenantId, JobStatus status);

    Page<JobEntity> findAllByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    Page<JobEntity> findAllByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, JobStatus status,
            Pageable pageable);
}
