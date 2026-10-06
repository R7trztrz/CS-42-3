package com.cs_42_3.surveyplatformbackend.study.service.implementation;

import com.cs_42_3.surveyplatformbackend.TestcontainersConfiguration;
import com.cs_42_3.surveyplatformbackend.feed.domain.StudyFeed;
import com.cs_42_3.surveyplatformbackend.feed.repository.FeedTemplateRepository;
import com.cs_42_3.surveyplatformbackend.feed.repository.StudyFeedRepository;
import com.cs_42_3.surveyplatformbackend.security.CurrentResearcher;
import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyUpdate;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.cs_42_3.surveyplatformbackend.study.service.StudyPublicationService;
import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.UpdateQuestionRequest;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.QuestionnaireSaveResult;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.SaveQuestionnaireItemRequest;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.SaveQuestionnaireRequest;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.domain.Questionnaire;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireLockedException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.repository.QuestionnaireRepository;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.service.QuestionnaireService;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.exception.QuestionnairePublicationException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.repository.QuestionnaireSnapshotRepository;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.service.QuestionnaireSnapshotReadService;
import com.cs_42_3.surveyplatformbackend.survey.repository.QuestionRepository;
import com.cs_42_3.surveyplatformbackend.survey.service.QuestionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** Real PostgreSQL coverage for the atomic questionnaire publication boundary. */
@SpringBootTest(properties = {
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Testcontainers
@WithMockUser(roles = "RESEARCHER")
class QuestionnairePublicationIntegrationTest {

    @Autowired
    private StudyPublicationService publicationService;
    @Autowired
    private StudyRepository studyRepository;
    @Autowired
    private StudyFeedRepository feedRepository;
    @Autowired
    private FeedTemplateRepository templateRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private QuestionnaireRepository questionnaireRepository;
    @Autowired
    private QuestionnaireSnapshotRepository snapshotRepository;
    @Autowired
    private QuestionnaireSnapshotReadService snapshotReadService;
    @Autowired
    private QuestionnaireService questionnaireService;
    @Autowired
    private QuestionService questionService;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private CurrentResearcher currentResearcher;

    @Test
    void enabledQuestionnairePublishesOneSelfContainedSnapshotUnaffectedByQuestionChanges() {
        TestFixture fixture = fixture(true, true);

        Study published = publicationService.publish(
                fixture.ownerId(),
                fixture.study().getId(),
                fixture.study().getLockVersion()
        );

        assertThat(published.getStatus()).isEqualTo(StudyStatus.COLLECTING);
        assertThat(snapshotRepository.findByStudyId(published.getId())).isPresent();
        var initialSnapshot = snapshotReadService.getPublishedQuestionnaire(published.getId());
        assertThat(initialSnapshot.contentSource().name()).isEqualTo("PUBLISHED_SNAPSHOT");
        assertThat(initialSnapshot.items()).singleElement()
                .satisfies(item -> assertThat(item.question().questionText())
                        .isEqualTo("Original publication text"));

        Question liveQuestion = questionRepository.findById(fixture.question().getId()).orElseThrow();
        liveQuestion.update(QuestionType.TEXT, "Changed after publication", false, null, null, null, null);
        questionRepository.saveAndFlush(liveQuestion);
        questionRepository.deleteById(liveQuestion.getId());
        questionRepository.flush();

        var unchangedSnapshot = snapshotReadService.getPublishedQuestionnaire(published.getId());
        assertThat(unchangedSnapshot.items()).singleElement()
                .satisfies(item -> assertThat(item.question().questionText())
                        .isEqualTo("Original publication text"));
    }

    @Test
    void publicationValidationFailureRollsBackSnapshotAndStudyTransition() {
        TestFixture fixture = fixture(true, false);

        assertThatThrownBy(() -> publicationService.publish(
                fixture.ownerId(),
                fixture.study().getId(),
                fixture.study().getLockVersion()
        )).isInstanceOf(QuestionnairePublicationException.class);

        assertThat(snapshotRepository.findByStudyId(fixture.study().getId())).isEmpty();
        assertThat(studyRepository.findById(fixture.study().getId()).orElseThrow().getStatus())
                .isEqualTo(StudyStatus.DRAFT);
    }

    @Test
    void questionnaireDisabledPublishesWithoutQuestionnaireOrSnapshot() {
        TestFixture fixture = fixture(false, false);

        Study published = publicationService.publish(
                fixture.ownerId(),
                fixture.study().getId(),
                fixture.study().getLockVersion()
        );

        assertThat(published.getStatus()).isEqualTo(StudyStatus.COLLECTING);
        assertThat(snapshotRepository.findByStudyId(published.getId())).isEmpty();
    }

    @Test
    void concurrentPublicationCreatesExactlyOneSnapshot() throws Exception {
        TestFixture fixture = fixture(true, true);
        long expectedVersion = fixture.study().getLockVersion();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger successes = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();
        var delegate = Executors.newFixedThreadPool(2);
        var executor = new DelegatingSecurityContextExecutorService(delegate);

        try {
            List<java.util.concurrent.Future<?>> futures = List.of(
                    executor.submit(() -> publishAfterBarrier(fixture, expectedVersion, ready, start,
                            successes, failures)),
                    executor.submit(() -> publishAfterBarrier(fixture, expectedVersion, ready, start,
                            successes, failures))
            );
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (var future : futures) {
                future.get(20, TimeUnit.SECONDS);
            }
        } finally {
            executor.shutdownNow();
        }

        assertThat(successes).hasValue(1);
        assertThat(failures).hasValue(1);
        assertThat(snapshotRepository.findByStudyId(fixture.study().getId())).isPresent();
    }

    @Test
    void publicationAndQuestionnaireSaveSerializeToOneConsistentSnapshot() throws Exception {
        TestFixture fixture = fixture(true, true);
        Questionnaire questionnaire = questionnaireRepository.findByStudyId(fixture.study().getId())
                .orElseThrow();
        UUID itemId = questionnaire.getItems().get(0).getId();
        Question replacement = questionRepository.saveAndFlush(Question.create(
                fixture.ownerId(),
                QuestionType.TEXT,
                "Replacement publication text",
                true,
                null,
                null,
                null,
                null
        ));
        SaveQuestionnaireRequest saveRequest = new SaveQuestionnaireRequest(
                questionnaire.getLockVersion(),
                List.of(new SaveQuestionnaireItemRequest(itemId, replacement.getId()))
        );

        List<Object> outcomes = executeConcurrently(
                () -> publicationService.publish(
                        fixture.ownerId(),
                        fixture.study().getId(),
                        fixture.study().getLockVersion()
                ),
                () -> questionnaireService.saveQuestionnaire(fixture.study().getId(), saveRequest)
        );

        assertThat(outcomes.get(0)).isInstanceOf(Study.class);
        assertThat(outcomes.get(1)).isInstanceOfAny(
                QuestionnaireSaveResult.class,
                QuestionnaireLockedException.class
        );
        Study reloaded = studyRepository.findById(fixture.study().getId()).orElseThrow();
        assertThat(reloaded.getStatus()).isEqualTo(StudyStatus.COLLECTING);
        var published = snapshotReadService.getPublishedQuestionnaire(fixture.study().getId());
        assertThat(published.items()).singleElement().satisfies(item -> {
            if (outcomes.get(1) instanceof QuestionnaireSaveResult) {
                assertThat(item.question().questionId()).isEqualTo(replacement.getId());
            } else {
                assertThat(item.question().questionId()).isEqualTo(fixture.question().getId());
            }
        });
    }

    @Test
    void publicationAndQuestionUpdateSerializeWithoutChangingTheCapturedSnapshot() throws Exception {
        TestFixture fixture = fixture(true, true);
        UpdateQuestionRequest update = new UpdateQuestionRequest(
                QuestionType.TEXT,
                "Updated during publication",
                true,
                List.of(),
                null,
                null,
                null,
                null,
                false
        );

        List<Object> outcomes = executeConcurrently(
                () -> publicationService.publish(
                        fixture.ownerId(),
                        fixture.study().getId(),
                        fixture.study().getLockVersion()
                ),
                () -> questionService.updateQuestion(fixture.question().getId(), update)
        );

        assertThat(outcomes.get(0)).isInstanceOf(Study.class);
        assertThat(outcomes.get(1))
                .isInstanceOf(com.cs_42_3.surveyplatformbackend.survey.api.dto.QuestionResponse.class);
        assertThat(questionRepository.findById(fixture.question().getId()).orElseThrow()
                .getQuestionText()).isEqualTo("Updated during publication");
        assertThat(snapshotReadService.getPublishedQuestionnaire(fixture.study().getId())
                .items().get(0).question().questionText())
                .isIn("Original publication text", "Updated during publication");
    }

    @Test
    void publicationAndQuestionDeleteProduceOnlyAnAtomicBeforeOrAfterOutcome() throws Exception {
        TestFixture fixture = fixture(true, true);

        List<Object> outcomes = executeConcurrently(
                () -> publicationService.publish(
                        fixture.ownerId(),
                        fixture.study().getId(),
                        fixture.study().getLockVersion()
                ),
                () -> {
                    questionService.deleteQuestion(fixture.question().getId());
                    return Boolean.TRUE;
                }
        );

        assertThat(outcomes.get(1)).isEqualTo(Boolean.TRUE);
        assertThat(questionRepository.findById(fixture.question().getId())).isEmpty();
        Study reloaded = studyRepository.findById(fixture.study().getId()).orElseThrow();
        if (outcomes.get(0) instanceof Study) {
            assertThat(reloaded.getStatus()).isEqualTo(StudyStatus.COLLECTING);
            assertThat(snapshotReadService.getPublishedQuestionnaire(fixture.study().getId())
                    .items().get(0).question().questionText())
                    .isEqualTo("Original publication text");
        } else {
            assertThat(outcomes.get(0)).isInstanceOf(QuestionnairePublicationException.class);
            assertThat(reloaded.getStatus()).isEqualTo(StudyStatus.DRAFT);
            assertThat(snapshotRepository.findByStudyId(fixture.study().getId())).isEmpty();
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "survey.performance.tests", matches = "true")
    void savesPublishesAndReadsOneHundredItemQuestionnaireWithinTentativeBudget() {
        TestFixture fixture = fixture(true, false);
        List<Question> questions = questionRepository.saveAll(IntStream.range(0, 100)
                .mapToObj(index -> Question.create(
                        fixture.ownerId(),
                        QuestionType.TEXT,
                        "Performance question " + index,
                        false,
                        null,
                        null,
                        null,
                        null
                ))
                .toList());
        questionRepository.flush();
        Questionnaire questionnaire = questionnaireRepository
                .findByStudyId(fixture.study().getId())
                .orElseThrow();

        long started = System.nanoTime();
        questionnaire.synchronizeItems(questions.stream()
                .map(question -> new Questionnaire.ItemPlacement(null, question.getId()))
                .toList());
        questionnaire.markModified();
        questionnaireRepository.saveAndFlush(questionnaire);
        publicationService.publish(
                fixture.ownerId(),
                fixture.study().getId(),
                fixture.study().getLockVersion()
        );
        var response = snapshotReadService.getPublishedQuestionnaire(fixture.study().getId());
        Duration elapsed = Duration.ofNanos(System.nanoTime() - started);

        assertThat(response.items()).hasSize(100);
        assertThat(elapsed).isLessThan(Duration.ofSeconds(15));
    }

    private void publishAfterBarrier(
            TestFixture fixture,
            long expectedVersion,
            CountDownLatch ready,
            CountDownLatch start,
            AtomicInteger successes,
            AtomicInteger failures
    ) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Publication barrier timed out");
            }
            publicationService.publish(fixture.ownerId(), fixture.study().getId(), expectedVersion);
            successes.incrementAndGet();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            failures.incrementAndGet();
        } catch (RuntimeException exception) {
            failures.incrementAndGet();
        }
    }

    private List<Object> executeConcurrently(
            Callable<?> first,
            Callable<?> second
    ) throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        var delegate = Executors.newFixedThreadPool(2);
        var executor = new DelegatingSecurityContextExecutorService(delegate);
        try {
            var firstOutcome = executor.submit(() -> runAfterBarrier(first, ready, start));
            var secondOutcome = executor.submit(() -> runAfterBarrier(second, ready, start));
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(
                    firstOutcome.get(20, TimeUnit.SECONDS),
                    secondOutcome.get(20, TimeUnit.SECONDS)
            );
        } finally {
            executor.shutdownNow();
        }
    }

    private Object runAfterBarrier(
            Callable<?> operation,
            CountDownLatch ready,
            CountDownLatch start
    ) {
        ready.countDown();
        try {
            if (!start.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Concurrent operation barrier timed out");
            }
            return operation.call();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            return exception;
        } catch (RuntimeException exception) {
            return exception;
        } catch (Exception exception) {
            return new IllegalStateException(exception);
        }
    }

    private TestFixture fixture(boolean questionnaireEnabled, boolean addQuestion) {
        UUID ownerId = UUID.randomUUID();
        when(currentResearcher.getId()).thenReturn(ownerId);
        jdbcTemplate.update(
                """
                        INSERT INTO researchers (id, email, password_hash, role)
                        VALUES (?, ?, 'test-only-password-hash', 'RESEARCHER')
                        """,
                ownerId,
                ownerId + "@publication-test.invalid"
        );
        Study study = new Study(ownerId, "Publication integration", null);
        study.applyUpdate(new StudyUpdate(0, null, false, null, false, questionnaireEnabled));
        study = studyRepository.saveAndFlush(study);
        feedRepository.saveAndFlush(new StudyFeed(
                study.getId(),
                templateRepository.findById("facebook").orElseThrow()
        ));

        Question question = null;
        if (questionnaireEnabled) {
            Questionnaire questionnaire = Questionnaire.create(study.getId());
            if (addQuestion) {
                question = questionRepository.saveAndFlush(Question.create(
                        ownerId,
                        QuestionType.TEXT,
                        "Original publication text",
                        true,
                        null,
                        null,
                        null,
                        null
                ));
                questionnaire.synchronizeItems(List.of(
                        new Questionnaire.ItemPlacement(null, question.getId())
                ));
            }
            questionnaire.markModified();
            questionnaireRepository.saveAndFlush(questionnaire);
        }
        return new TestFixture(ownerId, study, question);
    }

    private record TestFixture(UUID ownerId, Study study, Question question) {}
}
