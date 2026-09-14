package com.cs_42_3.surveyplatformbackend.survey.security;

import com.cs_42_3.surveyplatformbackend.survey.exception.InvalidResearcherIdentityException;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Resolves the current researcher's UUID from the authenticated JWT subject.
 */
@Component
public class CurrentResearcherProvider {

    /**
     * Returns the UUID stored in the current JWT's {@code sub} claim.
     *
     * @return the authenticated researcher's UUID
     * @throws InvalidResearcherIdentityException if the principal or subject is invalid
     */
    public UUID getCurrentResearcherId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuthentication)
                || !authentication.isAuthenticated()) {
            throw new InvalidResearcherIdentityException("No authenticated researcher is available");
        }

        String subject = jwtAuthentication.getToken().getSubject();
        if (subject == null || subject.isBlank()) {
            throw new InvalidResearcherIdentityException("Authenticated researcher subject is missing");
        }

        try {
            return UUID.fromString(subject);
        } catch (IllegalArgumentException exception) {
            throw new InvalidResearcherIdentityException("Authenticated researcher subject is not a UUID");
        }
    }
}
