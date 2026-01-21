package com.example.backend.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class JobMetrics {

    private final Counter jobsSubmitted;
    private final Counter jobsCompleted;
    private final Counter jobsFailed;
    private final Counter jobsRetried;
    private final Counter jobsMovedToDlq;

    private final Counter rateLimitBlocked;
    private final Counter quotaBlocked;

    public JobMetrics(MeterRegistry registry) {
        this.jobsSubmitted = Counter.builder("jobs_submitted_total")
                .description("Total jobs submitted")
                .register(registry);

        this.jobsCompleted = Counter.builder("jobs_completed_total")
                .description("Total jobs completed successfully")
                .register(registry);

        this.jobsFailed = Counter.builder("jobs_failed_total")
                .description("Total job failures (per attempt)")
                .register(registry);

        this.jobsRetried = Counter.builder("jobs_retried_total")
                .description("Total job retries")
                .register(registry);

        this.jobsMovedToDlq = Counter.builder("jobs_dlq_total")
                .description("Total jobs moved to DLQ")
                .register(registry);

        this.rateLimitBlocked = Counter.builder("jobs_rate_limit_blocked_total")
                .description("Total job submissions blocked by rate limit")
                .register(registry);

        this.quotaBlocked = Counter.builder("jobs_concurrency_quota_blocked_total")
                .description("Total job submissions blocked by concurrency quota")
                .register(registry);
    }

    public void incSubmitted() {
        jobsSubmitted.increment();
    }

    public void incCompleted() {
        jobsCompleted.increment();
    }

    public void incFailed() {
        jobsFailed.increment();
    }

    public void incRetried() {
        jobsRetried.increment();
    }

    public void incDlq() {
        jobsMovedToDlq.increment();
    }

    public void incRateLimitBlocked() {
        rateLimitBlocked.increment();
    }

    public void incQuotaBlocked() {
        quotaBlocked.increment();
    }
}
