package com.example.backend.ratelimit;

import com.example.backend.exception.RateLimitExceededException;
import com.example.backend.metrics.JobMetrics;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;

@Service
public class RateLimitService {

    private static final int MAX_JOBS_PER_MINUTE = 10;

    private final StringRedisTemplate redisTemplate;
    private final JobMetrics jobMetrics;

    public RateLimitService(StringRedisTemplate redisTemplate, JobMetrics jobMetrics) {
        this.redisTemplate = redisTemplate;
        this.jobMetrics = jobMetrics;
    }

    public void checkAndConsumeJobCreateAllowance(String tenantId) {
        String minuteBucket = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
                .withZone(ZoneOffset.UTC)
                .format(Instant.now());

        String key = "rate:jobs:create:" + tenantId + ":" + minuteBucket;

        Long currentCount = redisTemplate.opsForValue().increment(key);

        // Set TTL only when first created
        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(key, 60, TimeUnit.SECONDS);
        }
        jobMetrics.incRateLimitBlocked();

        if (currentCount != null && currentCount > MAX_JOBS_PER_MINUTE) {
            throw new RateLimitExceededException(
                    "Rate limit exceeded: max " + MAX_JOBS_PER_MINUTE + " job submissions per minute for tenant: "
                            + tenantId);
        }
    }
}
