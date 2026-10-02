
package com.cs_42_3.surveyplatformbackend.security.ratelimit;

import io.github.bucket4j.TimeMeter;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitServiceTest {

    /** Verifies that login requests are rejected after ten attempts from the same IP. */
    @Test
    void shouldRejectLoginAfterRateLimitIsExceeded() {

        RateLimitService rateLimitService = new RateLimitService();
        String clientIp = "192.0.2.1";

        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimitService.allowLogin(clientIp));
        }

        assertFalse(rateLimitService.allowLogin(clientIp));
    }

    /** Verifies that registration requests are rejected after five attempts from the same IP. */
    @Test
    void shouldRejectRegistrationAfterRateLimitIsExceeded() {

        RateLimitService rateLimitService = new RateLimitService();
        String clientIp = "192.0.2.2";

        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimitService.allowRegister(clientIp));
        }

        assertFalse(rateLimitService.allowRegister(clientIp));
    }

    /** Verifies that rate limits are maintained independently for different IP addresses. */
    @Test
    void shouldApplyRateLimitsIndependentlyForDifferentIps() {

        RateLimitService rateLimitService = new RateLimitService();

        String firstIp = "192.0.2.3";
        String secondIp = "192.0.2.4";

        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimitService.allowLogin(firstIp));
        }

        assertFalse(rateLimitService.allowLogin(firstIp));
        assertTrue(rateLimitService.allowLogin(secondIp));
    }


    /** Verifies that login requests are allowed again after the refill interval. */
    @Test
    void shouldRestoreLoginRequestsAfterRefill() {

        AtomicLong currentTime = new AtomicLong(0);

        TimeMeter testClock = new TimeMeter() {
            @Override
            public long currentTimeNanos() {
                return currentTime.get();
            }

            @Override
            public boolean isWallClockBased() {
                return false;
            }
        };

        RateLimitService rateLimitService =
                new RateLimitService(testClock);

        String clientIp = "192.0.2.5";

        for (int i = 0; i < 10; i++) {
            assertTrue(rateLimitService.allowLogin(clientIp));
        }

        assertFalse(rateLimitService.allowLogin(clientIp));

        // Advance simulated time by 59 seconds.
        currentTime.addAndGet(Duration.ofSeconds(59).toNanos());

        assertFalse(rateLimitService.allowLogin(clientIp));

        // Complete the one-minute refill interval.
        currentTime.addAndGet(Duration.ofSeconds(1).toNanos());

        assertTrue(rateLimitService.allowLogin(clientIp));
    }

    /** Verifies that registration requests are restored after the refill interval. */
    @Test
    void shouldRestoreRegistrationRequestsAfterRefill() {

        AtomicLong currentTime = new AtomicLong(0);

        TimeMeter testClock = new TimeMeter() {
            @Override
            public long currentTimeNanos() {
                return currentTime.get();
            }

            @Override
            public boolean isWallClockBased() {
                return false;
            }
        };

        RateLimitService rateLimitService =
                new RateLimitService(testClock);

        String clientIp = "192.0.2.6";

        for (int i = 0; i < 5; i++) {
            assertTrue(rateLimitService.allowRegister(clientIp));
        }

        assertFalse(rateLimitService.allowRegister(clientIp));

        // Advance simulated time by one minute.
        currentTime.addAndGet(Duration.ofMinutes(1).toNanos());

        assertTrue(rateLimitService.allowRegister(clientIp));
    }

}
