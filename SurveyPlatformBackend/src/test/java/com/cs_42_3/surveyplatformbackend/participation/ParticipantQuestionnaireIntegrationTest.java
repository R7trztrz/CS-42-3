package com.cs_42_3.surveyplatformbackend.participation;

import com.cs_42_3.surveyplatformbackend.TestcontainersConfiguration;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.CreateParticipantSessionRequest;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.SubmitQuestionnaireAnswerRequest;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionPrincipal;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionTokenService;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantAbandonmentReason;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSessionPhase;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSessionStatus;
import com.cs_42_3.surveyplatformbackend.participation.exception.ParticipationException;
import com.cs_42_3.surveyplatformbackend.participation.repository.ParticipantQuestionnaireStepRepository;
import com.cs_42_3.surveyplatformbackend.participation.repository.ParticipantSessionRepository;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantQuestionnaireService;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantSessionLifecyclePort;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantSessionService;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantSessionTimeoutService;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = {
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.participation.timeout-scheduler-enabled=false"
})
@Import(TestcontainersConfiguration.class)
@Testcontainers
class ParticipantQuestionnaireIntegrationTest {
    @Autowired
    private ParticipantSessionService sessionService;
    @Autowired
    private ParticipantQuestionnaireService questionnaire;
    @Autowired
    private ParticipantSessionTokenService tokens;
    @Autowired
    private ParticipantSessionRepository sessions;
    @Autowired
    private ParticipantQuestionnaireStepRepository steps;
    @Autowired
    private ParticipantSessionTimeoutService timeouts;
    @Autowired
    private ParticipantSessionLifecyclePort lifecycle;
    @Autowired
    private StudyRepository studies;
    @Autowired
    private PlatformTransactionManager transactionManager;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void traversesAllQuestionTypesReplaysIdempotentlyAndCompletes() {
        Fixture fixture = fixture(false);
        SessionContext context = startQuestionnaire(fixture);

        assertThat(questionnaire.current(context.principal()).currentQuestion().itemId())
                .isEqualTo(fixture.singleItemId());
        assertThatThrownBy(() -> sessionService.completeQuestionnaire(context.principal()))
                .isInstanceOf(ParticipationException.class)
                .extracting(error -> ((ParticipationException) error).getCode().code())
                .isEqualTo("PARTICIPANT_QUESTIONNAIRE_NOT_READY");

        UUID firstKey = UUID.randomUUID();
        SubmitQuestionnaireAnswerRequest firstRequest = request(
                fixture.defaultOptionId(), null, null, null, false
        );
        var afterSingle = questionnaire.answer(
                context.principal(), fixture.singleItemId(), firstKey, firstRequest
        );
        assertThat(afterSingle.currentQuestion().itemId()).isEqualTo(fixture.scaleItemId());

        var replay = questionnaire.answer(
                context.principal(), fixture.singleItemId(), firstKey, firstRequest
        );
        assertThat(replay.currentQuestion().itemId()).isEqualTo(fixture.scaleItemId());
        assertThat(steps.countBySessionId(context.sessionId())).isOne();
        assertThatThrownBy(() -> questionnaire.answer(
                context.principal(),
                fixture.singleItemId(),
                firstKey,
                request(fixture.branchOptionId(), null, null, null, false)
        )).isInstanceOf(ParticipationException.class)
                .extracting(error -> ((ParticipationException) error).getCode().code())
                .isEqualTo("PARTICIPANT_IDEMPOTENCY_CONFLICT");
        assertThatThrownBy(() -> questionnaire.answer(
                context.principal(),
                fixture.scaleItemId(),
                firstKey,
                request(null, null, 3, null, false)
        )).isInstanceOf(ParticipationException.class)
                .extracting(error -> ((ParticipationException) error).getCode().code())
                .isEqualTo("PARTICIPANT_IDEMPOTENCY_CONFLICT");
        assertThatThrownBy(() -> questionnaire.answer(
                context.principal(), fixture.singleItemId(), UUID.randomUUID(), firstRequest
        )).isInstanceOf(ParticipationException.class)
                .extracting(error -> ((ParticipationException) error).getCode().code())
                .isEqualTo("PARTICIPANT_QUESTION_NOT_CURRENT");

        assertThat(questionnaire.answer(
                context.principal(),
                fixture.scaleItemId(),
                UUID.randomUUID(),
                request(null, null, 3, null, false)
        ).currentQuestion().itemId()).isEqualTo(fixture.multiItemId());

        assertThat(questionnaire.answer(
                context.principal(),
                fixture.multiItemId(),
                UUID.randomUUID(),
                request(null, List.of(fixture.multiOption2(), fixture.multiOption1(), fixture.multiOption2()),
                        null, null, false)
        ).currentQuestion().itemId()).isEqualTo(fixture.textItemId());

        var ready = questionnaire.answer(
                context.principal(),
                fixture.textItemId(),
                UUID.randomUUID(),
                request(null, null, null, null, true)
        );
        assertThat(ready.readyToSubmit()).isTrue();
        assertThat(ready.currentQuestion()).isNull();
        assertThat(questionnaire.current(context.principal()).readyToSubmit()).isTrue();
        assertThat(steps.countBySessionId(context.sessionId())).isEqualTo(4);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM participant_answers a "
                        + "JOIN participant_questionnaire_steps s ON s.id = a.step_id "
                        + "WHERE s.session_id = ?",
                Integer.class,
                context.sessionId()
        )).isEqualTo(3);

        var completed = sessionService.completeQuestionnaire(context.principal());
        Instant completedAt = completed.completedAt();
        assertThat(completed.status()).isEqualTo(ParticipantSessionStatus.COMPLETED);
        assertThat(completed.phase()).isEqualTo(ParticipantSessionPhase.FINISHED);
        assertThat(sessionService.completeQuestionnaire(context.principal()).completedAt())
                .isEqualTo(completedAt);
        assertThatThrownBy(() -> questionnaire.answer(
                context.principal(), fixture.textItemId(), UUID.randomUUID(),
                request(null, null, null, "late", false)
        )).isInstanceOf(ParticipationException.class);
    }

    @Test
    void conditionalBranchSkipsAnItemAndOptionalUnansweredHasOnlyAStep() {
        Fixture fixture = fixture(false);
        SessionContext context = startQuestionnaire(fixture);

        assertThat(questionnaire.answer(
                context.principal(),
                fixture.singleItemId(),
                UUID.randomUUID(),
                request(fixture.branchOptionId(), null, null, null, false)
        ).currentQuestion().itemId()).isEqualTo(fixture.multiItemId());
        assertThat(questionnaire.answer(
                context.principal(),
                fixture.multiItemId(),
                UUID.randomUUID(),
                request(null, null, null, null, true)
        ).currentQuestion().itemId()).isEqualTo(fixture.textItemId());
        assertThat(questionnaire.answer(
                context.principal(),
                fixture.textItemId(),
                UUID.randomUUID(),
                request(null, null, null, "kept verbatim", false)
        ).readyToSubmit()).isTrue();

        assertThat(steps.findAllBySessionIdOrderBySequenceNumber(context.sessionId()))
                .extracting(step -> step.getItemId())
                .containsExactly(
                        fixture.singleItemId(), fixture.multiItemId(), fixture.textItemId()
                )
                .doesNotContain(fixture.scaleItemId());
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM participant_answers a "
                        + "JOIN participant_questionnaire_steps s ON s.id = a.step_id "
                        + "WHERE s.session_id = ?",
                Integer.class,
                context.sessionId()
        )).isEqualTo(2);
        sessionService.completeQuestionnaire(context.principal());
    }

    @Test
    void scaleBranchUsesThePublishedRuleAndSkipsTheDefaultItem() {
        Fixture fixture = fixture(false);
        SessionContext context = startQuestionnaire(fixture);
        questionnaire.answer(
                context.principal(), fixture.singleItemId(), UUID.randomUUID(),
                request(fixture.defaultOptionId(), null, null, null, false)
        );

        assertThat(questionnaire.answer(
                context.principal(), fixture.scaleItemId(), UUID.randomUUID(),
                request(null, null, 5, null, false)
        ).currentQuestion().itemId()).isEqualTo(fixture.textItemId());
        questionnaire.answer(
                context.principal(), fixture.textItemId(), UUID.randomUUID(),
                request(null, null, null, null, true)
        );

        assertThat(steps.findAllBySessionIdOrderBySequenceNumber(context.sessionId()))
                .extracting(step -> step.getItemId())
                .containsExactly(
                        fixture.singleItemId(), fixture.scaleItemId(), fixture.textItemId()
                )
                .doesNotContain(fixture.multiItemId());
        sessionService.completeQuestionnaire(context.principal());
    }

    @Test
    void invalidPublishedTargetRollsBackStepAnswerAndPointerTogether() {
        Fixture fixture = fixture(true);
        SessionContext context = startQuestionnaire(fixture);

        assertThatThrownBy(() -> questionnaire.answer(
                context.principal(),
                fixture.singleItemId(),
                UUID.randomUUID(),
                request(fixture.defaultOptionId(), null, null, null, false)
        )).isInstanceOf(ParticipationException.class)
                .extracting(error -> ((ParticipationException) error).getCode().code())
                .isEqualTo("PARTICIPANT_QUESTIONNAIRE_NOT_READY");

        assertThat(steps.countBySessionId(context.sessionId())).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM participant_answers a "
                        + "JOIN participant_questionnaire_steps s ON s.id = a.step_id "
                        + "WHERE s.session_id = ?",
                Integer.class,
                context.sessionId()
        )).isZero();
        assertThat(sessions.findById(context.sessionId()).orElseThrow().getCurrentQuestionItemId())
                .isEqualTo(fixture.singleItemId());
    }

    @Test
    void databaseRejectsDuplicateRuntimeIdentityAndNonObjectAnswerPayload() {
        Fixture fixture = fixture(false);
        SessionContext context = startQuestionnaire(fixture);
        UUID key = UUID.randomUUID();
        questionnaire.answer(
                context.principal(),
                fixture.singleItemId(),
                key,
                request(fixture.defaultOptionId(), null, null, null, false)
        );
        questionnaire.answer(
                context.principal(),
                fixture.scaleItemId(),
                UUID.randomUUID(),
                request(null, null, 3, null, false)
        );
        questionnaire.answer(
                context.principal(),
                fixture.multiItemId(),
                UUID.randomUUID(),
                request(null, null, null, null, true)
        );
        var optionalStep = steps.findAllBySessionIdOrderBySequenceNumber(context.sessionId()).get(2);

        assertThatThrownBy(() -> jdbc.update(
                """
                        INSERT INTO participant_questionnaire_steps (
                            id, session_id, item_id, sequence_number,
                            idempotency_key, request_hash, next_item_id, submitted_at
                        ) VALUES (?, ?, ?, 99, ?, ?, NULL, ?)
                        """,
                UUID.randomUUID(),
                context.sessionId(),
                UUID.randomUUID(),
                key,
                "b".repeat(64),
                Timestamp.from(Instant.now())
        )).isInstanceOf(DataIntegrityViolationException.class);

        assertThatThrownBy(() -> jdbc.update(
                """
                        INSERT INTO participant_answers (
                            id, step_id, question_type, answer_payload, answered_at
                        ) VALUES (?, ?, 'SINGLE_CHOICE', CAST('[]' AS jsonb), ?)
                        """,
                UUID.randomUUID(),
                optionalStep.getId(),
                Timestamp.from(Instant.now())
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void explicitExitTimeoutStudyCloseAndDeletionPreserveTerminalRulesAndCascades() {
        Fixture fixture = fixture(false);
        SessionContext explicit = startQuestionnaire(fixture);
        var abandoned = sessionService.abandon(explicit.principal());
        assertThat(abandoned.abandonmentReason())
                .isEqualTo(ParticipantAbandonmentReason.PARTICIPANT_EXIT);
        assertThat(sessionService.abandon(explicit.principal()).abandonedAt())
                .isEqualTo(abandoned.abandonedAt());

        SessionContext stale = startQuestionnaire(fixture);
        SessionContext active = startQuestionnaire(fixture);
        jdbc.update(
                "UPDATE participant_sessions SET last_activity_at = ? WHERE id = ?",
                Timestamp.from(Instant.now().minusSeconds(7200)),
                stale.sessionId()
        );
        int timedOut = timeouts.scanOnce();
        assertThat(timedOut).isGreaterThanOrEqualTo(1);
        assertThat(sessions.findById(stale.sessionId()).orElseThrow().getAbandonmentReason())
                .isEqualTo(ParticipantAbandonmentReason.INACTIVITY_TIMEOUT);
        assertThat(sessions.findById(active.sessionId()).orElseThrow().getStatus())
                .isEqualTo(ParticipantSessionStatus.IN_PROGRESS);

        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var study = studies.findOwnedByIdForUpdate(
                    fixture.studyId(), fixture.ownerId()
            ).orElseThrow();
            lifecycle.abandonActiveSessionsForStudy(fixture.studyId(), Instant.now());
            assertThat(entityManager.contains(study)).isTrue();
            ReflectionTestUtils.setField(study, "status", StudyStatus.CLOSED);
        });
        assertThat(sessions.findById(active.sessionId()).orElseThrow().getAbandonmentReason())
                .isEqualTo(ParticipantAbandonmentReason.STUDY_CLOSED);
        assertThat(sessions.findById(explicit.sessionId()).orElseThrow().getAbandonmentReason())
                .isEqualTo(ParticipantAbandonmentReason.PARTICIPANT_EXIT);

        jdbc.update("DELETE FROM questionnaire_publication_snapshots WHERE study_id = ?", fixture.studyId());
        jdbc.update("DELETE FROM studies WHERE id = ?", fixture.studyId());
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM participant_sessions WHERE study_id = ?",
                Integer.class,
                fixture.studyId()
        )).isZero();
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM participant_questionnaire_steps WHERE session_id IN (?, ?, ?)",
                Integer.class,
                explicit.sessionId(), stale.sessionId(), active.sessionId()
        )).isZero();
    }

    @Test
    void answerAndTimeoutRacesResolveToOneAtomicOutcome() throws Exception {
        Fixture answerWinsFixture = fixture(false);
        SessionContext answerWins = startQuestionnaire(answerWinsFixture);
        jdbc.update(
                "UPDATE participant_sessions SET last_activity_at = ? WHERE id = ?",
                Timestamp.from(Instant.now().minusSeconds(7200)),
                answerWins.sessionId()
        );
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch answerApplied = new CountDownLatch(1);
            CountDownLatch allowAnswerCommit = new CountDownLatch(1);
            Future<?> answer = executor.submit(() ->
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        questionnaire.answer(
                                answerWins.principal(),
                                answerWinsFixture.singleItemId(),
                                UUID.randomUUID(),
                                request(answerWinsFixture.defaultOptionId(), null, null, null, false)
                        );
                        answerApplied.countDown();
                        await(allowAnswerCommit);
                    })
            );
            assertThat(answerApplied.await(10, TimeUnit.SECONDS)).isTrue();
            Future<Integer> timeout = executor.submit(timeouts::scanOnce);
            allowAnswerCommit.countDown();
            answer.get(10, TimeUnit.SECONDS);
            timeout.get(10, TimeUnit.SECONDS);
            assertThat(sessions.findById(answerWins.sessionId()).orElseThrow().getStatus())
                    .isEqualTo(ParticipantSessionStatus.IN_PROGRESS);

            Fixture timeoutWinsFixture = fixture(false);
            SessionContext timeoutWins = startQuestionnaire(timeoutWinsFixture);
            Instant staleAt = Instant.now().minusSeconds(7200);
            jdbc.update(
                    "UPDATE participant_sessions SET last_activity_at = ? WHERE id = ?",
                    Timestamp.from(staleAt),
                    timeoutWins.sessionId()
            );
            CountDownLatch timeoutApplied = new CountDownLatch(1);
            CountDownLatch allowTimeoutCommit = new CountDownLatch(1);
            Future<?> timeoutTransaction = executor.submit(() ->
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        studies.findByIdForParticipation(timeoutWinsFixture.studyId()).orElseThrow();
                        sessions.findByIdForUpdate(timeoutWins.sessionId()).orElseThrow()
                                .abandonIfInactive(Instant.now().minusSeconds(1800), Instant.now());
                        timeoutApplied.countDown();
                        await(allowTimeoutCommit);
                    })
            );
            assertThat(timeoutApplied.await(10, TimeUnit.SECONDS)).isTrue();
            Future<?> lateAnswer = executor.submit(() -> questionnaire.answer(
                    timeoutWins.principal(),
                    timeoutWinsFixture.singleItemId(),
                    UUID.randomUUID(),
                    request(timeoutWinsFixture.defaultOptionId(), null, null, null, false)
            ));
            allowTimeoutCommit.countDown();
            timeoutTransaction.get(10, TimeUnit.SECONDS);
            assertThatThrownBy(() -> lateAnswer.get(10, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasRootCauseInstanceOf(ParticipationException.class);
            assertThat(sessions.findById(timeoutWins.sessionId()).orElseThrow().getAbandonmentReason())
                    .isEqualTo(ParticipantAbandonmentReason.INACTIVITY_TIMEOUT);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void finalSubmissionAndStudyCloseRacesRespectStudyBeforeSessionLockOrder() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Fixture submitWinsFixture = fixture(false);
            SessionContext submitWins = readyToSubmit(submitWinsFixture);
            CountDownLatch submitted = new CountDownLatch(1);
            CountDownLatch allowSubmitCommit = new CountDownLatch(1);
            Future<?> submit = executor.submit(() ->
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        sessionService.completeQuestionnaire(submitWins.principal());
                        submitted.countDown();
                        await(allowSubmitCommit);
                    })
            );
            assertThat(submitted.await(10, TimeUnit.SECONDS)).isTrue();
            Future<?> closeAfterSubmit = executor.submit(() -> closeStudy(submitWinsFixture));
            allowSubmitCommit.countDown();
            submit.get(10, TimeUnit.SECONDS);
            closeAfterSubmit.get(10, TimeUnit.SECONDS);
            assertThat(sessions.findById(submitWins.sessionId()).orElseThrow().getStatus())
                    .isEqualTo(ParticipantSessionStatus.COMPLETED);

            Fixture closeWinsFixture = fixture(false);
            SessionContext closeWins = readyToSubmit(closeWinsFixture);
            CountDownLatch closed = new CountDownLatch(1);
            CountDownLatch allowCloseCommit = new CountDownLatch(1);
            Future<?> close = executor.submit(() ->
                    new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
                        var study = studies.findOwnedByIdForUpdate(
                                closeWinsFixture.studyId(), closeWinsFixture.ownerId()
                        ).orElseThrow();
                        lifecycle.abandonActiveSessionsForStudy(
                                closeWinsFixture.studyId(), Instant.now()
                        );
                        assertThat(entityManager.contains(study)).isTrue();
                        ReflectionTestUtils.setField(study, "status", StudyStatus.CLOSED);
                        closed.countDown();
                        await(allowCloseCommit);
                    })
            );
            assertThat(closed.await(10, TimeUnit.SECONDS)).isTrue();
            Future<?> submitAfterClose = executor.submit(
                    () -> sessionService.completeQuestionnaire(closeWins.principal())
            );
            allowCloseCommit.countDown();
            close.get(10, TimeUnit.SECONDS);
            assertThatThrownBy(() -> submitAfterClose.get(10, TimeUnit.SECONDS))
                    .isInstanceOf(ExecutionException.class)
                    .hasRootCauseInstanceOf(com.cs_42_3.surveyplatformbackend.study.exception.StudyClosedException.class);
            var closedSession = sessions.findById(closeWins.sessionId()).orElseThrow();
            assertThat(closedSession.getStatus()).isEqualTo(ParticipantSessionStatus.ABANDONED);
            assertThat(closedSession.getAbandonmentReason())
                    .isEqualTo(ParticipantAbandonmentReason.STUDY_CLOSED);
        } finally {
            executor.shutdownNow();
        }
    }

    private SessionContext readyToSubmit(Fixture fixture) {
        SessionContext context = startQuestionnaire(fixture);
        questionnaire.answer(
                context.principal(), fixture.singleItemId(), UUID.randomUUID(),
                request(fixture.branchOptionId(), null, null, null, false)
        );
        questionnaire.answer(
                context.principal(), fixture.multiItemId(), UUID.randomUUID(),
                request(null, null, null, null, true)
        );
        questionnaire.answer(
                context.principal(), fixture.textItemId(), UUID.randomUUID(),
                request(null, null, null, null, true)
        );
        return context;
    }

    private void closeStudy(Fixture fixture) {
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> {
            var study = studies.findOwnedByIdForUpdate(
                    fixture.studyId(), fixture.ownerId()
            ).orElseThrow();
            lifecycle.abandonActiveSessionsForStudy(fixture.studyId(), Instant.now());
            assertThat(entityManager.contains(study)).isTrue();
            ReflectionTestUtils.setField(study, "status", StudyStatus.CLOSED);
        });
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(10, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for concurrent test coordination");
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Concurrent test was interrupted", exception);
        }
    }

    private SessionContext startQuestionnaire(Fixture fixture) {
        var created = sessionService.create(fixture.token(), new CreateParticipantSessionRequest(null));
        ParticipantSessionPrincipal principal = tokens.authenticate(created.sessionToken()).orElseThrow();
        sessionService.decideConsent(principal, true);
        sessionService.completeBrowsing(principal);
        return new SessionContext(created.sessionId(), principal);
    }

    private SubmitQuestionnaireAnswerRequest request(
            UUID optionId,
            List<UUID> optionIds,
            Integer scale,
            String text,
            boolean unanswered
    ) {
        return new SubmitQuestionnaireAnswerRequest(optionId, optionIds, scale, text, unanswered);
    }

    private Fixture fixture(boolean brokenDefaultTarget) {
        UUID ownerId = UUID.randomUUID();
        UUID studyId = UUID.randomUUID();
        UUID questionnaireId = UUID.randomUUID();
        UUID single = UUID.randomUUID();
        UUID scale = UUID.randomUUID();
        UUID multi = UUID.randomUUID();
        UUID text = UUID.randomUUID();
        UUID branchOption = UUID.randomUUID();
        UUID defaultOption = UUID.randomUUID();
        UUID multiOption1 = UUID.randomUUID();
        UUID multiOption2 = UUID.randomUUID();
        UUID defaultTarget = brokenDefaultTarget ? UUID.randomUUID() : scale;
        String token = token();
        Instant published = Instant.parse("2026-10-03T00:00:00Z");

        jdbc.update(
                "INSERT INTO researchers (id, email, password_hash, role) "
                        + "VALUES (?, ?, 'test-password-hash', 'RESEARCHER')",
                ownerId,
                ownerId + "@questionnaire-runtime.invalid"
        );
        jdbc.update(
                """
                        INSERT INTO studies (
                            id, owner_id, title, description, status,
                            eye_tracking_enabled, questionnaire_enabled,
                            participation_token, published_at, consent_document_version
                        ) VALUES (?, ?, 'Questionnaire runtime', 'Description', 'COLLECTING',
                                  false, true, ?, ?, 'platform-default-v1')
                        """,
                studyId,
                ownerId,
                token,
                Timestamp.from(published)
        );
        jdbc.update(
                "INSERT INTO study_feeds (study_id, template_code, theme, content, schema_version) "
                        + "VALUES (?, 'facebook', 'facebook', CAST(? AS jsonb), 1)",
                studyId,
                "{\"ROOT\":{\"type\":\"div\"}}"
        );
        String content = """
                {
                  "questionnaireId":"%s","studyId":"%s","version":0,
                  "sourceUpdatedAt":"2026-10-03T00:00:00Z",
                  "items":[
                    {
                      "itemId":"%s","sourceQuestionId":"%s","position":1,
                      "type":"SINGLE_CHOICE","questionText":"Choose","required":true,
                      "options":[
                        {"optionId":"%s","optionText":"Branch","optionOrder":1},
                        {"optionId":"%s","optionText":"Default","optionOrder":2}
                      ],"scaleMin":null,"scaleMax":null,"scaleMinLabel":null,"scaleMaxLabel":null,
                      "questionCreatedAt":"2026-10-03T00:00:00Z","questionUpdatedAt":"2026-10-03T00:00:00Z",
                      "defaultNextItemId":"%s",
                      "branchRules":[{"ruleId":"%s","sourceOptionId":"%s","sourceScaleValue":null,"targetItemId":"%s"}]
                    },
                    {
                      "itemId":"%s","sourceQuestionId":"%s","position":2,
                      "type":"SCALE","questionText":"Rate","required":true,
                      "options":[],"scaleMin":1,"scaleMax":5,"scaleMinLabel":"Low","scaleMaxLabel":"High",
                      "questionCreatedAt":"2026-10-03T00:00:00Z","questionUpdatedAt":"2026-10-03T00:00:00Z",
                      "defaultNextItemId":"%s",
                      "branchRules":[{"ruleId":"%s","sourceOptionId":null,"sourceScaleValue":5,"targetItemId":"%s"}]
                    },
                    {
                      "itemId":"%s","sourceQuestionId":"%s","position":3,
                      "type":"MULTI_CHOICE","questionText":"Many","required":false,
                      "options":[
                        {"optionId":"%s","optionText":"One","optionOrder":1},
                        {"optionId":"%s","optionText":"Two","optionOrder":2}
                      ],"scaleMin":null,"scaleMax":null,"scaleMinLabel":null,"scaleMaxLabel":null,
                      "questionCreatedAt":"2026-10-03T00:00:00Z","questionUpdatedAt":"2026-10-03T00:00:00Z",
                      "defaultNextItemId":"%s","branchRules":[]
                    },
                    {
                      "itemId":"%s","sourceQuestionId":"%s","position":4,
                      "type":"TEXT","questionText":"Comment","required":false,
                      "options":[],"scaleMin":null,"scaleMax":null,"scaleMinLabel":null,"scaleMaxLabel":null,
                      "questionCreatedAt":"2026-10-03T00:00:00Z","questionUpdatedAt":"2026-10-03T00:00:00Z",
                      "defaultNextItemId":null,"branchRules":[]
                    }
                  ]
                }
                """.formatted(
                questionnaireId, studyId,
                single, UUID.randomUUID(), branchOption, defaultOption, defaultTarget,
                UUID.randomUUID(), branchOption, multi,
                scale, UUID.randomUUID(), multi, UUID.randomUUID(), text,
                multi, UUID.randomUUID(), multiOption1, multiOption2, text,
                text, UUID.randomUUID()
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
                questionnaireId,
                Timestamp.from(published),
                content
        );
        return new Fixture(
                ownerId, studyId, token, single, scale, multi, text,
                branchOption, defaultOption, multiOption1, multiOption2
        );
    }

    private String token() {
        byte[] bytes = new byte[32];
        java.util.concurrent.ThreadLocalRandom.current().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private record SessionContext(UUID sessionId, ParticipantSessionPrincipal principal) {
    }

    private record Fixture(
            UUID ownerId,
            UUID studyId,
            String token,
            UUID singleItemId,
            UUID scaleItemId,
            UUID multiItemId,
            UUID textItemId,
            UUID branchOptionId,
            UUID defaultOptionId,
            UUID multiOption1,
            UUID multiOption2
    ) {
    }
}
