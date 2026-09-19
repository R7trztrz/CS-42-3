package com.cs_42_3.surveyplatformbackend.security.ratelimit;

import io.github.bucket4j.Bucket;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Provides in-memory rate limiting for authentication endpoints.
 *
 * @author Jiale Chen
 */
@Service
public class RateLimitService {

    private final ConcurrentMap<String, Bucket> registerBuckets = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Bucket> loginBuckets = new ConcurrentHashMap<>();

    public boolean allowRegister(String clientIp) {
        Bucket bucket = registerBuckets.computeIfAbsent(
                clientIp,
                ip -> createBucket(5)
        );

        return bucket.tryConsume(1);
    }

    public boolean allowLogin(String clientIp) {
        Bucket bucket = loginBuckets.computeIfAbsent(
                clientIp,
                ip -> createBucket(10)
        );

        return bucket.tryConsume(1);
    }

    private Bucket createBucket(long requestsPerMinute) {
        return Bucket.builder()
                .addLimit(limit -> limit
                        .capacity(requestsPerMinute)
                        .refillIntervally(
                                requestsPerMinute,
                                Duration.ofMinutes(1)
                        )
                )
                .build();
    }
}