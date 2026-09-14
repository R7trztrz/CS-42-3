package com.cs_42_3.surveyplatformbackend.survey.security;

import com.cs_42_3.surveyplatformbackend.survey.exception.InvalidResearcherIdentityException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for the JWT subject contract used by {@link CurrentResearcherProvider}.
 */
class CurrentResearcherProviderTest {

    private final CurrentResearcherProvider currentResearcherProvider =
            new CurrentResearcherProvider();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentResearcherIdReturnsUuidFromJwtSubject() {
        UUID researcherId = UUID.fromString("9e490694-f8df-411d-9a82-cb2bab07ab9e");
        setAuthentication(researcherId.toString());

        UUID result = currentResearcherProvider.getCurrentResearcherId();

        assertThat(result).isEqualTo(researcherId);
    }

    @Test
    void getCurrentResearcherIdRejectsMissingAuthentication() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(currentResearcherProvider::getCurrentResearcherId)
                .isInstanceOf(InvalidResearcherIdentityException.class)
                .hasMessageContaining("No authenticated researcher");
    }

    @Test
    void getCurrentResearcherIdRejectsMalformedUuidSubject() {
        setAuthentication("not-a-uuid");

        assertThatThrownBy(currentResearcherProvider::getCurrentResearcherId)
                .isInstanceOf(InvalidResearcherIdentityException.class)
                .hasMessageContaining("not a UUID");
    }

    @Test
    void getCurrentResearcherIdRejectsMissingSubject() {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        setAuthentication(jwt);

        assertThatThrownBy(currentResearcherProvider::getCurrentResearcherId)
                .isInstanceOf(InvalidResearcherIdentityException.class)
                .hasMessageContaining("subject is missing");
    }

    private void setAuthentication(String subject) {
        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject(subject)
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build();
        setAuthentication(jwt);
    }

    private void setAuthentication(Jwt jwt) {
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                jwt,
                List.of(new SimpleGrantedAuthority("ROLE_RESEARCHER"))
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
    }
}
