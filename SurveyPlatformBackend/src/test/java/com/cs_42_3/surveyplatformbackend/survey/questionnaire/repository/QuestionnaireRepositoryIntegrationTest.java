package com.cs_42_3.surveyplatformbackend.survey.questionnaire.repository;

import com.cs_42_3.surveyplatformbackend.TestcontainersConfiguration;
import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.domain.Questionnaire;
import com.cs_42_3.surveyplatformbackend.survey.repository.QuestionRepository;
import jakarta.persistence.EntityManager;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** PostgreSQL integration coverage for V10-V13 constraints and questionnaire mappings. */
@SpringBootTest(properties = {
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Transactional
@Testcontainers
class QuestionnaireRepositoryIntegrationTest {

    @Autowired
    private QuestionnaireRepository questionnaireRepository;
    @Autowired
    private StudyRepository studyRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private EntityManager entityManager;

    private UUID researcherId;
    private Study study;

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
        study = studyRepository.saveAndFlush(new Study(researcherId, "Questionnaire test", null));
    }

    @Test
    void v11CreatesUuidColumnsAndDeferredUniquenessConstraints() {
        assertThat(columnType("questionnaires", "id")).isEqualTo("uuid");
        assertThat(columnType("questionnaires", "study_id")).isEqualTo("uuid");
        assertThat(columnType("questionnaire_items", "question_id")).isEqualTo("uuid");

        assertThat(isDeferrable("uq_questionnaire_items_position")).isTrue();
        assertThat(isDeferrable("uq_questionnaire_items_question")).isTrue();
    }

    @Test
    void freshPostgresHasEverySuccessfulFlywayMigrationThroughV16() {
        List<String> versions = jdbcTemplate.queryForList(
                """
                        SELECT version
                        FROM flyway_schema_history
                        WHERE success = TRUE
                          AND version IS NOT NULL
                        ORDER BY installed_rank
                        """,
                String.class
        );

        assertThat(versions).containsExactly(
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16"
        );
    }

    @Test
    void v16PopulatesAllPlatformTemplatesWithoutChangingBlankTemplate() {
        assertThat(jdbcTemplate.queryForList(
                "SELECT code FROM feed_templates WHERE content IS NOT NULL AND schema_version = 1 ORDER BY code",
                String.class
        )).containsExactly("bluesky", "facebook", "instagram", "threads", "tiktok", "truth-social", "x");
        assertThat(jdbcTemplate.queryForObject(
                "SELECT content IS NULL AND schema_version IS NULL FROM feed_templates WHERE code = 'blank'",
                Boolean.class
        )).isTrue();
    }

    @Test
    void v12CreatesBranchChecksWithoutCouplingTriggersToMutableOptions() {
        assertThat(columnType("questionnaire_branch_rules", "source_option_id"))
                .isEqualTo("uuid");
        assertThat(columnType("questionnaire_branch_rules", "source_scale_value"))
                .isEqualTo("integer");
        assertThat(deleteAction("fk_branch_rules_source_item")).isEqualTo("CASCADE");
        assertThat(deleteAction("fk_branch_rules_target_item")).isEqualTo("RESTRICT");
        assertThat(constraintExists("fk_branch_rules_source_option")).isFalse();
    }

    @Test
    void v13CreatesSelfContainedJsonSnapshotWithOnePublicationPerStudy() {
        assertThat(columnType("questionnaire_publication_snapshots", "id")).isEqualTo("uuid");
        assertThat(columnType("questionnaire_publication_snapshots", "content")).isEqualTo("jsonb");
        assertThat(deleteAction("fk_questionnaire_publication_snapshots_study"))
                .isEqualTo("RESTRICT");
        assertThat(constraintExists("uq_questionnaire_publication_snapshots_study")).isTrue();
    }

    @Test
    void v12RejectsNonObjectSnapshotContent() {
        assertThatThrownBy(() -> insertSnapshot("[]"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void v12RejectsSecondSnapshotForTheSameStudy() {
        insertSnapshot("{\"items\":[]}");

        assertThatThrownBy(() -> insertSnapshot("{\"items\":[]}"))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void branchRuleRoundTripRetainsOptionTriggerAndStableTargetItem() {
        Questionnaire questionnaire = saveBranchedQuestionnaire();
        UUID sourceItemId = questionnaire.getItems().get(0).getId();
        UUID targetItemId = questionnaire.getItems().get(1).getId();
        UUID optionId = questionnaire.getItems().get(0).getBranchRules().stream()
                .findFirst()
                .orElseThrow()
                .getSourceOptionId();
        entityManager.clear();

        Questionnaire reloaded = questionnaireRepository.findByStudyId(study.getId())
                .orElseThrow();
        var reloadedRule = reloaded.getItems().get(0).getBranchRules().stream()
                .findFirst()
                .orElseThrow();

        assertThat(reloaded.getItems().get(0).getId()).isEqualTo(sourceItemId);
        assertThat(reloadedRule.getSourceOptionId()).isEqualTo(optionId);
        assertThat(reloadedRule.getSourceScaleValue()).isNull();
        assertThat(reloadedRule.getTargetItem().getId()).isEqualTo(targetItemId);
    }

    @Test
    void v11RejectsRulesWithoutExactlyOneTrigger() {
        Questionnaire questionnaire = saveBranchedQuestionnaire();
        UUID sourceItemId = questionnaire.getItems().get(0).getId();
        UUID targetItemId = questionnaire.getItems().get(1).getId();

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                        INSERT INTO questionnaire_branch_rules (
                            id, source_item_id, source_option_id, source_scale_value, target_item_id
                        ) VALUES (?, ?, NULL, NULL, ?)
                        """,
                UUID.randomUUID(),
                sourceItemId,
                targetItemId
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void v11RejectsSelfLoops() {
        Questionnaire questionnaire = saveBranchedQuestionnaire();
        UUID sourceItemId = questionnaire.getItems().get(0).getId();
        Question source = questionRepository.findById(
                questionnaire.getItems().get(0).getQuestionId()
        ).orElseThrow();
        UUID unusedOptionId = source.getOptions().get(1).getId();

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                        INSERT INTO questionnaire_branch_rules (
                            id, source_item_id, source_option_id, source_scale_value, target_item_id
                        ) VALUES (?, ?, ?, NULL, ?)
                        """,
                UUID.randomUUID(),
                sourceItemId,
                unusedOptionId,
                sourceItemId
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void v11RejectsDuplicateOptionTriggers() {
        Questionnaire questionnaire = saveBranchedQuestionnaire();
        UUID sourceItemId = questionnaire.getItems().get(0).getId();
        UUID targetItemId = questionnaire.getItems().get(1).getId();
        UUID optionId = questionnaire.getItems().get(0).getBranchRules().stream()
                .findFirst()
                .orElseThrow()
                .getSourceOptionId();

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                        INSERT INTO questionnaire_branch_rules (
                            id, source_item_id, source_option_id, source_scale_value, target_item_id
                        ) VALUES (?, ?, ?, NULL, ?)
                        """,
                UUID.randomUUID(),
                sourceItemId,
                optionId,
                targetItemId
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void v11RejectsDuplicateScaleTriggers() {
        Questionnaire questionnaire = saveScaleBranchedQuestionnaire();
        UUID sourceItemId = questionnaire.getItems().get(0).getId();
        UUID targetItemId = questionnaire.getItems().get(1).getId();

        assertThatThrownBy(() -> jdbcTemplate.update(
                """
                        INSERT INTO questionnaire_branch_rules (
                            id, source_item_id, source_option_id, source_scale_value, target_item_id
                        ) VALUES (?, ?, NULL, 3, ?)
                        """,
                UUID.randomUUID(),
                sourceItemId,
                targetItemId
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingBranchTargetIsRestricted() {
        Questionnaire questionnaire = saveBranchedQuestionnaire();
        UUID targetItemId = questionnaire.getItems().get(1).getId();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM questionnaire_items WHERE id = ?",
                targetItemId
        )).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void deletingBranchOptionLeavesDraftRuleForApplicationValidation() {
        Questionnaire questionnaire = saveBranchedQuestionnaire();
        UUID optionId = questionnaire.getItems().get(0).getBranchRules().stream()
                .findFirst()
                .orElseThrow()
                .getSourceOptionId();

        jdbcTemplate.update("DELETE FROM question_options WHERE id = ?", optionId);

        assertThat(count("question_options", "id", optionId)).isZero();
        assertThat(count("questionnaire_branch_rules", "source_option_id", optionId)).isOne();
    }

    @Test
    void deletingBranchSourceCascadesItsRules() {
        Questionnaire questionnaire = saveBranchedQuestionnaire();
        UUID sourceItemId = questionnaire.getItems().get(0).getId();

        jdbcTemplate.update("DELETE FROM questionnaire_items WHERE id = ?", sourceItemId);

        assertThat(count("questionnaire_branch_rules", "source_item_id", sourceItemId)).isZero();
    }

    @Test
    void differentialReorderKeepsItemIdsAndAdvancesParentVersion() {
        Question first = saveTextQuestion("First question");
        Question second = saveTextQuestion("Second question");
        Questionnaire questionnaire = Questionnaire.create(study.getId());
        questionnaire.synchronizeItems(List.of(
                new Questionnaire.ItemPlacement(null, first.getId()),
                new Questionnaire.ItemPlacement(null, second.getId())
        ));
        questionnaire.markModified();
        questionnaire = questionnaireRepository.saveAndFlush(questionnaire);
        UUID firstItemId = questionnaire.getItems().get(0).getId();
        UUID secondItemId = questionnaire.getItems().get(1).getId();
        long originalVersion = questionnaire.getLockVersion();

        boolean changed = questionnaire.synchronizeItems(List.of(
                new Questionnaire.ItemPlacement(secondItemId, second.getId()),
                new Questionnaire.ItemPlacement(firstItemId, first.getId())
        ));
        questionnaire.markModified();
        questionnaire = questionnaireRepository.saveAndFlush(questionnaire);
        jdbcTemplate.execute("SET CONSTRAINTS uq_questionnaire_items_position IMMEDIATE");

        assertThat(changed).isTrue();
        assertThat(questionnaire.getItems())
                .extracting(item -> item.getId())
                .containsExactly(secondItemId, firstItemId);
        assertThat(questionnaire.getItems())
                .extracting(item -> item.getPosition())
                .containsExactly(0, 1);
        assertThat(questionnaire.getLockVersion()).isEqualTo(originalVersion + 1);
    }

    @Test
    void deletingQuestionRetainsItemAsMissingReference() {
        Question question = saveTextQuestion("Delete from bank");
        Questionnaire questionnaire = saveQuestionnaire(question);
        UUID itemId = questionnaire.getItems().get(0).getId();

        questionRepository.deleteById(question.getId());
        questionRepository.flush();
        entityManager.clear();

        Questionnaire reloaded = questionnaireRepository.findByStudyId(study.getId()).orElseThrow();
        assertThat(reloaded.getItems()).hasSize(1);
        assertThat(reloaded.getItems().get(0).getId()).isEqualTo(itemId);
        assertThat(reloaded.getItems().get(0).getQuestionId()).isNull();
        assertThat(reloaded.getItems().get(0).getPosition()).isZero();
    }

    @Test
    void deletingStudyCascadesThroughQuestionnaireAndItems() {
        Question question = saveTextQuestion("Cascade check");
        Questionnaire questionnaire = saveQuestionnaire(question);
        UUID questionnaireId = questionnaire.getId();

        studyRepository.deleteById(study.getId());
        studyRepository.flush();
        entityManager.clear();

        assertThat(count("questionnaires", "id", questionnaireId)).isZero();
        assertThat(count("questionnaire_items", "questionnaire_id", questionnaireId)).isZero();
    }

    @Test
    void hundredItemReadPathUsesConstantThreeQueriesAndLoadsQuestionOptions() {
        List<Question> storedQuestions = questionRepository.saveAll(
                IntStream.range(0, 100)
                        .mapToObj(index -> choiceQuestion("Choice question " + index))
                        .toList()
        );
        questionRepository.flush();
        Questionnaire questionnaire = Questionnaire.create(study.getId());
        questionnaire.synchronizeItems(storedQuestions.stream()
                .map(question -> new Questionnaire.ItemPlacement(null, question.getId()))
                .toList());
        questionnaire.markModified();
        questionnaireRepository.saveAndFlush(questionnaire);
        entityManager.clear();

        Statistics statistics = entityManager.getEntityManagerFactory()
                .unwrap(SessionFactory.class)
                .getStatistics();
        statistics.clear();

        studyRepository.findByIdAndOwnerId(study.getId(), researcherId).orElseThrow();
        Questionnaire loaded = questionnaireRepository.findByStudyId(study.getId()).orElseThrow();
        Set<UUID> questionIds = loaded.getItems().stream()
                .map(item -> item.getQuestionId())
                .collect(java.util.stream.Collectors.toSet());
        List<Question> questions = questionRepository.findAllByResearcherIdAndIdIn(
                researcherId,
                questionIds
        );

        assertThat(statistics.getPrepareStatementCount()).isEqualTo(3L);
        assertThat(questions).hasSize(100);
        assertThat(questions)
                .allSatisfy(question -> assertThat(
                        entityManager.getEntityManagerFactory().getPersistenceUnitUtil()
                                .isLoaded(question, "options")
                ).isTrue());
    }

    private Questionnaire saveQuestionnaire(Question question) {
        Questionnaire questionnaire = Questionnaire.create(study.getId());
        questionnaire.synchronizeItems(List.of(
                new Questionnaire.ItemPlacement(null, question.getId())
        ));
        questionnaire.markModified();
        return questionnaireRepository.saveAndFlush(questionnaire);
    }

    private Questionnaire saveBranchedQuestionnaire() {
        Question source = choiceQuestion("Branch source");
        source = questionRepository.saveAndFlush(source);
        Question target = saveTextQuestion("Branch target");
        Questionnaire questionnaire = Questionnaire.create(study.getId());
        questionnaire.synchronizeItems(List.of(
                new Questionnaire.ItemPlacement(null, source.getId()),
                new Questionnaire.ItemPlacement(null, target.getId())
        ));
        questionnaire.replaceBranchRules(List.of(
                List.of(new Questionnaire.BranchRulePlacement(
                        source.getOptions().get(0).getId(),
                        null,
                        1
                )),
                List.of()
        ));
        questionnaire.markModified();
        return questionnaireRepository.saveAndFlush(questionnaire);
    }

    private Questionnaire saveScaleBranchedQuestionnaire() {
        Question source = questionRepository.saveAndFlush(Question.create(
                researcherId,
                QuestionType.SCALE,
                "Scale branch source",
                false,
                1,
                5,
                null,
                null
        ));
        Question target = saveTextQuestion("Scale branch target");
        Questionnaire questionnaire = Questionnaire.create(study.getId());
        questionnaire.synchronizeItems(List.of(
                new Questionnaire.ItemPlacement(null, source.getId()),
                new Questionnaire.ItemPlacement(null, target.getId())
        ));
        questionnaire.replaceBranchRules(List.of(
                List.of(new Questionnaire.BranchRulePlacement(null, 3, 1)),
                List.of()
        ));
        questionnaire.markModified();
        return questionnaireRepository.saveAndFlush(questionnaire);
    }

    private Question saveTextQuestion(String text) {
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

    private Question choiceQuestion(String text) {
        Question question = Question.create(
                researcherId,
                QuestionType.SINGLE_CHOICE,
                text,
                true,
                null,
                null,
                null,
                null
        );
        question.replaceOptions(List.of("Yes", "No"));
        return question;
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

    private boolean isDeferrable(String constraintName) {
        Boolean value = jdbcTemplate.queryForObject(
                """
                        SELECT condeferrable
                        FROM pg_constraint
                        WHERE conname = ?
                        """,
                Boolean.class,
                constraintName
        );
        return Boolean.TRUE.equals(value);
    }

    private String deleteAction(String constraintName) {
        return jdbcTemplate.queryForObject(
                """
                        SELECT rc.delete_rule
                        FROM information_schema.referential_constraints rc
                        WHERE rc.constraint_schema = current_schema()
                          AND rc.constraint_name = ?
                        """,
                String.class,
                constraintName
        );
    }

    private boolean constraintExists(String constraintName) {
        Boolean value = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = ?)",
                Boolean.class,
                constraintName
        );
        return Boolean.TRUE.equals(value);
    }

    private void insertSnapshot(String content) {
        jdbcTemplate.update(
                """
                        INSERT INTO questionnaire_publication_snapshots (
                            id,
                            study_id,
                            source_questionnaire_id,
                            questionnaire_version,
                            published_at,
                            content
                        ) VALUES (?, ?, ?, 0, CURRENT_TIMESTAMP, CAST(? AS jsonb))
                        """,
                UUID.randomUUID(),
                study.getId(),
                UUID.randomUUID(),
                content
        );
    }

    private long count(String table, String column, UUID id) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?",
                Long.class,
                id
        );
        return count == null ? 0L : count;
    }
}
