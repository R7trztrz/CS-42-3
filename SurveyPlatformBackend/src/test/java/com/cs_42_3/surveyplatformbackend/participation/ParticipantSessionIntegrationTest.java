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
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantCollectionActivityPort;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantCollectionPolicy;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantSessionService;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyClosedException;
import com.cs_42_3.surveyplatformbackend.study.exception.FeedNotReadyException;
import com.cs_42_3.surveyplatformbackend.study.exception.ParticipationNotFoundException;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.sql.DataSource;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Import({TestcontainersConfiguration.class, ParticipationGateTestConfiguration.class})
@ActiveProfiles("test")
@Testcontainers
class ParticipantSessionIntegrationTest {
    @Autowired
    private ParticipantSessionService sessionService;
    @Autowired
    private ParticipantCalibrationLifecyclePort calibrationLifecycle;
    @Autowired
    private ParticipantCollectionPolicy collectionPolicy;
    @Autowired
    private ParticipantCollectionActivityPort collectionActivity;
    @Autowired
    private ParticipantSessionRepository sessions;
    @Autowired
    private StudyRepository studies;
    @Autowired
    private ParticipantSessionTokenService tokens;
    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private ParticipationGateTestConfiguration.RecordingCollectionCompletionGate completionGate;

    @BeforeEach
    void resetCompletionGate() {
        completionGate.reset();
    }

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
        assertThat(completionGate.browsingChecks()).isOne();
        assertThat(completionGate.sessionChecks()).isOne();
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
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM participant_questionnaire_steps WHERE session_id = ?",
                Integer.class,
                created.sessionId()
        )).isZero();
        assertThatThrownBy(() -> inTransaction(() -> collectionPolicy.assertCollectionAllowed(
                created.sessionId(), ParticipantCollectionPolicy.CollectionKind.BEHAVIOR
        ))).isInstanceOf(ParticipationException.class);
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
        assertThatThrownBy(() -> calibrationLifecycle.completeCalibration(created.sessionId()))
                .isInstanceOf(IllegalTransactionStateException.class);

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            calibrationLifecycle.completeCalibration(created.sessionId());
            status.setRollbackOnly();
        });
        assertThat(sessions.findById(created.sessionId()).orElseThrow().getPhase())
                .isEqualTo(ParticipantSessionPhase.CALIBRATION);

        transaction.executeWithoutResult(status ->
                calibrationLifecycle.completeCalibration(created.sessionId()));
        Instant completedAt = sessions.findById(created.sessionId()).orElseThrow()
                .getCalibrationCompletedAt();
        transaction.executeWithoutResult(status ->
                calibrationLifecycle.completeCalibration(created.sessionId()));

        var restored = sessionService.getCurrent(principal);
        assertThat(restored.phase()).isEqualTo(ParticipantSessionPhase.BROWSING);
        assertThat(restored.calibrationCompletedAt()).isEqualTo(completedAt);
    }

    @Test
    void collectionPolicyEnforcesConsentPhaseEyeTrackingAndCallerTransactions() {
        Fixture eyeTracked = fixture(true, false, true);
        var eyeSession = sessionService.create(eyeTracked.token(), request());
        ParticipantSessionPrincipal eyePrincipal = tokens.authenticate(
                eyeSession.sessionToken()
        ).orElseThrow();

        assertThatThrownBy(() -> collectionPolicy.assertCollectionAllowed(
                eyeSession.sessionId(), ParticipantCollectionPolicy.CollectionKind.BEHAVIOR
        )).isInstanceOf(IllegalTransactionStateException.class);
        assertThatThrownBy(() -> inTransaction(() -> collectionPolicy.assertCollectionAllowed(
                eyeSession.sessionId(), ParticipantCollectionPolicy.CollectionKind.BEHAVIOR
        ))).isInstanceOf(ParticipationException.class);

        sessionService.decideConsent(eyePrincipal, true);
        assertThatThrownBy(() -> inTransaction(() -> collectionPolicy.assertCollectionAllowed(
                eyeSession.sessionId(), ParticipantCollectionPolicy.CollectionKind.GAZE
        ))).isInstanceOf(ParticipationException.class);
        inTransaction(() -> calibrationLifecycle.completeCalibration(eyeSession.sessionId()));
        inTransaction(() -> collectionPolicy.assertCollectionAllowed(
                eyeSession.sessionId(), ParticipantCollectionPolicy.CollectionKind.BEHAVIOR
        ));
        inTransaction(() -> collectionPolicy.assertCollectionAllowed(
                eyeSession.sessionId(), ParticipantCollectionPolicy.CollectionKind.GAZE
        ));

        Fixture behaviorOnly = fixture(false, false, true);
        var behaviorSession = sessionService.create(behaviorOnly.token(), request());
        ParticipantSessionPrincipal behaviorPrincipal = tokens.authenticate(
                behaviorSession.sessionToken()
        ).orElseThrow();
        sessionService.decideConsent(behaviorPrincipal, true);
        inTransaction(() -> collectionPolicy.assertCollectionAllowed(
                behaviorSession.sessionId(), ParticipantCollectionPolicy.CollectionKind.BEHAVIOR
        ));
        assertThatThrownBy(() -> inTransaction(() -> collectionPolicy.assertCollectionAllowed(
                behaviorSession.sessionId(), ParticipantCollectionPolicy.CollectionKind.GAZE
        ))).isInstanceOf(ParticipationException.class);
    }

    @Test
    void acceptedBatchActivityCommitsAndRollsBackWithTheCallingTransaction() {
        Fixture fixture = fixture(false, false, true);
        var created = sessionService.create(fixture.token(), request());
        ParticipantSessionPrincipal principal = tokens.authenticate(created.sessionToken()).orElseThrow();
        sessionService.decideConsent(principal, true);
        Instant oldActivity = Instant.parse("2026-01-01T00:00:00Z");
        jdbc.update(
                "UPDATE participant_sessions SET last_activity_at = ?, updated_at = ? WHERE id = ?",
                Timestamp.from(oldActivity),
                Timestamp.from(oldActivity),
                created.sessionId()
        );

        assertThatThrownBy(() -> collectionActivity.recordAcceptedBatch(
                created.sessionId(), ParticipantCollectionPolicy.CollectionKind.BEHAVIOR
        )).isInstanceOf(IllegalTransactionStateException.class);

        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(status -> {
            collectionPolicy.assertCollectionAllowed(
                    created.sessionId(), ParticipantCollectionPolicy.CollectionKind.BEHAVIOR
            );
            collectionActivity.recordAcceptedBatch(
                    created.sessionId(), ParticipantCollectionPolicy.CollectionKind.BEHAVIOR
            );
            assertThat(sessions.findById(created.sessionId()).orElseThrow().getLastActivityAt())
                    .isAfter(oldActivity);
            status.setRollbackOnly();
        });
        assertThat(sessions.findById(created.sessionId()).orElseThrow().getLastActivityAt())
                .isEqualTo(oldActivity);

        inTransaction(() -> {
            collectionPolicy.assertCollectionAllowed(
                    created.sessionId(), ParticipantCollectionPolicy.CollectionKind.BEHAVIOR
            );
            collectionActivity.recordAcceptedBatch(
                    created.sessionId(), ParticipantCollectionPolicy.CollectionKind.BEHAVIOR
            );
        });
        assertThat(sessions.findById(created.sessionId()).orElseThrow().getLastActivityAt())
                .isAfter(oldActivity);
    }

    @Test
    void completionGateFailuresLeaveBrowsingStateUnchanged() {
        Fixture fixture = fixture(false, false, true);
        var created = sessionService.create(fixture.token(), request());
        ParticipantSessionPrincipal principal = tokens.authenticate(created.sessionToken()).orElseThrow();
        sessionService.decideConsent(principal, true);

        completionGate.rejectBrowsing();
        assertThatThrownBy(() -> sessionService.completeBrowsing(principal))
                .isInstanceOf(ParticipationException.class);
        assertThat(sessionService.getCurrent(principal).phase())
                .isEqualTo(ParticipantSessionPhase.BROWSING);
        assertThat(completionGate.browsingChecks()).isOne();
        assertThat(completionGate.sessionChecks()).isZero();

        completionGate.reset();
        completionGate.rejectSession();
        assertThatThrownBy(() -> sessionService.completeBrowsing(principal))
                .isInstanceOf(ParticipationException.class);
        assertThat(sessionService.getCurrent(principal).phase())
                .isEqualTo(ParticipantSessionPhase.BROWSING);
        assertThat(completionGate.browsingChecks()).isOne();
        assertThat(completionGate.sessionChecks()).isOne();
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
        assertThat(completionGate.browsingChecks()).isOne();
        assertThat(completionGate.sessionChecks()).isZero();
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
        assertThatThrownBy(() -> sessionService.decideConsent(forged, true))
                .isInstanceOf(ParticipationException.class)
                .extracting(exception -> ((ParticipationException) exception).getCode().code())
                .isEqualTo("PARTICIPANT_SESSION_NOT_FOUND");
        assertThat(sessions.findById(first.sessionId()).orElseThrow().getPhase())
                .isEqualTo(ParticipantSessionPhase.CONSENT);
    }

    @Test
    void differentSessionsInOneStudyCanHoldWriteTransactionsConcurrently() throws Exception {
        Fixture fixture = fixture(false, false, true);
        var first = sessionService.create(fixture.token(), request());
        var second = sessionService.create(fixture.token(), request());
        CountDownLatch firstLocked = new CountDownLatch(1);
        CountDownLatch secondLocked = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<?> firstWrite = executor.submit(() ->
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        studies.findByIdForParticipation(fixture.studyId()).orElseThrow();
                        var session = sessions.findByIdForUpdate(first.sessionId()).orElseThrow();
                        firstLocked.countDown();
                        await(secondLocked);
                        session.decideConsent(true, false, Instant.now());
                    })
            );
            Future<?> secondWrite = executor.submit(() -> {
                await(firstLocked);
                new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                    studies.findByIdForParticipation(fixture.studyId()).orElseThrow();
                    var session = sessions.findByIdForUpdate(second.sessionId()).orElseThrow();
                    session.decideConsent(true, false, Instant.now());
                    secondLocked.countDown();
                });
            });

            firstWrite.get(10, TimeUnit.SECONDS);
            secondWrite.get(10, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertThat(sessions.findById(first.sessionId()).orElseThrow().getPhase())
                .isEqualTo(ParticipantSessionPhase.BROWSING);
        assertThat(sessions.findById(second.sessionId()).orElseThrow().getPhase())
                .isEqualTo(ParticipantSessionPhase.BROWSING);
    }

    @Test
    void v14UpgradesSchemaValidLegacyLifecycleRowsWithoutInventingPublicationData() {
        String schema = "m5_upgrade_" + UUID.randomUUID().toString().replace("-", "");
        jdbc.execute("CREATE SCHEMA " + schema);
        try {
            Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(schema)
                    .defaultSchema(schema)
                    .locations("classpath:db/migration")
                    .target(MigrationVersion.fromVersion("13"))
                    .load()
                    .migrate();

            UUID ownerId = UUID.randomUUID();
            UUID legacyStudyId = UUID.randomUUID();
            UUID publishedStudyId = UUID.randomUUID();
            jdbc.update(
                    "INSERT INTO " + schema + ".researchers "
                            + "(id, email, password_hash, role) VALUES (?, ?, 'hash', 'RESEARCHER')",
                    ownerId,
                    ownerId + "@migration-test.invalid"
            );
            jdbc.update(
                    "INSERT INTO " + schema + ".studies "
                            + "(id, owner_id, title, status) VALUES (?, ?, 'Legacy', 'COLLECTING')",
                    legacyStudyId,
                    ownerId
            );
            jdbc.update(
                    "INSERT INTO " + schema + ".studies "
                            + "(id, owner_id, title, status, participation_token, published_at) "
                            + "VALUES (?, ?, 'Published', 'COLLECTING', ?, ?)",
                    publishedStudyId,
                    ownerId,
                    "p".repeat(43),
                    Timestamp.from(Instant.parse("2026-10-03T00:00:00Z"))
            );

            Flyway.configure()
                    .dataSource(dataSource)
                    .schemas(schema)
                    .defaultSchema(schema)
                    .locations("classpath:db/migration")
                    .load()
                    .migrate();

            assertThat(jdbc.queryForObject(
                    "SELECT participation_token IS NULL "
                            + "AND published_at IS NULL "
                            + "AND consent_document_version IS NULL "
                            + "FROM " + schema + ".studies WHERE id = ?",
                    Boolean.class,
                    legacyStudyId
            )).isTrue();
            assertThat(jdbc.queryForObject(
                    "SELECT consent_document_version FROM " + schema + ".studies WHERE id = ?",
                    String.class,
                    publishedStudyId
            )).isEqualTo("platform-default-v1");
        } finally {
            jdbc.execute("DROP SCHEMA " + schema + " CASCADE");
        }
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

    private void inTransaction(Runnable action) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> action.run());
    }

    private void await(CountDownLatch latch) {
        try {
            assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while coordinating lock test", exception);
        }
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
