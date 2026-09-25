package com.cs_42_3.surveyplatformbackend.survey.questionnaire.service;

import com.cs_42_3.surveyplatformbackend.TestcontainersConfiguration;
import com.cs_42_3.surveyplatformbackend.security.CurrentResearcher;
import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.QuestionnaireSaveResult;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.SaveQuestionnaireItemRequest;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.SaveQuestionnaireRequest;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireVersionConflictException;
import com.cs_42_3.surveyplatformbackend.survey.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** Verifies that the shared study-row lock serializes first questionnaire creation. */
@SpringBootTest(properties = {
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Import(TestcontainersConfiguration.class)
@Testcontainers(disabledWithoutDocker = true)
class QuestionnaireConcurrencyIntegrationTest {

    @Autowired
    private QuestionnaireService questionnaireService;
    @Autowired
    private StudyRepository studyRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private CurrentResearcher currentResearcher;

    private UUID researcherId;
    private UUID studyId;
    private UUID firstQuestionId;
    private UUID secondQuestionId;

    @BeforeEach
    void setUp() {
        researcherId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                        INSERT INTO researchers (id, email, password_hash, role)
                        VALUES (?, ?, 'test-only-password-hash', 'RESEARCHER')
                        """,
                researcherId,
                researcherId + "@survey-test.invalid"
        );
        studyId = studyRepository.saveAndFlush(
                new Study(researcherId, "Concurrent questionnaire", null)
        ).getId();
        firstQuestionId = saveQuestion("First concurrent question").getId();
        secondQuestionId = saveQuestion("Second concurrent question").getId();
        when(currentResearcher.getId()).thenReturn(researcherId);
    }

    @Test
    void identicalConcurrentFirstCreatesReturnOneCreatedAndOneSafeReplay() throws Exception {
        SaveQuestionnaireRequest request = request(firstQuestionId);

        List<Object> outcomes = executeConcurrently(request, request);

        assertThat(outcomes).allMatch(QuestionnaireSaveResult.class::isInstance);
        assertThat(outcomes.stream()
                .map(QuestionnaireSaveResult.class::cast)
                .map(QuestionnaireSaveResult::created)
                .toList()).containsExactlyInAnyOrder(true, false);
    }

    @Test
    void differentConcurrentFirstCreatesReturnOneCreatedAndOneConflict() throws Exception {
        List<Object> outcomes = executeConcurrently(
                request(firstQuestionId),
                request(secondQuestionId)
        );

        assertThat(outcomes.stream().filter(QuestionnaireSaveResult.class::isInstance)).hasSize(1);
        assertThat(outcomes.stream().filter(QuestionnaireVersionConflictException.class::isInstance))
                .hasSize(1);
        QuestionnaireSaveResult success = outcomes.stream()
                .filter(QuestionnaireSaveResult.class::isInstance)
                .map(QuestionnaireSaveResult.class::cast)
                .findFirst()
                .orElseThrow();
        assertThat(success.created()).isTrue();
    }

    private List<Object> executeConcurrently(
            SaveQuestionnaireRequest first,
            SaveQuestionnaireRequest second
    ) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            Future<Object> firstOutcome = executor.submit(() -> saveAfter(start, first));
            Future<Object> secondOutcome = executor.submit(() -> saveAfter(start, second));
            start.countDown();
            return List.of(
                    firstOutcome.get(15, TimeUnit.SECONDS),
                    secondOutcome.get(15, TimeUnit.SECONDS)
            );
        } finally {
            executor.shutdownNow();
        }
    }

    private Object saveAfter(
            CountDownLatch start,
            SaveQuestionnaireRequest request
    ) throws InterruptedException {
        start.await();
        try {
            return questionnaireService.saveQuestionnaire(studyId, request);
        } catch (RuntimeException exception) {
            return exception;
        }
    }

    private SaveQuestionnaireRequest request(UUID questionId) {
        return new SaveQuestionnaireRequest(
                null,
                List.of(new SaveQuestionnaireItemRequest(null, questionId))
        );
    }

    private Question saveQuestion(String text) {
        return questionRepository.saveAndFlush(Question.create(
                researcherId,
                QuestionType.TEXT,
                text,
                false,
                null,
                null,
                null,
                null
        ));
    }
}
