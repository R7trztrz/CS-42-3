package com.cs_42_3.surveyplatformbackend.participation;

import com.cs_42_3.surveyplatformbackend.TestcontainersConfiguration;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.CreateParticipantSessionRequest;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.SubmitQuestionnaireAnswerRequest;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionTokenService;
import com.cs_42_3.surveyplatformbackend.participation.repository.ParticipantSessionRepository;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantQuestionnaireService;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantSessionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.participation.timeout-scheduler-enabled=false"
})
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Testcontainers
class ParticipantPerformanceIntegrationTest {
    private static final Instant PUBLISHED_AT = Instant.parse("2026-10-03T00:00:00Z");

    @Autowired
    private ParticipantSessionService sessionService;
    @Autowired
    private ParticipantQuestionnaireService questionnaire;
    @Autowired
    private ParticipantSessionTokenService tokens;
    @Autowired
    private ParticipantSessionRepository sessions;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @EnabledIfSystemProperty(named = "survey.performance.tests", matches = "true")
    void routesOneHundredPublishedQuestionsWithinThePerformanceBudget() {
        List<UUID> itemIds = new ArrayList<>();
        for (int index = 0; index < 100; index++) {
            itemIds.add(UUID.randomUUID());
        }
        Fixture fixture = fixture(itemIds);
        var created = sessionService.create(fixture.participationToken(),
                new CreateParticipantSessionRequest(null));
        var principal = tokens.authenticate(created.sessionToken()).orElseThrow();
        sessionService.decideConsent(principal, true);
        sessionService.completeBrowsing(principal);

        long started = System.nanoTime();
        long slowestAnswerNanos = 0;
        for (UUID itemId : itemIds) {
            long answerStarted = System.nanoTime();
            questionnaire.answer(
                    principal,
                    itemId,
                    UUID.randomUUID(),
                    new SubmitQuestionnaireAnswerRequest(null, null, null, "answer", false)
            );
            slowestAnswerNanos = Math.max(slowestAnswerNanos, System.nanoTime() - answerStarted);
        }
        Duration total = Duration.ofNanos(System.nanoTime() - started);

        assertThat(questionnaire.current(principal).readyToSubmit()).isTrue();
        assertThat(Duration.ofNanos(slowestAnswerNanos)).isLessThan(Duration.ofSeconds(1));
        assertThat(total).isLessThan(Duration.ofSeconds(15));
    }

    @Test
    @EnabledIfSystemProperty(named = "survey.performance.tests", matches = "true")
    void tokenHashLookupUsesTheUniqueIndexWithinThePerformanceBudget() {
        Fixture fixture = fixture(List.of());
        var created = sessionService.create(fixture.participationToken(),
                new CreateParticipantSessionRequest(null));
        insertSessions(fixture.studyId(), 2_000, Instant.now(), false);

        assertThat(tokens.authenticate(created.sessionToken())).isPresent();
        long started = System.nanoTime();
        for (int attempt = 0; attempt < 200; attempt++) {
            assertThat(tokens.authenticate(created.sessionToken())).isPresent();
        }
        Duration elapsed = Duration.ofNanos(System.nanoTime() - started);

        String plan = String.join("\n", jdbc.queryForList(
                "EXPLAIN (COSTS OFF) SELECT id FROM participant_sessions WHERE session_token_hash = ?",
                String.class,
                tokens.hash(created.sessionToken())
        ));
        assertThat(plan).contains("uq_participant_sessions_token_hash");
        assertThat(elapsed).isLessThan(Duration.ofSeconds(5));
    }

    @Test
    @EnabledIfSystemProperty(named = "survey.performance.tests", matches = "true")
    void timeoutCandidateScanUsesTheActivityIndexWithinThePerformanceBudget() {
        Fixture fixture = fixture(List.of());
        Instant oldActivity = Instant.parse("2026-01-01T00:00:00Z");
        insertSessions(fixture.studyId(), 3_000, oldActivity, true);
        Instant cutoff = Instant.parse("2026-02-01T00:00:00Z");

        assertThat(sessions.findTimeoutCandidates(cutoff, PageRequest.of(0, 100)))
                .hasSize(100);
        long started = System.nanoTime();
        for (int attempt = 0; attempt < 100; attempt++) {
            assertThat(sessions.findTimeoutCandidates(cutoff, PageRequest.of(0, 100)))
                    .hasSize(100);
        }
        Duration elapsed = Duration.ofNanos(System.nanoTime() - started);

        String plan = String.join("\n", jdbc.queryForList(
                """
                        EXPLAIN (COSTS OFF)
                        SELECT id, study_id
                        FROM participant_sessions
                        WHERE status = 'IN_PROGRESS' AND last_activity_at < ?
                        ORDER BY last_activity_at, id
                        LIMIT 100
                        """,
                String.class,
                Timestamp.from(cutoff)
        ));
        assertThat(plan).contains("idx_participant_sessions_status_activity");
        assertThat(elapsed).isLessThan(Duration.ofSeconds(5));
    }

    private Fixture fixture(List<UUID> itemIds) {
        UUID ownerId = UUID.randomUUID();
        UUID studyId = UUID.randomUUID();
        String participationToken = token();
        boolean questionnaireEnabled = !itemIds.isEmpty();
        jdbc.update(
                """
                        INSERT INTO researchers (id, email, password_hash, role)
                        VALUES (?, ?, 'test-password-hash', 'RESEARCHER')
                        """,
                ownerId,
                ownerId + "@performance-test.invalid"
        );
        jdbc.update(
                """
                        INSERT INTO studies (
                            id, owner_id, title, description, status,
                            eye_tracking_enabled, questionnaire_enabled,
                            participation_token, published_at, consent_document_version
                        ) VALUES (?, ?, 'Participant performance', 'Description', 'COLLECTING',
                                  false, ?, ?, ?, 'platform-default-v1')
                """,
                studyId,
                ownerId,
                questionnaireEnabled,
                participationToken,
                Timestamp.from(PUBLISHED_AT)
        );
        jdbc.update(
                """
                        INSERT INTO study_feeds (study_id, template_code, theme, content, schema_version)
                        VALUES (?, 'facebook', 'facebook', CAST(? AS jsonb), 1)
                        """,
                studyId,
                "{\"ROOT\":{\"type\":\"div\"}}"
        );
        if (questionnaireEnabled) {
            jdbc.update(
                    """
                            INSERT INTO questionnaire_publication_snapshots (
                                id, study_id, source_questionnaire_id,
                                questionnaire_version, published_at, content
                            ) VALUES (?, ?, ?, 0, ?, CAST(? AS jsonb))
                            """,
                    UUID.randomUUID(),
                    studyId,
                    UUID.randomUUID(),
                    Timestamp.from(PUBLISHED_AT),
                    snapshot(studyId, itemIds)
            );
        }
        return new Fixture(studyId, participationToken);
    }

    private String snapshot(UUID studyId, List<UUID> itemIds) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (int index = 0; index < itemIds.size(); index++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("itemId", itemIds.get(index));
            item.put("sourceQuestionId", UUID.randomUUID());
            item.put("position", index + 1);
            item.put("type", "TEXT");
            item.put("questionText", "Question " + (index + 1));
            item.put("required", true);
            item.put("options", List.of());
            item.put("scaleMin", null);
            item.put("scaleMax", null);
            item.put("scaleMinLabel", null);
            item.put("scaleMaxLabel", null);
            item.put("questionCreatedAt", PUBLISHED_AT);
            item.put("questionUpdatedAt", PUBLISHED_AT);
            item.put("defaultNextItemId",
                    index + 1 < itemIds.size() ? itemIds.get(index + 1) : null);
            item.put("branchRules", List.of());
            items.add(item);
        }
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("questionnaireId", UUID.randomUUID());
        snapshot.put("studyId", studyId);
        snapshot.put("version", 0);
        snapshot.put("sourceUpdatedAt", PUBLISHED_AT);
        snapshot.put("items", items);
        return objectMapper.writeValueAsString(snapshot);
    }

    private void insertSessions(
            UUID studyId,
            int count,
            Instant activityAt,
            boolean oldTokenRange
    ) {
        String sql = """
                INSERT INTO participant_sessions (
                    id, study_id, anonymous_participant_id, session_token_hash,
                    status, phase, entered_at, last_activity_at,
                    lock_version, created_at, updated_at
                ) VALUES (?, ?, ?, ?, 'IN_PROGRESS', 'CONSENT', ?, ?, 0, ?, ?)
                """;
        jdbc.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement statement, int index) throws SQLException {
                Instant enteredAt = activityAt.plusNanos(index * 1_000L);
                statement.setObject(1, UUID.randomUUID());
                statement.setObject(2, studyId);
                statement.setObject(3, UUID.randomUUID());
                statement.setString(4, String.format("%064x", index + (oldTokenRange ? 10_000 : 1)));
                statement.setTimestamp(5, Timestamp.from(enteredAt));
                statement.setTimestamp(6, Timestamp.from(enteredAt));
                statement.setTimestamp(7, Timestamp.from(enteredAt));
                statement.setTimestamp(8, Timestamp.from(enteredAt));
            }

            @Override
            public int getBatchSize() {
                return count;
            }
        });
    }

    private String token() {
        byte[] bytes = new byte[32];
        java.util.concurrent.ThreadLocalRandom.current().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record Fixture(UUID studyId, String participationToken) {
    }
}
