package com.cs_42_3.surveyplatformbackend.security.turnstile;

/**
 * Thrown when Cloudflare Turnstile human verification fails.
 *
 * @author Jiale Chen
 */
public class HumanVerificationException extends RuntimeException {

    public HumanVerificationException() {
        super("Human verification failed.");
    }
}