package com.cs_42_3.surveyplatformbackend.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Provides the identity of the currently authenticated researcher.
 *
 * @author Jiale Chen
 */
@Component
public class CurrentResearcher {

    /**
     * Returns the UUID of the currently authenticated researcher.
     *
     * @return authenticated researcher UUID
     */
    public UUID getId() {
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (!(authentication instanceof JwtAuthenticationToken token)
                || !token.isAuthenticated()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required."
            );
        }

        String researcherId = token.getToken().getSubject();

        try {
            UUID id = UUID.fromString(researcherId);

            if (!id.toString().equalsIgnoreCase(researcherId)) {
                throw new IllegalArgumentException(
                        "Noncanonical UUID subject."
                );
            }

            return id;

        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Invalid authentication identity."
            );
        }
    }
}