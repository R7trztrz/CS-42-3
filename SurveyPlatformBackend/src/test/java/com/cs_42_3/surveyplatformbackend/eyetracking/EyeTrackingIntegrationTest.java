package com.cs_42_3.surveyplatformbackend.eyetracking;

import com.cs_42_3.surveyplatformbackend.TestcontainersConfiguration;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionTokenService;
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
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Drives the M6 endpoints through the real security chain, M5 session lifecycle and PostgreSQL.
 *
 * <p>The request bodies come from {@code src/test/resources/eyetracking}. Those files are the
 * serialized output of the frontend's FR-49 {@code CalibrationController} and FR-50
 * {@code GazeEventCollector}, produced with fixed clocks and ids, so these tests exercise the
 * exact wire format the browser sends rather than a hand-written approximation of it.
 */
@SpringBootTest(properties = {
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
class EyeTrackingIntegrationTest {
    static final String FIXTURE_SESSION_ID = "11111111-1111-4111-8111-111111111111";
    static final String FIXTURE_ATTEMPT_ID = "22222222-2222-4222-8222-222222222222";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void attemptsAreStoredIdempotentlyWithoutChangingThePhase() throws Exception {
        Participant participant = consentedParticipant(true);
        UUID attemptId = UUID.randomUUID();
        String attempt = attempt(participant, attemptId);

        String created = recordAttempt(participant, attemptId, attempt)
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.attemptId").value(attemptId.toString()))
                .andExpect(jsonPath("$.attemptNumber").value(1))
                .andReturn().getResponse().getContentAsString();
        String replayed = recordAttempt(participant, attemptId, attempt)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        assertThat(objectMapper.readTree(replayed).get("receivedAt"))
                .isEqualTo(objectMapper.readTree(created).get("receivedAt"));

        recordAttempt(participant, attemptId, edit(attempt, node -> node.put("attemptNumber", 2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PARTICIPANT_IDEMPOTENCY_CONFLICT"));
        recordAttempt(participant, UUID.randomUUID(), attempt)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"));

        assertThat(count("calibration_attempts", participant)).isOne();
        assertThat(count("calibration_point_residuals", participant)).isEqualTo(9);
        assertThat(count("calibration_completions", participant)).isZero();
        assertThat(phase(participant)).isEqualTo("CALIBRATION");
        Double residual = jdbc.queryForObject(
                "SELECT residual_median_px FROM calibration_attempts WHERE session_id = ?",
                Double.class,
                participant.sessionId()
        );
        assertThat(residual).isEqualTo(6.0);
    }

    @Test
    void completionStoresTheAttemptAndMovesTheSessionToBrowsingInOneTransaction() throws Exception {
        Participant participant = consentedParticipant(true);
        UUID attemptId = UUID.randomUUID();
        String attempt = attempt(participant, attemptId);
        // A stored attempt with other content makes the completion fail after M5 already moved
        // the session in memory; nothing of that transaction may survive.
        recordAttempt(participant, attemptId, edit(attempt, node -> node.put("attemptNumber", 2)))
                .andExpect(status().isCreated());
        completeCalibration(participant, attempt)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PARTICIPANT_IDEMPOTENCY_CONFLICT"));
        assertThat(phase(participant)).isEqualTo("CALIBRATION");
        assertThat(count("calibration_completions", participant)).isZero();

        UUID finalAttemptId = UUID.randomUUID();
        String finalAttempt = attempt(participant, finalAttemptId);
        completeCalibration(participant, finalAttempt)
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.phase").value("BROWSING"))
                .andExpect(jsonPath("$.calibrationCompletedAt").isNotEmpty());
        assertThat(jdbc.queryForObject(
                "SELECT attempt_id FROM calibration_completions WHERE session_id = ?",
                UUID.class,
                participant.sessionId()
        )).isEqualTo(finalAttemptId);

        completeCalibration(participant, finalAttempt)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("BROWSING"));
        completeCalibration(participant, attempt(participant, UUID.randomUUID()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PARTICIPANT_IDEMPOTENCY_CONFLICT"));
        assertThat(count("calibration_attempts", participant)).isEqualTo(2);

        // A delivery retried after the phase moved on still lands.
        UUID lateAttemptId = UUID.randomUUID();
        recordAttempt(participant, lateAttemptId, attempt(participant, lateAttemptId))
                .andExpect(status().isCreated());
    }

    @Test
    void anyTerminalOutcomeCompletesCalibrationButOnlyAUsableOneAcceptsGaze() throws Exception {
        Participant participant = consentedParticipant(true);
        UUID attemptId = UUID.randomUUID();
        String unavailable = edit(attempt(participant, attemptId), node -> {
            node.put("outcome", "UNAVAILABLE");
            node.put("trackingAvailable", false);
            node.put("unavailableReason", "PERMISSION_DENIED");
            node.put("quality", "NOT_MEASURED");
            node.put("qualityBasis", "NONE");
            node.set("lowQualityReasons", objectMapper.createArrayNode()
                    .add("ENGINE_UNAVAILABLE").add("NOT_MEASURED").add("INSUFFICIENT_SAMPLES"));
            node.putNull("residualMedianPx");
            node.put("calibrationTargetsMeasured", 0);
            node.set("pointResiduals", objectMapper.createArrayNode());
        });

        completeCalibration(participant, unavailable)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("BROWSING"));

        String batch = editEvents(batch(participant, attemptId), event -> {
            event.put("calibrationQuality", "NOT_MEASURED");
            event.set("calibrationLowQualityReasons", objectMapper.createArrayNode()
                    .add("ENGINE_UNAVAILABLE").add("NOT_MEASURED").add("INSUFFICIENT_SAMPLES"));
        });
        submitBatch(participant, batch)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.error").value(containsString("usable eye tracker")));
        assertThat(count("gaze_batches", participant)).isZero();
    }

    @Test
    void completionRequiresConsentAndAnEyeTrackingStudy() throws Exception {
        Participant beforeConsent = participant(true);
        completeCalibration(beforeConsent, attempt(beforeConsent, UUID.randomUUID()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PARTICIPANT_SESSION_STATE_INVALID"));
        UUID attemptId = UUID.randomUUID();
        recordAttempt(beforeConsent, attemptId, attempt(beforeConsent, attemptId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PARTICIPANT_SESSION_STATE_INVALID"));

        Participant withoutEyeTracking = consentedParticipant(false);
        assertThat(phase(withoutEyeTracking)).isEqualTo("BROWSING");
        completeCalibration(withoutEyeTracking, attempt(withoutEyeTracking, UUID.randomUUID()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PARTICIPANT_SESSION_STATE_INVALID"));

        Participant other = consentedParticipant(true);
        completeCalibration(other, attempt(beforeConsent, UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"));
        assertThat(count("calibration_attempts", other)).isZero();
    }

    @Test
    void gazeBatchesAreStoredWholeIdempotentlyAndRecordActivity() throws Exception {
        Participant participant = browsingParticipant();
        // Recent enough that the inactivity sweep never touches the session during the test.
        Instant oldActivity = Instant.now().minusSeconds(60).truncatedTo(ChronoUnit.MICROS);
        jdbc.update(
                "UPDATE participant_sessions SET last_activity_at = ? WHERE id = ?",
                Timestamp.from(oldActivity),
                participant.sessionId()
        );
        String batch = batch(participant, participant.attemptId());

        submitBatch(participant, batch)
                .andExpect(status().isCreated())
                .andExpect(header().string("Cache-Control", containsString("no-store")))
                .andExpect(jsonPath("$.sequence").value(1))
                .andExpect(jsonPath("$.eventCount").value(3));
        submitBatch(participant, batch)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sequence").value(1));
        submitBatch(participant, edit(batch, node -> node.put("batchId", UUID.randomUUID().toString())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PARTICIPANT_IDEMPOTENCY_CONFLICT"));

        List<String> rows = jdbc.query(
                """
                        SELECT attribution, widget_id, page_id, scroll_y, page_y, calibration_attempt_id
                        FROM gaze_events WHERE session_id = ? ORDER BY event_index
                        """,
                (rs, row) -> rs.getString("attribution") + "|" + rs.getString("widget_id") + "|"
                        + rs.getString("page_id") + "|" + rs.getDouble("scroll_y") + "|"
                        + rs.getDouble("page_y") + "|" + rs.getObject("calibration_attempt_id"),
                participant.sessionId()
        );
        String calibration = participant.attemptId().toString();
        assertThat(rows).containsExactly(
                "WIDGET|node-post-1:like|feed|120.0|180.0|" + calibration,
                "NO_CONTENT|null|feed|120.0|180.0|" + calibration,
                "UNRESOLVED|null|feed|120.0|180.0|" + calibration
        );
        assertThat(jdbc.queryForObject(
                "SELECT occurred_at FROM gaze_events WHERE session_id = ? AND event_index = 0",
                Timestamp.class,
                participant.sessionId()
        ).toInstant()).isEqualTo(Instant.parse("2023-11-14T22:13:20.020Z"));
        assertThat(jdbc.queryForObject(
                "SELECT last_activity_at FROM participant_sessions WHERE id = ?",
                Timestamp.class,
                participant.sessionId()
        ).toInstant()).isAfter(oldActivity);

        String second = edit(batch, node -> {
            node.put("sequence", 2);
            node.put("batchId", UUID.randomUUID().toString());
            node.put("droppedEvents", 4);
            node.put("droppedEventsTotal", 4);
        });
        submitBatch(participant, second).andExpect(status().isCreated());
        assertThat(count("gaze_batches", participant)).isEqualTo(2);
        assertThat(count("gaze_events", participant)).isEqualTo(6);
    }

    @Test
    void gazeBatchesAreRejectedOutsideTheCollectionPhases() throws Exception {
        Participant calibrating = consentedParticipant(true);
        UUID attemptId = UUID.randomUUID();
        recordAttempt(calibrating, attemptId, attempt(calibrating, attemptId))
                .andExpect(status().isCreated());
        submitBatch(calibrating, batch(calibrating, attemptId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PARTICIPANT_SESSION_STATE_INVALID"));

        Participant finished = browsingParticipant();
        mockMvc.perform(post("/api/participant-session/browsing-completion")
                        .header(ParticipantSessionTokenService.HEADER_NAME, finished.token()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"));
        submitBatch(finished, batch(finished, finished.attemptId()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("PARTICIPANT_SESSION_TERMINATED"));
        assertThat(count("gaze_batches", calibrating) + count("gaze_batches", finished)).isZero();
    }

    @Test
    void invalidGazeBatchesStoreNothing() throws Exception {
        Participant participant = browsingParticipant();
        String batch = batch(participant, participant.attemptId());

        submitBatch(participant, edit(batch, node -> node.put("eventCount", 2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("eventCount")));
        submitBatch(participant, editEvents(batch, event -> event.put("sessionId", UUID.randomUUID().toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("payload[0].sessionId")));
        submitBatch(participant, editEvents(batch, event ->
                event.put("calibrationAttemptId", UUID.randomUUID().toString())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("calibrationAttemptId")));
        submitBatch(participant, editEvents(batch, event -> event.put("calibrationQuality", "FAIR")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("calibration quality")));
        submitBatch(participant, editEvents(batch, event -> event.put("x", "NaN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("payload[0].x")));
        // The first event is a WIDGET hit; keeping its widgetId under another attribution is invalid.
        submitBatch(participant, editEvents(batch, event -> event.put("attribution", "NO_CONTENT")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(containsString("widgetId")));
        submitBatch(participant, edit(batch, node -> node.put("payload", "{\"not\":\"an array\"}")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"));
        submitBatch(participant, edit(batch, node -> node.put("sessionId", UUID.randomUUID().toString())))
                .andExpect(status().isBadRequest());
        submitBatch(participant, edit(batch, node -> node.put("sequence", 0)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"));

        assertThat(count("gaze_batches", participant)).isZero();
        assertThat(count("gaze_events", participant)).isZero();
    }

    @Test
    void eyeTrackingEndpointsRequireTheParticipantSessionToken() throws Exception {
        Participant participant = consentedParticipant(true);
        UUID attemptId = UUID.randomUUID();
        mockMvc.perform(put("/api/participant-session/calibration-attempts/{id}", attemptId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(attempt(participant, attemptId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("PARTICIPANT_SESSION_UNAUTHORIZED"));
        mockMvc.perform(post("/api/participant-session/gaze-batches")
                        .header(ParticipantSessionTokenService.HEADER_NAME, "x".repeat(43))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batch(participant, attemptId)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("PARTICIPANT_SESSION_UNAUTHORIZED"));
    }

    @Test
    void deletingTheStudyRemovesItsEyeTrackingRecords() throws Exception {
        Participant participant = browsingParticipant();
        submitBatch(participant, batch(participant, participant.attemptId()))
                .andExpect(status().isCreated());

        jdbc.update("DELETE FROM studies WHERE id = ?", participant.studyId());

        for (String table : List.of("calibration_attempts", "calibration_point_residuals",
                "calibration_completions", "gaze_batches", "gaze_events")) {
            assertThat(count(table, participant)).as(table).isZero();
        }
    }

    private ResultActions recordAttempt(Participant participant, UUID attemptId, String body)
            throws Exception {
        return mockMvc.perform(put("/api/participant-session/calibration-attempts/{id}", attemptId)
                .header(ParticipantSessionTokenService.HEADER_NAME, participant.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions completeCalibration(Participant participant, String body) throws Exception {
        return mockMvc.perform(post("/api/participant-session/calibration-completion")
                .header(ParticipantSessionTokenService.HEADER_NAME, participant.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private ResultActions submitBatch(Participant participant, String body) throws Exception {
        return mockMvc.perform(post("/api/participant-session/gaze-batches")
                .header(ParticipantSessionTokenService.HEADER_NAME, participant.token())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private Participant browsingParticipant() throws Exception {
        Participant participant = consentedParticipant(true);
        UUID attemptId = UUID.randomUUID();
        completeCalibration(participant, attempt(participant, attemptId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value("BROWSING"));
        return participant.withAttempt(attemptId);
    }

    private Participant consentedParticipant(boolean eyeTracking) throws Exception {
        Participant participant = participant(eyeTracking);
        mockMvc.perform(put("/api/participant-session/consent")
                        .header(ParticipantSessionTokenService.HEADER_NAME, participant.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"accepted\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phase").value(eyeTracking ? "CALIBRATION" : "BROWSING"));
        return participant;
    }

    private Participant participant(boolean eyeTracking) throws Exception {
        UUID studyId = UUID.randomUUID();
        String studyToken = study(studyId, eyeTracking);
        String response = mockMvc.perform(post("/api/participation/{token}/sessions", studyToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceInfo\":null}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        JsonNode created = objectMapper.readTree(response);
        return new Participant(
                studyId,
                UUID.fromString(created.get("sessionId").asString()),
                created.get("sessionToken").asString(),
                null
        );
    }

    private String study(UUID studyId, boolean eyeTracking) {
        UUID ownerId = UUID.randomUUID();
        byte[] bytes = new byte[32];
        ThreadLocalRandom.current().nextBytes(bytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update(
                """
                        INSERT INTO researchers (id, email, password_hash, role)
                        VALUES (?, ?, 'test-password-hash', 'RESEARCHER')
                        """,
                ownerId,
                ownerId + "@eye-tracking-test.invalid"
        );
        jdbc.update(
                """
                        INSERT INTO studies (
                            id, owner_id, title, description, status,
                            eye_tracking_enabled, questionnaire_enabled,
                            participation_token, published_at, consent_document_version
                        ) VALUES (?, ?, 'Eye tracking integration', 'Description', 'COLLECTING',
                                  ?, false, ?, ?, 'platform-default-v1')
                        """,
                studyId,
                ownerId,
                eyeTracking,
                token,
                Timestamp.from(Instant.parse("2026-10-03T00:00:00Z"))
        );
        jdbc.update(
                """
                        INSERT INTO study_feeds (study_id, template_code, theme, content, schema_version)
                        VALUES (?, 'facebook', 'facebook', CAST(? AS jsonb), 1)
                        """,
                studyId,
                "{\"ROOT\":{\"type\":\"div\"}}"
        );
        return token;
    }

    private String attempt(Participant participant, UUID attemptId) {
        return fixture("calibration-attempt.json")
                .replace(FIXTURE_SESSION_ID, participant.sessionId().toString())
                .replace(FIXTURE_ATTEMPT_ID, attemptId.toString());
    }

    private String batch(Participant participant, UUID attemptId) {
        return fixture("gaze-batch.json")
                .replace(FIXTURE_SESSION_ID, participant.sessionId().toString())
                .replace(FIXTURE_ATTEMPT_ID, attemptId.toString());
    }

    private String edit(String json, Consumer<ObjectNode> change) {
        ObjectNode node = (ObjectNode) objectMapper.readTree(json);
        change.accept(node);
        return objectMapper.writeValueAsString(node);
    }

    /** Applies a change to the first event inside the serialized payload. */
    private String editEvents(String batch, Consumer<ObjectNode> change) {
        return edit(batch, node -> {
            ArrayNode events = (ArrayNode) objectMapper.readTree(node.get("payload").asString());
            change.accept((ObjectNode) events.get(0));
            node.put("payload", objectMapper.writeValueAsString(events));
        });
    }

    private static String fixture(String name) {
        try {
            return new ClassPathResource("eyetracking/" + name).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private int count(String table, Participant participant) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE session_id = ?",
                Integer.class,
                participant.sessionId()
        );
        return count == null ? 0 : count;
    }

    private String phase(Participant participant) {
        return jdbc.queryForObject(
                "SELECT phase FROM participant_sessions WHERE id = ?",
                String.class,
                participant.sessionId()
        );
    }

    private record Participant(UUID studyId, UUID sessionId, String token, UUID attemptId) {
        Participant withAttempt(UUID attempt) {
            return new Participant(studyId, sessionId, token, attempt);
        }
    }
}
