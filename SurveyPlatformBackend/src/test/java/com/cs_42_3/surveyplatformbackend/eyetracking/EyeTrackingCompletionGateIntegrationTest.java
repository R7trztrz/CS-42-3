package com.cs_42_3.surveyplatformbackend.eyetracking;

import com.cs_42_3.surveyplatformbackend.TestcontainersConfiguration;
import com.cs_42_3.surveyplatformbackend.eyetracking.service.EyeTrackingCollectionCompletionGate;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionTokenService;
import com.cs_42_3.surveyplatformbackend.participation.service.CollectionCompletionGate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Runs the participant flow under a non-development profile, where the M6 gate replaces M5's
 * no-op adapter, to show the production wiring starts and completes an eye-tracked session.
 */
@SpringBootTest(properties = {
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("eyetracking-gate")
@AutoConfigureMockMvc
@Testcontainers
class EyeTrackingCompletionGateIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private CollectionCompletionGate gate;
    @Autowired
    private ParticipantSessionTokenService tokens;

    @Test
    void eyeTrackedSessionsCompleteThroughTheProductionGate() throws Exception {
        assertThat(gate).isInstanceOf(EyeTrackingCollectionCompletionGate.class);
        String completed = calibratedSessionToken();

        mockMvc.perform(post("/api/participant-session/browsing-completion")
                        .header(ParticipantSessionTokenService.HEADER_NAME, completed))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }

    @Test
    void theGateRejectsAnEyeTrackedSessionWhoseCalibrationOutcomeIsMissing() throws Exception {
        String token = calibratedSessionToken();
        // Simulates a session that reached BROWSING without M6 persisting its outcome.
        jdbc.update("""
                DELETE FROM calibration_completions
                WHERE session_id = (SELECT id FROM participant_sessions WHERE session_token_hash = ?)
                """, tokens.hash(token));

        mockMvc.perform(post("/api/participant-session/browsing-completion")
                        .header(ParticipantSessionTokenService.HEADER_NAME, token))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PARTICIPANT_SESSION_STATE_INVALID"));
    }

    private String calibratedSessionToken() throws Exception {
        String studyToken = study();
        JsonNode created = objectMapper.readTree(mockMvc.perform(
                        post("/api/participation/{token}/sessions", studyToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"deviceInfo\":null}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString());
        String sessionId = created.get("sessionId").asString();
        String token = created.get("sessionToken").asString();
        mockMvc.perform(put("/api/participant-session/consent")
                        .header(ParticipantSessionTokenService.HEADER_NAME, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accepted\":true}"))
                .andExpect(jsonPath("$.phase").value("CALIBRATION"));
        String attempt = new ClassPathResource("eyetracking/calibration-attempt.json")
                .getContentAsString(StandardCharsets.UTF_8)
                .replace(EyeTrackingIntegrationTest.FIXTURE_SESSION_ID, sessionId)
                .replace(EyeTrackingIntegrationTest.FIXTURE_ATTEMPT_ID, UUID.randomUUID().toString());
        mockMvc.perform(post("/api/participant-session/calibration-completion")
                        .header(ParticipantSessionTokenService.HEADER_NAME, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(attempt))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("BROWSING"));
        return token;
    }

    private String study() {
        UUID ownerId = UUID.randomUUID();
        UUID studyId = UUID.randomUUID();
        byte[] bytes = new byte[32];
        ThreadLocalRandom.current().nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update("""
                INSERT INTO researchers (id, email, password_hash, role)
                VALUES (?, ?, 'test-password-hash', 'RESEARCHER')
                """, ownerId, ownerId + "@eye-tracking-gate.invalid");
        jdbc.update("""
                INSERT INTO studies (
                    id, owner_id, title, description, status,
                    eye_tracking_enabled, questionnaire_enabled,
                    participation_token, published_at, consent_document_version
                ) VALUES (?, ?, 'Eye tracking gate', 'Description', 'COLLECTING',
                          true, false, ?, ?, 'platform-default-v1')
                """, studyId, ownerId, token, Timestamp.from(Instant.parse("2026-10-03T00:00:00Z")));
        jdbc.update("""
                INSERT INTO study_feeds (study_id, template_code, theme, content, schema_version)
                VALUES (?, 'facebook', 'facebook', CAST(? AS jsonb), 1)
                """, studyId, "{\"ROOT\":{\"type\":\"div\"}}");
        return token;
    }
}
