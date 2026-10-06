package com.cs_42_3.surveyplatformbackend.participation.api;

import com.cs_42_3.surveyplatformbackend.common.exception.GlobalExceptionHandler;
import com.cs_42_3.surveyplatformbackend.config.SecurityConfig;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.ConsentDocumentResponse;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.CreateParticipantSessionRequest;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.CreateParticipantSessionResponse;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.ParticipantSessionResponse;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.ParticipantQuestionResponse;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.ParticipantQuestionnaireStateResponse;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantNoStoreFilter;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionPrincipal;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionTokenService;
import com.cs_42_3.surveyplatformbackend.participation.config.ParticipationConfiguration;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSessionPhase;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSessionStatus;
import com.cs_42_3.surveyplatformbackend.participation.exception.ParticipationException;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantSessionService;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantQuestionnaireService;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
    private ParticipantQuestionnaireService questionnaire;
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
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(jsonPath("$.sessionToken").value(SESSION_TOKEN))
                .andExpect(jsonPath("$.phase").value("CONSENT"));

        verify(sessions).create(any(String.class), any(CreateParticipantSessionRequest.class));
    }

    @Test
    void ignoresUnknownFieldsForForwardCompatibleParticipantRequests() throws Exception {
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
        when(sessions.decideConsent(PRINCIPAL, true)).thenReturn(state(PRINCIPAL.sessionId()));
        when(questionnaire.answer(any(), any(), any(), any()))
                .thenReturn(new ParticipantQuestionnaireStateResponse(null, true));

        mockMvc.perform(post("/api/participation/{token}/sessions", STUDY_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "deviceInfo": {
                                    "browser": "Chrome",
                                    "futureDeviceField": "accepted"
                                  },
                                  "futureSessionField": "accepted"
                                }
                                """))
                .andExpect(status().isCreated());

        mockMvc.perform(put("/api/participant-session/consent")
                        .header(ParticipantSessionTokenService.HEADER_NAME, SESSION_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accepted\":true,\"futureConsentField\":\"accepted\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(put(
                        "/api/participant-session/questionnaire/answers/{itemId}",
                        UUID.randomUUID()
                )
                        .header(ParticipantSessionTokenService.HEADER_NAME, SESSION_TOKEN)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"textValue\":\"answer\",\"futureAnswerField\":\"accepted\"}"))
                .andExpect(status().isOk());

        verify(sessions).create(any(String.class), any(CreateParticipantSessionRequest.class));
        verify(sessions).decideConsent(PRINCIPAL, true);
        verify(questionnaire).answer(any(), any(), any(), any());
    }

    @Test
    void participantRequestParsingFailuresExposeDocumentedCodes() throws Exception {
        mockMvc.perform(post("/api/participation/{token}/sessions", STUDY_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceInfo\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_BODY_INVALID"));

        mockMvc.perform(put("/api/participant-session/consent")
                        .header(ParticipantSessionTokenService.HEADER_NAME, SESSION_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accepted\":null}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"));

        mockMvc.perform(put(
                        "/api/participant-session/questionnaire/answers/{itemId}",
                        "not-a-uuid"
                )
                        .header(ParticipantSessionTokenService.HEADER_NAME, SESSION_TOKEN)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"textValue\":\"answer\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ARGUMENT_TYPE_MISMATCH"));

        mockMvc.perform(put(
                        "/api/participant-session/questionnaire/answers/{itemId}",
                        UUID.randomUUID()
                )
                        .header(ParticipantSessionTokenService.HEADER_NAME, SESSION_TOKEN)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"textValue\":\"answer\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_FAILED"));
    }

    @Test
    void rejectsMissingParticipantTokenWithParticipantSpecific401AndNoStore() throws Exception {
        mockMvc.perform(get("/api/participant-session"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("Cache-Control",
                        org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
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

    @Test
    void getsAndAnswersOnlyTheServerSelectedQuestion() throws Exception {
        UUID itemId = UUID.randomUUID();
        UUID key = UUID.randomUUID();
        ParticipantQuestionnaireStateResponse state = new ParticipantQuestionnaireStateResponse(
                new ParticipantQuestionResponse(
                        itemId, 1, QuestionType.TEXT, "Question", true,
                        java.util.List.of(), null, null, null, null
                ),
                false
        );
        when(questionnaire.current(PRINCIPAL)).thenReturn(state);
        when(questionnaire.answer(any(), any(), any(), any()))
                .thenReturn(new ParticipantQuestionnaireStateResponse(null, true));

        mockMvc.perform(get("/api/participant-session/questionnaire/current")
                        .header(ParticipantSessionTokenService.HEADER_NAME, SESSION_TOKEN))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentQuestion.itemId").value(itemId.toString()))
                .andExpect(jsonPath("$.currentQuestion.questionType").value("TEXT"));

        mockMvc.perform(put("/api/participant-session/questionnaire/answers/{itemId}", itemId)
                        .header(ParticipantSessionTokenService.HEADER_NAME, SESSION_TOKEN)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"textValue\":\"answer\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control",
                        org.hamcrest.Matchers.containsString("no-store")))
                .andExpect(jsonPath("$.readyToSubmit").value(true));

        verify(questionnaire).current(PRINCIPAL);
        verify(questionnaire).answer(
                org.mockito.ArgumentMatchers.eq(PRINCIPAL),
                org.mockito.ArgumentMatchers.eq(itemId),
                org.mockito.ArgumentMatchers.eq(key),
                any()
        );
    }

    @Test
    void finalSubmissionAndExplicitAbandonmentUseAuthenticatedSession() throws Exception {
        when(sessions.completeQuestionnaire(PRINCIPAL)).thenReturn(state(PRINCIPAL.sessionId()));
        when(sessions.abandon(PRINCIPAL)).thenReturn(state(PRINCIPAL.sessionId()));

        mockMvc.perform(post("/api/participant-session/questionnaire/submission")
                        .header(ParticipantSessionTokenService.HEADER_NAME, SESSION_TOKEN))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/participant-session/abandonment")
                        .header(ParticipantSessionTokenService.HEADER_NAME, SESSION_TOKEN))
                .andExpect(status().isOk());

        verify(sessions).completeQuestionnaire(PRINCIPAL);
        verify(sessions).abandon(PRINCIPAL);
    }

    @Test
    void answerFailuresExposeStableCodesWithoutEchoingTheAnswer() throws Exception {
        UUID itemId = UUID.randomUUID();
        UUID key = UUID.randomUUID();
        when(questionnaire.answer(any(), any(), any(), any()))
                .thenThrow(ParticipationException.answerInvalid());

        mockMvc.perform(put("/api/participant-session/questionnaire/answers/{itemId}", itemId)
                        .header(ParticipantSessionTokenService.HEADER_NAME, SESSION_TOKEN)
                        .header("Idempotency-Key", key)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"textValue\":\"private participant answer\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PARTICIPANT_ANSWER_INVALID"))
                .andExpect(jsonPath("$.error").value(
                        org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.containsString("private participant answer")
                        )
                ));
    }

    @Test
    void idempotencyConflictUsesConflictStatusAndStableCode() throws Exception {
        when(questionnaire.answer(any(), any(), any(), any()))
                .thenThrow(ParticipationException.idempotencyConflict());

        mockMvc.perform(put(
                        "/api/participant-session/questionnaire/answers/{itemId}",
                        UUID.randomUUID()
                )
                        .header(ParticipantSessionTokenService.HEADER_NAME, SESSION_TOKEN)
                        .header("Idempotency-Key", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"textValue\":\"different\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PARTICIPANT_IDEMPOTENCY_CONFLICT"));
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
