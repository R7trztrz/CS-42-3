package com.cs_42_3.surveyplatformbackend.survey.questionnaire.service.implementation;

import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionOption;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.exception.QuestionReferenceConflictException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.domain.Questionnaire;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.domain.QuestionnaireBranchRule;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.domain.QuestionnaireItem;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireValidationException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.repository.QuestionnaireRepository;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.service.QuestionnaireFlowValidator;
import com.cs_42_3.surveyplatformbackend.survey.repository.QuestionRepository;
import com.cs_42_3.surveyplatformbackend.survey.service.QuestionnaireQuestionReferencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/** Questionnaire-side implementation of question answer-domain reference protection. */
@Component
@RequiredArgsConstructor
public class QuestionnaireQuestionReferenceService
        implements QuestionnaireQuestionReferencePort {

    private final QuestionnaireRepository questionnaireRepository;
    private final StudyRepository studyRepository;
    private final QuestionRepository questionRepository;
    private final QuestionnaireFlowValidator flowValidator;

    @Override
    public ReferenceLock lockReferences(UUID questionId) {
        List<UUID> studyIds = questionnaireRepository.findReferencedStudyIds(questionId);
        List<Study> studies = studyIds.isEmpty()
                ? List.of()
                : studyRepository.lockAllByIdsForQuestionReferenceChange(studyIds);

        List<UUID> questionnaireIds = questionnaireRepository
                .findIdsReferencingQuestion(questionId);
        if (!questionnaireIds.isEmpty()) {
            questionnaireRepository.lockAllByIds(questionnaireIds);
        }

        return new ReferenceLock(
                new HashSet<>(studyIds),
                new HashSet<>(questionnaireIds),
                studies.stream().anyMatch(study -> study.getStatus() != StudyStatus.DRAFT)
        );
    }

    @Override
    public void validateUpdate(
            Question currentQuestion,
            ProposedQuestionDefinition proposedDefinition,
            ReferenceLock referenceLock
    ) {
        List<Questionnaire> references = loadCurrentReferences(currentQuestion.getId());
        rejectNonDraftReferences(references, referenceLock);
        validateDirectRules(currentQuestion.getId(), proposedDefinition, references);

        if (!answerDomainChanged(currentQuestion, proposedDefinition)) {
            return;
        }

        Question proposedQuestion = toQuestion(proposedDefinition, currentQuestion.getResearcherId());
        for (Questionnaire questionnaire : references) {
            validateQuestionnaireFlow(questionnaire, currentQuestion.getId(), proposedQuestion);
        }
    }

    @Override
    public void prepareDelete(Question currentQuestion, ReferenceLock referenceLock) {
        List<Questionnaire> references = loadCurrentReferences(currentQuestion.getId());
        rejectNonDraftReferences(references, referenceLock);

        for (Questionnaire questionnaire : references) {
            questionnaire.clearOutgoingBranchRules(currentQuestion.getId());
            questionnaire.markModified();
        }
        if (!references.isEmpty()) {
            questionnaireRepository.saveAll(references);
            questionnaireRepository.flush();
        }
    }

    private List<Questionnaire> loadCurrentReferences(UUID questionId) {
        return questionnaireRepository.findAllWithGraphReferencingQuestion(questionId);
    }

    private void rejectNonDraftReferences(
            List<Questionnaire> questionnaires,
            ReferenceLock referenceLock
    ) {
        if (referenceLock.hasNonDraftReference()) {
            throw conflict(
                    "QUESTION_REFERENCED_BY_NON_DRAFT",
                    "Question is referenced by a non-draft study and cannot be changed"
            );
        }
        Set<UUID> studyIds = questionnaires.stream()
                .map(Questionnaire::getStudyId)
                .collect(Collectors.toSet());
        if (studyIds.isEmpty()) {
            return;
        }
        boolean hasNonDraft = studyRepository.findAllById(studyIds).stream()
                .anyMatch(study -> study.getStatus() != StudyStatus.DRAFT);
        if (hasNonDraft) {
            throw conflict(
                    "QUESTION_REFERENCED_BY_NON_DRAFT",
                    "Question is referenced by a non-draft study and cannot be changed"
            );
        }
    }

    private void validateDirectRules(
            UUID questionId,
            ProposedQuestionDefinition proposed,
            List<Questionnaire> questionnaires
    ) {
        List<QuestionnaireBranchRule> rules = questionnaires.stream()
                .flatMap(questionnaire -> questionnaire.getItems().stream())
                .filter(item -> Objects.equals(item.getQuestionId(), questionId))
                .flatMap(item -> item.getBranchRules().stream())
                .toList();
        if (rules.isEmpty()) {
            return;
        }

        if (proposed.type() != QuestionType.SINGLE_CHOICE
                && proposed.type() != QuestionType.SCALE) {
            throw conflict(
                    "QUESTION_TYPE_IN_USE_BY_BRANCH_RULE",
                    "Question type cannot be changed while branch rules use this question"
            );
        }
        if (proposed.type() == QuestionType.SINGLE_CHOICE) {
            if (rules.stream().anyMatch(rule -> rule.getSourceOptionId() == null)) {
                throw conflict(
                        "QUESTION_TYPE_IN_USE_BY_BRANCH_RULE",
                        "Question type cannot change the trigger kind used by branch rules"
                );
            }
            boolean incompatible = rules.stream().anyMatch(rule ->
                    !proposed.retainedOptionIds().contains(rule.getSourceOptionId())
            );
            if (incompatible) {
                throw conflict(
                        "OPTION_IN_USE_BY_BRANCH_RULE",
                        "An option used by a branch rule cannot be removed or replaced"
                );
            }
            return;
        }

        if (rules.stream().anyMatch(rule -> rule.getSourceScaleValue() == null)) {
            throw conflict(
                    "QUESTION_TYPE_IN_USE_BY_BRANCH_RULE",
                    "Question type cannot change the trigger kind used by branch rules"
            );
        }
        boolean incompatible = rules.stream().anyMatch(rule ->
                rule.getSourceScaleValue() < proposed.scaleMin()
                        || rule.getSourceScaleValue() > proposed.scaleMax()
        );
        if (incompatible) {
            throw conflict(
                    "SCALE_TRIGGER_OUTSIDE_NEW_RANGE",
                    "The new scale range excludes a value used by a branch rule"
            );
        }
    }

    private boolean answerDomainChanged(
            Question current,
            ProposedQuestionDefinition proposed
    ) {
        if (current.getType() != proposed.type()
                || current.isRequired() != proposed.required()
                || !Objects.equals(current.getScaleMin(), proposed.scaleMin())
                || !Objects.equals(current.getScaleMax(), proposed.scaleMax())) {
            return true;
        }
        Set<UUID> currentOptionIds = current.getOptions().stream()
                .map(QuestionOption::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        return current.getOptions().size() != proposed.optionCount()
                || !currentOptionIds.equals(proposed.retainedOptionIds());
    }

    private Question toQuestion(ProposedQuestionDefinition proposed, UUID researcherId) {
        Question question = Question.create(
                researcherId,
                proposed.type(),
                "Proposed questionnaire definition",
                proposed.required(),
                proposed.scaleMin(),
                proposed.scaleMax(),
                null,
                null
        );
        if (proposed.type() == QuestionType.SINGLE_CHOICE
                || proposed.type() == QuestionType.MULTI_CHOICE) {
            question.replaceOptions(IntStream.range(0, proposed.optionCount())
                    .mapToObj(index -> "Option " + index)
                    .toList());
        }
        return question;
    }

    private void validateQuestionnaireFlow(
            Questionnaire questionnaire,
            UUID changedQuestionId,
            Question proposedQuestion
    ) {
        Set<UUID> questionIds = questionnaire.getItems().stream()
                .map(QuestionnaireItem::getQuestionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<UUID, Question> questionsById = questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(
                        Question::getId,
                        Function.identity(),
                        (existing, ignored) -> existing,
                        HashMap::new
                ));
        questionsById.put(changedQuestionId, proposedQuestion);

        Map<QuestionnaireItem, Integer> positionByItem = new HashMap<>();
        for (QuestionnaireItem item : questionnaire.getItems()) {
            positionByItem.put(item, item.getPosition());
        }

        List<QuestionnaireFlowValidator.FlowItem> flowItems = new ArrayList<>();
        for (QuestionnaireItem item : questionnaire.getItems()) {
            Question question = questionsById.get(item.getQuestionId());
            if (question == null) {
                throw conflict(
                        "QUESTIONNAIRE_HAS_MISSING_QUESTION",
                        "Question update cannot be validated because a questionnaire has a missing question"
                );
            }
            List<QuestionnaireFlowValidator.FlowRule> rules = new ArrayList<>();
            int ruleIndex = 0;
            for (QuestionnaireBranchRule rule : item.getBranchRules()) {
                Integer targetPosition = positionByItem.get(rule.getTargetItem());
                if (targetPosition == null) {
                    throw conflict(
                            "QUESTIONNAIRE_HAS_INVALID_BRANCH_TARGET",
                            "Questionnaire contains a branch target outside its item set"
                    );
                }
                rules.add(new QuestionnaireFlowValidator.FlowRule(
                        rule.getSourceOptionId(),
                        rule.getSourceScaleValue(),
                        targetPosition,
                        ruleIndex++
                ));
            }
            flowItems.add(new QuestionnaireFlowValidator.FlowItem(
                    item.getId(),
                    question,
                    rules
            ));
        }

        try {
            flowValidator.validate(flowItems);
        } catch (QuestionnaireValidationException exception) {
            throw conflict(
                    "QUESTION_UPDATE_INVALIDATES_FLOW",
                    "Question update would invalidate questionnaire flow: " + exception.getMessage()
            );
        }
    }

    private QuestionReferenceConflictException conflict(String code, String message) {
        return new QuestionReferenceConflictException(code, message);
    }
}
