package com.cs_42_3.surveyplatformbackend.security.ratelimit;

/**
 * Thrown when a client exceeds the allowed request rate.
 *
 * @author Jiale Chen
 */
public class RateLimitExceededException extends RuntimeException {

    public RateLimitExceededException() {
        super("Too many requests. Please try again later.");
    }
}