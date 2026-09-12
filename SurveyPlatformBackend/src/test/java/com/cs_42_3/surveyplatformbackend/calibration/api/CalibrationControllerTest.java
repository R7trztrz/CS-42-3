package com.cs_42_3.surveyplatformbackend.calibration.api;

import com.cs_42_3.surveyplatformbackend.calibration.api.security.ParticipantSessionAuthenticator;
import com.cs_42_3.surveyplatformbackend.calibration.domain.CalibrationOutcome;
import com.cs_42_3.surveyplatformbackend.calibration.domain.CalibrationRecord;
import com.cs_42_3.surveyplatformbackend.calibration.service.CalibrationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Verifies the participant authentication boundary of the FR-49 ingest endpoint.
 * The controller is driven standalone so that the researcher security chain is not involved.
 *
 * @author Shuo Gu
 */
class CalibrationControllerTest {

    private static final UUID SESSION_ID = UUID.fromString("11111111-1111-4111-8111-111111111111");
    private static final UUID OTHER_SESSION_ID = UUID.fromString("22222222-2222-4222-8222-222222222222");
    private static final String PATH = "/api/participant/sessions/" + SESSION_ID + "/calibration";

    private CalibrationService calibrationService;
    private ObjectProvider<ParticipantSessionAuthenticator> authenticatorProvider;
    private ObjectMapper objectMapper;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        calibrationService = mock(CalibrationService.class);
        authenticatorProvider = mock(ObjectProvider.class);
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

        CalibrationRecord stored = new CalibrationRecord(SESSION_ID, 1, CalibrationOutcome.ABANDONED,
                null, null, null, 2268, 1293, 2.0,
                Instant.parse("2026-09-12T04:11:02.310Z"), Instant.parse("2026-09-12T04:12:18.774Z"));
        when(calibrationService.submitAttempt(any(), any()))
                .thenReturn(new CalibrationService.SubmissionResult(stored, true));
    }

    @Test
    @DisplayName("Without a participant authenticator the endpoint refuses to store anything")
    void refusesWhenAuthenticationIsNotConfigured() throws Exception {
        when(authenticatorProvider.getIfAvailable()).thenReturn(null);

        mockMvc().perform(post(PATH)
                        .header("X-Participant-Token", "any-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized());

        verify(calibrationService, never()).submitAttempt(any(), any());
    }

    @Test
    @DisplayName("A missing participant token is rejected")
    void rejectsMissingToken() throws Exception {
        when(authenticatorProvider.getIfAvailable()).thenReturn(token -> Optional.of(SESSION_ID));

        mockMvc().perform(post(PATH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized());

        verify(calibrationService, never()).submitAttempt(any(), any());
    }

    @Test
    @DisplayName("An unresolvable participant token is rejected")
    void rejectsUnknownToken() throws Exception {
        when(authenticatorProvider.getIfAvailable()).thenReturn(token -> Optional.empty());

        mockMvc().perform(post(PATH)
                        .header("X-Participant-Token", "stale-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized());

        verify(calibrationService, never()).submitAttempt(any(), any());
    }

    @Test
    @DisplayName("A token belonging to another session cannot write to this one")
    void rejectsCrossSessionWrite() throws Exception {
        when(authenticatorProvider.getIfAvailable()).thenReturn(token -> Optional.of(OTHER_SESSION_ID));

        mockMvc().perform(post(PATH)
                        .header("X-Participant-Token", "other-session-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden());

        verify(calibrationService, never()).submitAttempt(any(), any());
    }

    @Test
    @DisplayName("A valid token stores the attempt under the token's session")
    void storesAttemptForResolvedSession() throws Exception {
        when(authenticatorProvider.getIfAvailable()).thenReturn(token -> Optional.of(SESSION_ID));

        mockMvc().perform(post(PATH)
                        .header("X-Participant-Token", "good-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isCreated());

        verify(calibrationService).submitAttempt(eq(SESSION_ID), any());
    }

    private MockMvc mockMvc() {
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter();
        converter.setObjectMapper(objectMapper);
        return MockMvcBuilders
                .standaloneSetup(new CalibrationController(calibrationService, authenticatorProvider))
                .setMessageConverters(converter)
                .build();
    }

    private String validBody() throws Exception {
        return objectMapper.writeValueAsString(Map.of(
                "attemptNumber", 1,
                "outcome", "ABANDONED",
                "viewportWidth", 2268,
                "viewportHeight", 1293,
                "devicePixelRatio", 2.0,
                "startedAt", "2026-09-12T04:11:02.310Z",
                "finishedAt", "2026-09-12T04:12:18.774Z",
                "pointResiduals", java.util.List.of()));
    }
}
