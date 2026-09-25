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
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** PostgreSQL integration coverage for V7/V8 constraints and questionnaire mappings. */
@SpringBootTest(properties = {
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "spring.jpa.properties.hibernate.generate_statistics=true"
})
@Import(TestcontainersConfiguration.class)
@Transactional
@Testcontainers(disabledWithoutDocker = true)
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
    void v7CreatesUuidColumnsAndDeferredUniquenessConstraints() {
        assertThat(columnType("questionnaires", "id")).isEqualTo("uuid");
        assertThat(columnType("questionnaires", "study_id")).isEqualTo("uuid");
        assertThat(columnType("questionnaire_items", "question_id")).isEqualTo("uuid");

        assertThat(isDeferrable("uq_questionnaire_items_position")).isTrue();
        assertThat(isDeferrable("uq_questionnaire_items_question")).isTrue();
    }

    @Test
    void v8CreatesBranchColumnsChecksAndRestrictiveReferences() {
        assertThat(columnType("questionnaire_branch_rules", "source_option_id"))
                .isEqualTo("uuid");
        assertThat(columnType("questionnaire_branch_rules", "source_scale_value"))
                .isEqualTo("integer");
        assertThat(deleteAction("fk_branch_rules_source_item")).isEqualTo("CASCADE");
        assertThat(deleteAction("fk_branch_rules_target_item")).isEqualTo("RESTRICT");
        assertThat(deleteAction("fk_branch_rules_source_option")).isEqualTo("RESTRICT");
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
    void v8RejectsRulesWithoutExactlyOneTrigger() {
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
    void v8RejectsSelfLoops() {
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
    void v8RejectsDuplicateOptionTriggers() {
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
    void v8RejectsDuplicateScaleTriggers() {
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
    void deletingBranchOptionIsRestricted() {
        Questionnaire questionnaire = saveBranchedQuestionnaire();
        UUID optionId = questionnaire.getItems().get(0).getBranchRules().stream()
                .findFirst()
                .orElseThrow()
                .getSourceOptionId();

        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM question_options WHERE id = ?",
                optionId
        )).isInstanceOf(DataIntegrityViolationException.class);
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

    private long count(String table, String column, UUID id) {
        Long count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM " + table + " WHERE " + column + " = ?",
                Long.class,
                id
        );
        return count == null ? 0L : count;
    }
}
