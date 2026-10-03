package com.cs_42_3.surveyplatformbackend.participation;

import com.cs_42_3.surveyplatformbackend.TestcontainersConfiguration;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.CreateParticipantSessionRequest;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.ParticipantDeviceInfoRequest;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionPrincipal;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionTokenService;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantAbandonmentReason;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSessionPhase;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSessionStatus;
import com.cs_42_3.surveyplatformbackend.participation.exception.ParticipationException;
import com.cs_42_3.surveyplatformbackend.participation.repository.ParticipantSessionRepository;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantCalibrationLifecyclePort;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantSessionService;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyClosedException;
import com.cs_42_3.surveyplatformbackend.study.exception.FeedNotReadyException;
import com.cs_42_3.surveyplatformbackend.study.exception.ParticipationNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Import(TestcontainersConfiguration.class)
@Testcontainers
class ParticipantSessionIntegrationTest {
    @Autowired
    private ParticipantSessionService sessionService;
    @Autowired
    private ParticipantCalibrationLifecyclePort calibrationLifecycle;
    @Autowired
    private ParticipantSessionRepository sessions;
    @Autowired
    private ParticipantSessionTokenService tokens;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void createsAuthenticatesRestoresAndCompletesAStudyWithoutQuestionnaire() {
        Fixture fixture = fixture(false, false, true);

        var created = sessionService.create(fixture.token(), request());
        ParticipantSessionPrincipal principal = tokens.authenticate(created.sessionToken()).orElseThrow();

        assertThat(created.sessionToken()).hasSize(43);
        assertThat(created.consentDocument().approvedForProduction()).isFalse();
        assertThat(principal.sessionId()).isEqualTo(created.sessionId());
        var persisted = sessions.findById(created.sessionId()).orElseThrow();
        assertThat(persisted.getSessionTokenHash())
                .isEqualTo(tokens.hash(created.sessionToken()))
                .matches("[0-9a-f]{64}")
                .isNotEqualTo(created.sessionToken());
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM participant_sessions WHERE session_token_hash = ?",
                Integer.class,
                created.sessionToken()
        )).isZero();

        var consented = sessionService.decideConsent(principal, true);
        assertThat(consented.phase()).isEqualTo(ParticipantSessionPhase.BROWSING);
        Instant consentedAt = consented.consentedAt();
        assertThat(sessionService.decideConsent(principal, true).consentedAt()).isEqualTo(consentedAt);

        var completed = sessionService.completeBrowsing(principal);
        assertThat(completed.status()).isEqualTo(ParticipantSessionStatus.COMPLETED);
        assertThat(completed.phase()).isEqualTo(ParticipantSessionPhase.FINISHED);
        Instant completedAt = completed.completedAt();
        assertThat(sessionService.completeBrowsing(principal).completedAt()).isEqualTo(completedAt);
        assertThat(sessionService.getCurrent(principal).status())
                .isEqualTo(ParticipantSessionStatus.COMPLETED);
    }

    @Test
    void decliningConsentClearsParticipantProvidedDeviceDataAndIsIdempotent() {
        Fixture fixture = fixture(true, false, true);
        var created = sessionService.create(fixture.token(), request());
        ParticipantSessionPrincipal principal = tokens.authenticate(created.sessionToken()).orElseThrow();

        var declined = sessionService.decideConsent(principal, false);
        assertThat(declined.status()).isEqualTo(ParticipantSessionStatus.ABANDONED);
        assertThat(declined.phase()).isEqualTo(ParticipantSessionPhase.FINISHED);
        assertThat(declined.abandonmentReason())
                .isEqualTo(ParticipantAbandonmentReason.CONSENT_DECLINED);
        var persisted = sessions.findById(created.sessionId()).orElseThrow();
        assertThat(persisted.getDeviceInfo()).isNull();
        assertThat(persisted.getConsentedAt()).isNull();
        assertThat(sessionService.decideConsent(principal, false).abandonedAt())
                .isEqualTo(declined.abandonedAt());
        assertThatThrownBy(() -> sessionService.decideConsent(principal, true))
                .isInstanceOf(ParticipationException.class);
    }

    @Test
    void calibrationPortJoinsThePersistedStateMachineAndIsIdempotent() {
        Fixture fixture = fixture(true, false, true);
        var created = sessionService.create(fixture.token(), request());
        ParticipantSessionPrincipal principal = tokens.authenticate(created.sessionToken()).orElseThrow();

        assertThat(sessionService.decideConsent(principal, true).phase())
                .isEqualTo(ParticipantSessionPhase.CALIBRATION);
        calibrationLifecycle.completeCalibration(created.sessionId());
        Instant completedAt = sessions.findById(created.sessionId()).orElseThrow()
                .getCalibrationCompletedAt();
        calibrationLifecycle.completeCalibration(created.sessionId());

        var restored = sessionService.getCurrent(principal);
        assertThat(restored.phase()).isEqualTo(ParticipantSessionPhase.BROWSING);
        assertThat(restored.calibrationCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    void questionnaireStudyUsesTheLowestPublishedPositionAsItsFirstItem() {
        Fixture fixture = fixture(false, true, true);
        var created = sessionService.create(fixture.token(), request());
        ParticipantSessionPrincipal principal = tokens.authenticate(created.sessionToken()).orElseThrow();
        sessionService.decideConsent(principal, true);

        var questionnaire = sessionService.completeBrowsing(principal);

        assertThat(questionnaire.phase()).isEqualTo(ParticipantSessionPhase.QUESTIONNAIRE);
        assertThat(questionnaire.currentQuestionItemId()).isEqualTo(fixture.firstItemId());
        assertThat(questionnaire.status()).isEqualTo(ParticipantSessionStatus.IN_PROGRESS);
    }

    @Test
    void missingQuestionnaireSnapshotAndClosedStudyCreateNoSessions() {
        Fixture missingSnapshot = fixture(false, true, false);
        Fixture closed = fixture(false, false, true);
        jdbc.update("UPDATE studies SET status = 'CLOSED' WHERE id = ?", closed.studyId());

        assertThatThrownBy(() -> sessionService.create(missingSnapshot.token(), request()))
                .isInstanceOf(ParticipationException.class)
                .extracting(exception -> ((ParticipationException) exception).getCode().code())
                .isEqualTo("PARTICIPANT_QUESTIONNAIRE_NOT_READY");
        assertThatThrownBy(() -> sessionService.create(closed.token(), request()))
                .isInstanceOf(StudyClosedException.class);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM participant_sessions WHERE study_id IN (?, ?)",
                Integer.class,
                missingSnapshot.studyId(),
                closed.studyId()
        )).isZero();
    }

    @Test
    void invalidDraftAndFeedlessStudiesCreateNoSessions() {
        Fixture draft = fixture(false, false, true);
        Fixture feedless = fixture(false, false, true);
        jdbc.update(
                """
                        UPDATE studies
                        SET status = 'DRAFT', participation_token = NULL,
                            published_at = NULL, consent_document_version = NULL
                        WHERE id = ?
                        """,
                draft.studyId()
        );
        jdbc.update("DELETE FROM study_feeds WHERE study_id = ?", feedless.studyId());

        assertThatThrownBy(() -> sessionService.create("invalid", request()))
                .isInstanceOf(ParticipationNotFoundException.class);
        assertThatThrownBy(() -> sessionService.create(draft.token(), request()))
                .isInstanceOf(ParticipationNotFoundException.class);
        assertThatThrownBy(() -> sessionService.create(feedless.token(), request()))
                .isInstanceOf(FeedNotReadyException.class);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM participant_sessions WHERE study_id IN (?, ?)",
                Integer.class,
                draft.studyId(),
                feedless.studyId()
        )).isZero();
    }

    @Test
    void databaseRejectsInconsistentTerminalStateAndStudyDeletionCascades() {
        Fixture fixture = fixture(false, false, true);
        var created = sessionService.create(fixture.token(), request());

        assertThatThrownBy(() -> jdbc.update(
                "UPDATE participant_sessions SET status = 'COMPLETED' WHERE id = ?",
                created.sessionId()
        )).isInstanceOf(DataIntegrityViolationException.class);

        jdbc.update("DELETE FROM studies WHERE id = ?", fixture.studyId());
        assertThat(sessions.findById(created.sessionId())).isEmpty();
    }

    @Test
    void oneTokenCannotSelectAnotherSession() {
        Fixture firstStudy = fixture(false, false, true);
        Fixture secondStudy = fixture(false, false, true);
        var first = sessionService.create(firstStudy.token(), request());
        var second = sessionService.create(secondStudy.token(), request());
        ParticipantSessionPrincipal firstPrincipal = tokens.authenticate(first.sessionToken()).orElseThrow();
        ParticipantSessionPrincipal secondPrincipal = tokens.authenticate(second.sessionToken()).orElseThrow();

        assertThat(sessionService.getCurrent(firstPrincipal).sessionId()).isEqualTo(first.sessionId());
        assertThat(sessionService.getCurrent(secondPrincipal).sessionId()).isEqualTo(second.sessionId());
        ParticipantSessionPrincipal forged = new ParticipantSessionPrincipal(
                firstPrincipal.sessionId(), secondPrincipal.studyId()
        );
        assertThatThrownBy(() -> sessionService.getCurrent(forged))
                .isInstanceOf(ParticipationException.class)
                .extracting(exception -> ((ParticipationException) exception).getCode().code())
                .isEqualTo("PARTICIPANT_SESSION_NOT_FOUND");
    }

    private CreateParticipantSessionRequest request() {
        return new CreateParticipantSessionRequest(new ParticipantDeviceInfoRequest(
                "Chrome",
                "140",
                "Windows",
                1920,
                1080,
                "Australia/Sydney"
        ));
    }

    private Fixture fixture(
            boolean eyeTrackingEnabled,
            boolean questionnaireEnabled,
            boolean addSnapshot
    ) {
        UUID ownerId = UUID.randomUUID();
        UUID studyId = UUID.randomUUID();
        UUID firstItemId = questionnaireEnabled ? UUID.randomUUID() : null;
        String token = token();
        jdbc.update(
                """
                        INSERT INTO researchers (id, email, password_hash, role)
                        VALUES (?, ?, 'test-password-hash', 'RESEARCHER')
                        """,
                ownerId,
                ownerId + "@participant-test.invalid"
        );
        jdbc.update(
                """
                        INSERT INTO studies (
                            id, owner_id, title, description, status,
                            eye_tracking_enabled, questionnaire_enabled,
                            participation_token, published_at, consent_document_version
                        ) VALUES (?, ?, 'Participant integration', 'Description', 'COLLECTING',
                                  ?, ?, ?, ?, 'platform-default-v1')
                        """,
                studyId,
                ownerId,
                eyeTrackingEnabled,
                questionnaireEnabled,
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
        if (questionnaireEnabled && addSnapshot) {
            UUID secondItemId = UUID.randomUUID();
            String content = """
                    {
                      "questionnaireId":"%s",
                      "studyId":"%s",
                      "version":0,
                      "sourceUpdatedAt":"2026-10-03T00:00:00Z",
                      "items":[
                        {
                          "itemId":"%s","sourceQuestionId":"%s","position":2,
                          "type":"TEXT","questionText":"Second","required":false,
                          "options":[],"scaleMin":null,"scaleMax":null,
                          "scaleMinLabel":null,"scaleMaxLabel":null,
                          "questionCreatedAt":"2026-10-03T00:00:00Z",
                          "questionUpdatedAt":"2026-10-03T00:00:00Z",
                          "defaultNextItemId":null,"branchRules":[]
                        },
                        {
                          "itemId":"%s","sourceQuestionId":"%s","position":1,
                          "type":"TEXT","questionText":"First","required":true,
                          "options":[],"scaleMin":null,"scaleMax":null,
                          "scaleMinLabel":null,"scaleMaxLabel":null,
                          "questionCreatedAt":"2026-10-03T00:00:00Z",
                          "questionUpdatedAt":"2026-10-03T00:00:00Z",
                          "defaultNextItemId":"%s","branchRules":[]
                        }
                      ]
                    }
                    """.formatted(
                    UUID.randomUUID(),
                    studyId,
                    secondItemId,
                    UUID.randomUUID(),
                    firstItemId,
                    UUID.randomUUID(),
                    secondItemId
            );
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
                    Timestamp.from(Instant.parse("2026-10-03T00:00:00Z")),
                    content
            );
        }
        return new Fixture(studyId, token, firstItemId);
    }

    private String token() {
        byte[] bytes = new byte[32];
        java.util.concurrent.ThreadLocalRandom.current().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record Fixture(UUID studyId, String token, UUID firstItemId) {
    }
}
