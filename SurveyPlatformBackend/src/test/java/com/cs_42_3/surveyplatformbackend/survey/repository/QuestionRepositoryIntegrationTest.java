package com.cs_42_3.surveyplatformbackend.survey.repository;

import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * PostgreSQL integration tests for the Flyway V3 schema and question repository.
 * <p>
 * Runs only when a local PostgreSQL JDBC URL is supplied through the normal project environment.
 */
@SpringBootTest(properties = {
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Transactional
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfEnvironmentVariable(
        named = "SPRING_DATASOURCE_URL",
        matches = "jdbc:postgresql:.+"
)
class QuestionRepositoryIntegrationTest {

    private static final Logger LOGGER =
            LoggerFactory.getLogger(QuestionRepositoryIntegrationTest.class);
    private static final int QUESTION_BANK_SIZE = 1_000;
    private static final Duration LIST_TIME_BUDGET = Duration.ofSeconds(2);

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Value("${survey.test.schema:}")
    private String isolatedSchema;

    private UUID researcherId;

    @BeforeEach
    void createResearcher() {
        researcherId = UUID.randomUUID();
        insertResearcher(researcherId);
    }

    @AfterAll
    void removeIsolatedTestSchema() {
        if (isolatedSchema.isBlank()) {
            return;
        }
        if (!isolatedSchema.matches("survey_it_[0-9a-f]{32}")) {
            throw new IllegalStateException("Refusing to remove an unexpected database schema");
        }

        jdbcTemplate.execute("DROP SCHEMA \"" + isolatedSchema + "\" CASCADE");
        LOGGER.info("Removed isolated Survey integration-test schema {}", isolatedSchema);
    }

    @Test
    void flywaySchemaUsesUuidColumnsAndResearcherForeignKey() {
        assertThat(columnType("questions", "id")).isEqualTo("uuid");
        assertThat(columnType("questions", "researcher_id")).isEqualTo("uuid");
        assertThat(columnType("question_options", "id")).isEqualTo("uuid");
        assertThat(columnType("question_options", "question_id")).isEqualTo("uuid");

        Long foreignKeyCount = jdbcTemplate.queryForObject(
                """
                        SELECT COUNT(*)
                        FROM information_schema.table_constraints
                        WHERE table_schema = current_schema()
                          AND table_name = 'questions'
                          AND constraint_name = 'fk_questions_researcher'
                          AND constraint_type = 'FOREIGN KEY'
                        """,
                Long.class
        );
        assertThat(foreignKeyCount).isEqualTo(1L);
    }

    @Test
    void searchQuestionsAppliesOwnershipTypeKeywordAndStableOrdering() {
        UUID otherResearcherId = UUID.randomUUID();
        insertResearcher(otherResearcherId);

        Question first = saveTextQuestion(researcherId, "Target Alpha");
        Question second = saveTextQuestion(researcherId, "TARGET Beta");
        saveTextQuestion(otherResearcherId, "Target Hidden");
        questionRepository.saveAndFlush(Question.create(
                researcherId,
                QuestionType.SCALE,
                "Target Scale",
                false,
                1,
                5,
                null,
                null
        ));

        jdbcTemplate.update(
                """
                        UPDATE questions
                        SET updated_at = TIMESTAMPTZ '2026-09-14 06:00:00+00'
                        WHERE id IN (?, ?)
                        """,
                first.getId(),
                second.getId()
        );
        entityManager.clear();

        List<Question> results = questionRepository.searchQuestions(
                researcherId,
                QuestionType.TEXT,
                "target"
        );
        List<UUID> actualIds = results.stream()
                .map(Question::getId)
                .toList();
        List<UUID> expectedIds = actualIds.stream()
                .sorted(Comparator.comparing(UUID::toString).reversed())
                .toList();

        assertThat(results)
                .extracting(Question::getQuestionText)
                .containsExactlyInAnyOrder("Target Alpha", "TARGET Beta");
        assertThat(actualIds).containsExactlyElementsOf(expectedIds);
    }

    @Test
    void deletingQuestionCascadesToQuestionOptions() {
        Question question = Question.create(
                researcherId,
                QuestionType.SINGLE_CHOICE,
                "Choose one",
                true,
                null,
                null,
                null,
                null
        );
        question.replaceOptions(List.of("Yes", "No"));
        Question savedQuestion = questionRepository.saveAndFlush(question);
        UUID questionId = savedQuestion.getId();

        assertThat(countOptions(questionId)).isEqualTo(2);

        entityManager.clear();
        jdbcTemplate.update("DELETE FROM questions WHERE id = ?", questionId);

        assertThat(countOptions(questionId)).isZero();
    }

    @Test
    void replacingOnlyOptionsRefreshesQuestionUpdatedAt() {
        Question question = Question.create(
                researcherId,
                QuestionType.MULTI_CHOICE,
                "Choose any",
                false,
                null,
                null,
                null,
                null
        );
        question.replaceOptions(List.of("Alpha", "Beta"));
        Question savedQuestion = questionRepository.saveAndFlush(question);

        jdbcTemplate.update(
                """
                        UPDATE questions
                        SET updated_at = TIMESTAMPTZ '2000-01-01 00:00:00+00'
                        WHERE id = ?
                        """,
                savedQuestion.getId()
        );
        entityManager.clear();

        Question reloadedQuestion = questionRepository.findById(savedQuestion.getId())
                .orElseThrow();
        Instant previousUpdatedAt = reloadedQuestion.getUpdatedAt();
        reloadedQuestion.replaceOptions(List.of("Alpha", "Beta", "Gamma"));
        Question updatedQuestion = questionRepository.saveAndFlush(reloadedQuestion);
        jdbcTemplate.execute(
                "SET CONSTRAINTS uk_question_options_question_order IMMEDIATE"
        );

        assertThat(updatedQuestion.getUpdatedAt()).isAfter(previousUpdatedAt);
        assertThat(updatedQuestion.getOptions())
                .extracting(option -> option.getOptionOrder())
                .containsExactly(0, 1, 2);
    }

    @Test
    void listAndSearchOneThousandQuestionsStayWithinTentativeBudget() {
        List<Object[]> batchArguments = IntStream.range(0, QUESTION_BANK_SIZE)
                .mapToObj(index -> new Object[]{
                        UUID.randomUUID(),
                        researcherId,
                        "Load test question " + index
                })
                .toList();
        jdbcTemplate.batchUpdate(
                """
                        INSERT INTO questions (
                            id,
                            researcher_id,
                            type,
                            question_text,
                            is_required,
                            created_at,
                            updated_at
                        )
                        VALUES (?, ?, 'TEXT', ?, FALSE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                        """,
                batchArguments
        );
        entityManager.clear();

        long startedAt = System.nanoTime();
        List<Question> questions = questionRepository.searchQuestions(
                researcherId,
                QuestionType.TEXT,
                "load test"
        );
        Duration elapsed = Duration.ofNanos(System.nanoTime() - startedAt);

        LOGGER.info(
                "Listed and searched {} survey questions in {} ms",
                QUESTION_BANK_SIZE,
                elapsed.toMillis()
        );
        assertThat(questions).hasSize(QUESTION_BANK_SIZE);
        assertThat(elapsed).isLessThan(LIST_TIME_BUDGET);
    }

    private Question saveTextQuestion(UUID ownerId, String questionText) {
        return questionRepository.saveAndFlush(Question.create(
                ownerId,
                QuestionType.TEXT,
                questionText,
                false,
                null,
                null,
                null,
                null
        ));
    }

    private void insertResearcher(UUID id) {
        jdbcTemplate.update(
                """
                        INSERT INTO researchers (id, email, password_hash, role)
                        VALUES (?, ?, ?, 'RESEARCHER')
                        """,
                id,
                id + "@survey-test.invalid",
                "test-only-password-hash"
        );
    }

    private String columnType(String tableName, String columnName) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT data_type
                        FROM information_schema.columns
                        WHERE table_schema = current_schema()
                          AND table_name = ?
                          AND column_name = ?
                        """,
                String.class,
                tableName,
                columnName
        );
    }

    private long countOptions(UUID questionId) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM question_options WHERE question_id = ?",
                Long.class,
                questionId
        );
        return count == null ? 0L : count;
    }
}
