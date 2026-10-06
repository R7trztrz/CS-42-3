package com.cs_42_3.surveyplatformbackend.security;

import com.cs_42_3.surveyplatformbackend.config.SecurityConfig;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionAuthenticationFilter;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionTokenService;
import com.cs_42_3.surveyplatformbackend.study.api.StudyController;
import com.cs_42_3.surveyplatformbackend.study.service.StudyService;
import com.cs_42_3.surveyplatformbackend.study.service.ParticipationLinks;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;


import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.UUID;

@WebMvcTest(StudyController.class)
@Import({SecurityConfig.class, ParticipantSessionAuthenticationFilter.class})
@TestPropertySource(properties = {
        "security.jwt.secret=Y/KkJGsSTMeS+u3PyY7AUbvWjoy6sozESc+bVJKlftw="
})
class SecurityWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudyService studyService;

    @MockitoBean
    private CurrentResearcher currentResearcher;

    @MockitoBean
    private ParticipationLinks participationLinks;

    @MockitoBean
    private ParticipantSessionTokenService participantSessionTokens;

    @Autowired
    private JwtEncoder jwtEncoder;

    /** Verifies that accessing a protected endpoint without a JWT returns 401. */
    @Test
    void shouldReturnUnauthorizedWhenJwtIsMissing() throws Exception {

        mockMvc.perform(get("/api/studies"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("AUTH_UNAUTHORIZED"))
                .andExpect(jsonPath("$.error")
                        .value("Authentication required."));
    }

    /** Verifies that an invalid JWT is rejected with 401 AUTH_UNAUTHORIZED. */
    @Test
    void shouldReturnUnauthorizedWhenJwtIsInvalid() throws Exception {

        mockMvc.perform(
                        get("/api/studies")
                                .header(
                                        "Authorization",
                                        "Bearer abc.def.ghi"
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("AUTH_UNAUTHORIZED"))
                .andExpect(jsonPath("$.error")
                        .value("Authentication required."));
    }

    /** Verifies that an expired JWT is rejected with 401 AUTH_UNAUTHORIZED. */
    @Test
    void shouldReturnUnauthorizedWhenJwtIsExpired() throws Exception {

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(UUID.randomUUID().toString())
                .issuedAt(now.minusSeconds(3600))
                .expiresAt(now.minusSeconds(1800))
                .claim("role", "RESEARCHER")
                .build();

        String expiredToken = jwtEncoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();

        mockMvc.perform(
                        get("/api/studies")
                                .header(
                                        "Authorization",
                                        "Bearer " + expiredToken
                                )
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("AUTH_UNAUTHORIZED"))
                .andExpect(jsonPath("$.error")
                        .value("Authentication required."));
    }

    /** Verifies that a non-researcher role is rejected with 403 AUTH_FORBIDDEN. */
    @Test
    void shouldReturnForbiddenWhenRoleIsNotResearcher() throws Exception {

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(UUID.randomUUID().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("role", "ADMIN")
                .build();

        String adminToken = jwtEncoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();

        mockMvc.perform(
                        get("/actuator/health")
                                .header(
                                        "Authorization",
                                        "Bearer " + adminToken
                                )
                )
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code")
                        .value("AUTH_FORBIDDEN"))
                .andExpect(jsonPath("$.error")
                        .value("Access denied."));
    }

}
