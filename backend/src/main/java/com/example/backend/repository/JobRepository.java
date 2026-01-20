package com.example.backend.repository;

import com.example.backend.model.JobEntity;
import com.example.backend.model.JobStatus;

import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<JobEntity, UUID> {

    Optional<JobEntity> findByTenantIdAndIdempotencyKey(String tenantId, String idempotencyKey);

    long countByTenantIdAndStatus(String tenantId, JobStatus status);

    // ✅ Claim ONE available job safely using SKIP LOCKED
    @Transactional
    @Query(value = """
            SELECT * FROM jobs
            WHERE
                (
                    status = 'PENDING'
                    OR (status = 'RUNNING' AND lease_until IS NOT NULL AND lease_until < now())
                )
            ORDER BY created_at ASC
            FOR UPDATE SKIP LOCKED
            LIMIT 1
            """, nativeQuery = true)
    Optional<JobEntity> findOneAvailableJobForUpdateSkipLocked();

    // ✅ Update job to RUNNING + lease
    @Modifying
    @Transactional
    @Query(value = """
            UPDATE jobs
            SET status = 'RUNNING',
                lease_until = :leaseUntil,
                updated_at = now()
            WHERE id = :jobId
            """, nativeQuery = true)
    int markJobAsRunning(@Param("jobId") UUID jobId, @Param("leaseUntil") Instant leaseUntil);

    @Transactional
    @Query(value = """
            WITH running_counts AS (
                SELECT tenant_id, COUNT(*) AS running_count
                FROM jobs
                WHERE status = 'RUNNING'
                AND lease_until IS NOT NULL
                AND lease_until > now()
                GROUP BY tenant_id
            )
            SELECT j.*
            FROM jobs j
            LEFT JOIN running_counts rc ON rc.tenant_id = j.tenant_id
            WHERE
                (
                    j.status = 'PENDING'
                    OR (j.status = 'RUNNING' AND j.lease_until IS NOT NULL AND j.lease_until < now())
                )
                AND COALESCE(rc.running_count, 0) < :maxRunningPerTenant
            ORDER BY j.created_at ASC
            FOR UPDATE SKIP LOCKED
            LIMIT 1
            """, nativeQuery = true)
    Optional<JobEntity> findOneAvailableJobForUpdateSkipLockedWithTenantQuota(
            @Param("maxRunningPerTenant") int maxRunningPerTenant);

    Page<JobEntity> findAllByTenantIdOrderByCreatedAtDesc(String tenantId, Pageable pageable);

    Page<JobEntity> findAllByTenantIdAndStatusOrderByCreatedAtDesc(String tenantId, JobStatus status,
            Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE jobs
            SET status = 'DONE',
                lease_until = NULL,
                last_error = NULL,
                updated_at = now()
            WHERE id = :jobId
            """, nativeQuery = true)
    int markJobAsDone(@Param("jobId") UUID jobId);

    @Modifying
    @Transactional
    @Query(value = """
            UPDATE jobs
            SET status = :status,
                attempts = :attempts,
                lease_until = NULL,
                last_error = :lastError,
                updated_at = now()
            WHERE id = :jobId
            """, nativeQuery = true)
    int markJobAfterFailure(
            @Param("jobId") UUID jobId,
            @Param("status") String status,
            @Param("attempts") int attempts,
            @Param("lastError") String lastError);

    @Query(value = """
            SELECT COUNT(*)
            FROM jobs
            WHERE tenant_id = :tenantId
            AND status = 'RUNNING'
            AND lease_until IS NOT NULL
            AND lease_until > now()
            """, nativeQuery = true)
    long countActiveRunningJobs(@Param("tenantId") String tenantId);
}
