package com.cs_42_3.surveyplatformbackend.participation.api;

import com.cs_42_3.surveyplatformbackend.common.exception.GlobalExceptionHandler;
import com.cs_42_3.surveyplatformbackend.config.SecurityConfig;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.ConsentDocumentResponse;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.CreateParticipantSessionRequest;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.CreateParticipantSessionResponse;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.ParticipantSessionResponse;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantNoStoreFilter;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionPrincipal;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionTokenService;
import com.cs_42_3.surveyplatformbackend.participation.config.ParticipationConfiguration;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSessionPhase;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSessionStatus;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantSessionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ParticipantSessionController.class)
@Import({
        SecurityConfig.class,
        ParticipantNoStoreFilter.class,
        ParticipationConfiguration.class,
        GlobalExceptionHandler.class
})
@TestPropertySource(properties = {
        "security.jwt.secret=Y/KkJGsSTMeS+u3PyY7AUbvWjoy6sozESc+bVJKlftw="
})
class ParticipantSessionControllerTest {
    private static final String STUDY_TOKEN = "s".repeat(43);
    private static final String SESSION_TOKEN = "a".repeat(43);
    private static final Instant NOW = Instant.parse("2026-10-03T00:00:00Z");
    private static final ParticipantSessionPrincipal PRINCIPAL =
            new ParticipantSessionPrincipal(UUID.randomUUID(), UUID.randomUUID());

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JwtEncoder jwtEncoder;
    @MockitoBean
    private ParticipantSessionService sessions;
    @MockitoBean
    private ParticipantSessionTokenService tokens;

    @BeforeEach
    void authenticateParticipantToken() {
        when(tokens.authenticate(SESSION_TOKEN)).thenReturn(Optional.of(PRINCIPAL));
    }

    @Test
    void createsSessionAnonymouslyAndReturnsTokenOnceWithNoStore() throws Exception {
        when(sessions.create(any(), any())).thenReturn(new CreateParticipantSessionResponse(
                PRINCIPAL.sessionId(),
                SESSION_TOKEN,
                ParticipantSessionStatus.IN_PROGRESS,
                ParticipantSessionPhase.CONSENT,
                "Study",
                "Description",
                false,
                false,
                consent(),
                NOW
        ));

        mockMvc.perform(post("/api/participation/{token}/sessions", STUDY_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceInfo\":{\"browser\":\"Chrome\"}}"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control",
                        org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.sessionToken").value(SESSION_TOKEN))
                .andExpect(jsonPath("$.phase").value("CONSENT"));

        verify(sessions).create(any(String.class), any(CreateParticipantSessionRequest.class));
    }

    @Test
    void rejectsMissingParticipantTokenWithParticipantSpecific401AndNoStore() throws Exception {
        mockMvc.perform(get("/api/participant-session"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Cache-Control",
                        org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.code").value("PARTICIPANT_SESSION_UNAUTHORIZED"));
    }

    @Test
    void researcherJwtCannotSubstituteForParticipantToken() throws Exception {
        Instant now = Instant.now();
        String jwt = jwtEncoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder()
                        .subject(UUID.randomUUID().toString())
                        .issuedAt(now)
                        .expiresAt(now.plusSeconds(600))
                        .claim("role", "RESEARCHER")
                        .build()))
                .getTokenValue();

        mockMvc.perform(get("/api/participant-session")
                        .header("Authorization", "Bearer " + jwt))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("PARTICIPANT_SESSION_UNAUTHORIZED"));
    }

    @Test
    void validParticipantTokenRestoresOnlyItsPrincipalSession() throws Exception {
        when(sessions.getCurrent(PRINCIPAL)).thenReturn(state(PRINCIPAL.sessionId()));

        mockMvc.perform(get("/api/participant-session")
                        .header(ParticipantSessionTokenService.HEADER_NAME, SESSION_TOKEN))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control",
                        org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.sessionId").value(PRINCIPAL.sessionId().toString()));

        verify(sessions).getCurrent(PRINCIPAL);
    }

    @Test
    void corsAllowsOnlyTheParticipantContractHeaders() throws Exception {
        mockMvc.perform(options("/api/participant-session")
                        .header("Origin", "http://localhost:5173")
                        .header("Access-Control-Request-Method", "GET")
                        .header("Access-Control-Request-Headers",
                                "X-Participant-Session-Token, Idempotency-Key"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:5173"))
                .andExpect(header().string("Access-Control-Allow-Headers",
                        org.hamcrest.Matchers.containsString("X-Participant-Session-Token")));
    }

    private ParticipantSessionResponse state(UUID sessionId) {
        return new ParticipantSessionResponse(
                sessionId,
                ParticipantSessionStatus.IN_PROGRESS,
                ParticipantSessionPhase.CONSENT,
                null,
                null,
                false,
                true,
                "Study",
                "Description",
                false,
                false,
                consent(),
                NOW,
                null,
                null,
                null,
                null,
                null,
                NOW
        );
    }

    private ConsentDocumentResponse consent() {
        return new ConsentDocumentResponse(
                "platform-default-v1",
                "Pending",
                "NON-PRODUCTION PLACEHOLDER",
                false
        );
    }
}
