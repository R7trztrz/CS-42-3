package com.cs_42_3.surveyplatformbackend.survey.questionnaire.service.implementation;

import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.exception.QuestionReferenceConflictException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.domain.Questionnaire;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.repository.QuestionnaireRepository;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.service.QuestionnaireFlowValidator;
import com.cs_42_3.surveyplatformbackend.survey.repository.QuestionRepository;
import com.cs_42_3.surveyplatformbackend.survey.service.QuestionnaireQuestionReferencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionnaireQuestionReferenceServiceTest {

    private static final UUID RESEARCHER_ID = UUID.randomUUID();
    private static final UUID STUDY_ID = UUID.randomUUID();
    private static final UUID QUESTION_ID = UUID.randomUUID();
    private static final UUID SECOND_QUESTION_ID = UUID.randomUUID();
    private static final UUID THIRD_QUESTION_ID = UUID.randomUUID();

    @Mock
    private QuestionnaireRepository questionnaireRepository;
    @Mock
    private StudyRepository studyRepository;
    @Mock
    private QuestionRepository questionRepository;

    private QuestionnaireQuestionReferenceService service;
    private Study draftStudy;

    @BeforeEach
    void setUp() {
        service = new QuestionnaireQuestionReferenceService(
                questionnaireRepository,
                studyRepository,
                questionRepository,
                new QuestionnaireFlowValidator()
        );
        draftStudy = new Study(RESEARCHER_ID, "Draft", null);
        ReflectionTestUtils.setField(draftStudy, "id", STUDY_ID);
    }

    @Test
    void locksStudiesBeforeQuestionnairesAndReportsNonDraftReferences() {
        UUID questionnaireId = UUID.randomUUID();
        Study collectingStudy = new Study(RESEARCHER_ID, "Collecting", null);
        ReflectionTestUtils.setField(collectingStudy, "id", STUDY_ID);
        ReflectionTestUtils.setField(collectingStudy, "status", StudyStatus.COLLECTING);
        when(questionnaireRepository.findReferencedStudyIds(QUESTION_ID))
                .thenReturn(List.of(STUDY_ID));
        when(studyRepository.lockAllByIdsForQuestionReferenceChange(List.of(STUDY_ID)))
                .thenReturn(List.of(collectingStudy));
        when(questionnaireRepository.findIdsReferencingQuestion(QUESTION_ID))
                .thenReturn(List.of(questionnaireId));

        var lock = service.lockReferences(QUESTION_ID);

        assertThat(lock.studyIds()).containsExactly(STUDY_ID);
        assertThat(lock.questionnaireIds()).containsExactly(questionnaireId);
        assertThat(lock.hasNonDraftReference()).isTrue();
        verify(questionnaireRepository).lockAllByIds(List.of(questionnaireId));
    }

    @Test
    void rejectsUpdatesAndDeletesReportedByTheLockedNonDraftScope() {
        Question current = textQuestion(QUESTION_ID);
        var nonDraftLock = new QuestionnaireQuestionReferencePort.ReferenceLock(
                Set.of(STUDY_ID),
                Set.of(),
                true
        );
        var proposed = definition(
                QuestionType.TEXT,
                false,
                Set.of(),
                0,
                null,
                null
        );

        assertThatThrownBy(() -> service.validateUpdate(current, proposed, nonDraftLock))
                .isInstanceOfSatisfying(QuestionReferenceConflictException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo("QUESTION_REFERENCED_BY_NON_DRAFT"));
        assertThatThrownBy(() -> service.prepareDelete(current, nonDraftLock))
                .isInstanceOfSatisfying(QuestionReferenceConflictException.class, exception ->
                        assertThat(exception.getCode())
                                .isEqualTo("QUESTION_REFERENCED_BY_NON_DRAFT"));
    }

    @Test
    void rejectsRemovingAnOptionUsedByABranchRule() {
        UUID referencedOption = UUID.randomUUID();
        UUID retainedOption = UUID.randomUUID();
        Question current = choiceQuestion(QUESTION_ID, true, referencedOption, retainedOption);
        Questionnaire questionnaire = questionnaire(
                QUESTION_ID,
                SECOND_QUESTION_ID,
                THIRD_QUESTION_ID
        );
        questionnaire.replaceBranchRules(List.of(
                List.of(new Questionnaire.BranchRulePlacement(referencedOption, null, 2)),
                List.of(),
                List.of()
        ));
        stubDraftReferences(questionnaire);

        assertThatThrownBy(() -> service.validateUpdate(
                current,
                definition(QuestionType.SINGLE_CHOICE, true, Set.of(retainedOption), 2, null, null),
                emptyLock()
        )).isInstanceOfSatisfying(QuestionReferenceConflictException.class, exception ->
                assertThat(exception.getCode()).isEqualTo("OPTION_IN_USE_BY_BRANCH_RULE"));
    }

    @Test
    void rejectsScaleRangeThatExcludesAnExistingTrigger() {
        Question current = scaleQuestion(QUESTION_ID, false, 1, 10);
        Questionnaire questionnaire = questionnaire(QUESTION_ID, SECOND_QUESTION_ID);
        questionnaire.replaceBranchRules(List.of(
                List.of(new Questionnaire.BranchRulePlacement(null, 9, 1)),
                List.of()
        ));
        stubDraftReferences(questionnaire);

        assertThatThrownBy(() -> service.validateUpdate(
                current,
                definition(QuestionType.SCALE, false, Set.of(), 0, 1, 5),
                emptyLock()
        )).isInstanceOfSatisfying(QuestionReferenceConflictException.class, exception ->
                assertThat(exception.getCode()).isEqualTo("SCALE_TRIGGER_OUTSIDE_NEW_RANGE"));
    }

    @Test
    void revalidatesDefaultEdgesWhenTheAnswerDomainChanges() {
        UUID firstOption = UUID.randomUUID();
        UUID secondOption = UUID.randomUUID();
        UUID defaultOption = UUID.randomUUID();
        Question current = choiceQuestion(
                QUESTION_ID,
                true,
                firstOption,
                secondOption,
                defaultOption
        );
        Question second = textQuestion(SECOND_QUESTION_ID);
        Question third = textQuestion(THIRD_QUESTION_ID);
        Questionnaire questionnaire = questionnaire(
                QUESTION_ID,
                SECOND_QUESTION_ID,
                THIRD_QUESTION_ID
        );
        questionnaire.replaceBranchRules(List.of(
                List.of(
                        new Questionnaire.BranchRulePlacement(firstOption, null, 2),
                        new Questionnaire.BranchRulePlacement(secondOption, null, 2)
                ),
                List.of(),
                List.of()
        ));
        stubDraftReferences(questionnaire);
        when(questionRepository.findAllById(any()))
                .thenReturn(List.of(current, second, third));

        assertThatThrownBy(() -> service.validateUpdate(
                current,
                definition(
                        QuestionType.SINGLE_CHOICE,
                        true,
                        Set.of(firstOption, secondOption),
                        2,
                        null,
                        null
                ),
                emptyLock()
        )).isInstanceOfSatisfying(QuestionReferenceConflictException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("QUESTION_UPDATE_INVALIDATES_FLOW"));
    }

    @Test
    void revalidatesFlowWhenRequiredRemovesTheNoAnswerDefault() {
        UUID firstOption = UUID.randomUUID();
        UUID secondOption = UUID.randomUUID();
        Question current = choiceQuestion(
                QUESTION_ID,
                false,
                firstOption,
                secondOption
        );
        Question second = textQuestion(SECOND_QUESTION_ID);
        Question third = textQuestion(THIRD_QUESTION_ID);
        Questionnaire questionnaire = questionnaire(
                QUESTION_ID,
                SECOND_QUESTION_ID,
                THIRD_QUESTION_ID
        );
        questionnaire.replaceBranchRules(List.of(
                List.of(
                        new Questionnaire.BranchRulePlacement(firstOption, null, 2),
                        new Questionnaire.BranchRulePlacement(secondOption, null, 2)
                ),
                List.of(),
                List.of()
        ));
        stubDraftReferences(questionnaire);
        when(questionRepository.findAllById(any()))
                .thenReturn(List.of(current, second, third));

        assertThatThrownBy(() -> service.validateUpdate(
                current,
                definition(
                        QuestionType.SINGLE_CHOICE,
                        true,
                        Set.of(firstOption, secondOption),
                        2,
                        null,
                        null
                ),
                emptyLock()
        )).isInstanceOfSatisfying(QuestionReferenceConflictException.class, exception ->
                assertThat(exception.getCode()).isEqualTo("QUESTION_UPDATE_INVALIDATES_FLOW"));
    }

    @Test
    void revalidatesScaleRangeChangesEvenWhenNoTriggerFallsOutsideTheRange() {
        Question current = scaleQuestion(QUESTION_ID, true, 1, 3);
        Question second = textQuestion(SECOND_QUESTION_ID);
        Question third = textQuestion(THIRD_QUESTION_ID);
        Questionnaire questionnaire = questionnaire(
                QUESTION_ID,
                SECOND_QUESTION_ID,
                THIRD_QUESTION_ID
        );
        questionnaire.replaceBranchRules(List.of(
                List.of(
                        new Questionnaire.BranchRulePlacement(null, 1, 2),
                        new Questionnaire.BranchRulePlacement(null, 2, 2)
                ),
                List.of(),
                List.of()
        ));
        stubDraftReferences(questionnaire);
        when(questionRepository.findAllById(any()))
                .thenReturn(List.of(current, second, third));

        assertThatThrownBy(() -> service.validateUpdate(
                current,
                definition(QuestionType.SCALE, true, Set.of(), 0, 1, 2),
                emptyLock()
        )).isInstanceOfSatisfying(QuestionReferenceConflictException.class, exception ->
                assertThat(exception.getCode()).isEqualTo("QUESTION_UPDATE_INVALIDATES_FLOW"));
    }

    @Test
    void deletingAQuestionClearsOnlyItsOutgoingRulesAndPreservesIncomingRules() {
        UUID incomingOption = UUID.randomUUID();
        Question current = scaleQuestion(QUESTION_ID, false, 1, 5);
        Questionnaire questionnaire = questionnaire(
                SECOND_QUESTION_ID,
                QUESTION_ID,
                THIRD_QUESTION_ID
        );
        questionnaire.replaceBranchRules(List.of(
                List.of(new Questionnaire.BranchRulePlacement(incomingOption, null, 1)),
                List.of(new Questionnaire.BranchRulePlacement(null, 3, 2)),
                List.of()
        ));
        stubDraftReferences(questionnaire);

        service.prepareDelete(current, emptyLock());

        assertThat(questionnaire.getItems().get(0).getBranchRules()).hasSize(1);
        assertThat(questionnaire.getItems().get(1).getBranchRules()).isEmpty();
        verify(questionnaireRepository).saveAll(List.of(questionnaire));
        verify(questionnaireRepository).flush();
    }

    private void stubDraftReferences(Questionnaire questionnaire) {
        when(questionnaireRepository.findAllWithGraphReferencingQuestion(QUESTION_ID))
                .thenReturn(List.of(questionnaire));
        when(studyRepository.findAllById(Set.of(STUDY_ID))).thenReturn(List.of(draftStudy));
    }

    private Questionnaire questionnaire(UUID... questionIds) {
        Questionnaire questionnaire = Questionnaire.create(STUDY_ID);
        questionnaire.synchronizeItems(java.util.Arrays.stream(questionIds)
                .map(questionId -> new Questionnaire.ItemPlacement(null, questionId))
                .toList());
        ReflectionTestUtils.setField(questionnaire, "id", UUID.randomUUID());
        questionnaire.getItems().forEach(item ->
                ReflectionTestUtils.setField(item, "id", UUID.randomUUID()));
        return questionnaire;
    }

    private Question choiceQuestion(
            UUID questionId,
            boolean required,
            UUID... optionIds
    ) {
        Question question = Question.create(
                RESEARCHER_ID,
                QuestionType.SINGLE_CHOICE,
                "Choice",
                required,
                null,
                null,
                null,
                null
        );
        question.replaceOptions(java.util.stream.IntStream.range(0, optionIds.length)
                .mapToObj(index -> "Option " + index)
                .toList());
        ReflectionTestUtils.setField(question, "id", questionId);
        for (int index = 0; index < optionIds.length; index++) {
            ReflectionTestUtils.setField(question.getOptions().get(index), "id", optionIds[index]);
        }
        return question;
    }

    private Question scaleQuestion(UUID questionId, boolean required, int min, int max) {
        Question question = Question.create(
                RESEARCHER_ID,
                QuestionType.SCALE,
                "Scale",
                required,
                min,
                max,
                null,
                null
        );
        ReflectionTestUtils.setField(question, "id", questionId);
        return question;
    }

    private Question textQuestion(UUID questionId) {
        Question question = Question.create(
                RESEARCHER_ID,
                QuestionType.TEXT,
                "Text",
                false,
                null,
                null,
                null,
                null
        );
        ReflectionTestUtils.setField(question, "id", questionId);
        return question;
    }

    private QuestionnaireQuestionReferencePort.ProposedQuestionDefinition definition(
            QuestionType type,
            boolean required,
            Set<UUID> optionIds,
            int optionCount,
            Integer scaleMin,
            Integer scaleMax
    ) {
        return new QuestionnaireQuestionReferencePort.ProposedQuestionDefinition(
                type,
                required,
                optionIds,
                optionCount,
                scaleMin,
                scaleMax
        );
    }

    private QuestionnaireQuestionReferencePort.ReferenceLock emptyLock() {
        return new QuestionnaireQuestionReferencePort.ReferenceLock(Set.of(), Set.of(), false);
    }
}
