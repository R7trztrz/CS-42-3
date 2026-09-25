package com.cs_42_3.surveyplatformbackend.security.turnstile;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Map;

/**
 * Verifies Cloudflare Turnstile tokens.
 *
 * @author Jiale Chen
 */
@Service
public class TurnstileService {

    private static final String VERIFY_URL =
            "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private final String secret;
    private final RestClient restClient;

    public TurnstileService(
            @Value("${turnstile.secret}") String secret
    ) {
        this.secret = secret;
        this.restClient = RestClient.create();
    }

    public boolean verify(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        TurnstileResponse response = restClient.post()
                .uri(VERIFY_URL)
                .body(Map.of(
                        "secret", secret,
                        "response", token
                ))
                .retrieve()
                .body(TurnstileResponse.class);

        return response != null && response.success();
    }

    private record TurnstileResponse(boolean success) {
    }
}