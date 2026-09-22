package com.cs_42_3.surveyplatformbackend.questionnaire.service.implementation;

import com.cs_42_3.surveyplatformbackend.questionnaire.api.dto.QuestionnaireBranchRuleRequest;
import com.cs_42_3.surveyplatformbackend.questionnaire.api.dto.QuestionnaireBranchRuleResponse;
import com.cs_42_3.surveyplatformbackend.questionnaire.api.dto.QuestionnaireItemRequest;
import com.cs_42_3.surveyplatformbackend.questionnaire.api.dto.QuestionnaireItemResponse;
import com.cs_42_3.surveyplatformbackend.questionnaire.api.dto.QuestionnaireResponse;
import com.cs_42_3.surveyplatformbackend.questionnaire.api.dto.SaveQuestionnaireRequest;
import com.cs_42_3.surveyplatformbackend.questionnaire.domain.Questionnaire;
import com.cs_42_3.surveyplatformbackend.questionnaire.domain.QuestionnaireBranchRule;
import com.cs_42_3.surveyplatformbackend.questionnaire.domain.QuestionnaireBranchRulePlan;
import com.cs_42_3.surveyplatformbackend.questionnaire.domain.QuestionnaireItem;
import com.cs_42_3.surveyplatformbackend.questionnaire.domain.QuestionnaireItemPlan;
import com.cs_42_3.surveyplatformbackend.questionnaire.exception.InvalidQuestionnaireDataException;
import com.cs_42_3.surveyplatformbackend.questionnaire.exception.QuestionnaireStudyNotFoundException;
import com.cs_42_3.surveyplatformbackend.questionnaire.repository.QuestionnaireRepository;
import com.cs_42_3.surveyplatformbackend.questionnaire.service.QuestionnaireService;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.QuestionResponse;
import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionOption;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.repository.QuestionRepository;
import com.cs_42_3.surveyplatformbackend.survey.security.CurrentResearcherProvider;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Implements researcher-owned questionnaire editing operations for FR36-FR39.
 * <p>
 * Enforces study ownership, question-bank ownership, branch-rule determinism (NFR-15),
 * transaction boundaries, and DTO mapping. Saves always replace the full item and
 * branch-rule set as one unit, mirroring how {@code Question} replaces its options.
 */
@Service
@RequiredArgsConstructor
public class QuestionnaireServiceImpl implements QuestionnaireService {

    private final QuestionnaireRepository questionnaireRepository;
    private final StudyRepository studyRepository;
    private final QuestionRepository questionRepository;
    private final CurrentResearcherProvider currentResearcherProvider;

    @Override
    @Transactional(readOnly = true)
    public QuestionnaireResponse getQuestionnaire(UUID studyId) {
        UUID researcherId = currentResearcherProvider.getCurrentResearcherId();
        requireOwnedStudy(studyId, researcherId);

        return questionnaireRepository.findByStudyId(studyId)
                .map(this::toResponse)
                .orElseGet(() -> emptyResponse(studyId));
    }

    @Override
    @Transactional
    public QuestionnaireResponse saveQuestionnaire(UUID studyId, SaveQuestionnaireRequest request) {
        UUID researcherId = currentResearcherProvider.getCurrentResearcherId();
        requireOwnedStudy(studyId, researcherId);

        List<QuestionnaireItemRequest> itemRequests = request.items();
        Map<UUID, Question> ownedQuestions = loadOwnedQuestions(itemRequests, researcherId);
        List<QuestionnaireItemPlan> plans = buildPlans(itemRequests, ownedQuestions);

        Questionnaire questionnaire = questionnaireRepository.findByStudyId(studyId)
                .orElseGet(() -> Questionnaire.create(studyId));
        questionnaire.replaceItems(plans);

        Questionnaire saved = questionnaireRepository.saveAndFlush(questionnaire);
        return toResponse(saved, ownedQuestions);
    }

    private void requireOwnedStudy(UUID studyId, UUID researcherId) {
        studyRepository.findByIdAndOwnerId(studyId, researcherId)
                .orElseThrow(() -> new QuestionnaireStudyNotFoundException(studyId));
    }

    // Resolves and ownership-checks every referenced question in one round trip, and
    // rejects a question enabled twice in the same questionnaire.
    private Map<UUID, Question> loadOwnedQuestions(
            List<QuestionnaireItemRequest> itemRequests,
            UUID researcherId
    ) {
        List<UUID> questionIds = new ArrayList<>(itemRequests.size());
        Set<UUID> seen = new HashSet<>();
        for (QuestionnaireItemRequest itemRequest : itemRequests) {
            if (itemRequest == null || itemRequest.questionId() == null) {
                throw new InvalidQuestionnaireDataException("Every item must reference a question");
            }
            if (!seen.add(itemRequest.questionId())) {
                throw new InvalidQuestionnaireDataException(
                        "Question " + itemRequest.questionId() + " is enabled more than once"
                );
            }
            questionIds.add(itemRequest.questionId());
        }

        Map<UUID, Question> ownedQuestions = questionRepository
                .findAllByIdInAndResearcherId(questionIds, researcherId).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        for (UUID questionId : questionIds) {
            if (!ownedQuestions.containsKey(questionId)) {
                throw new InvalidQuestionnaireDataException(
                        "Question not found in your question bank: " + questionId
                );
            }
        }

        return ownedQuestions;
    }

    private List<QuestionnaireItemPlan> buildPlans(
            List<QuestionnaireItemRequest> itemRequests,
            Map<UUID, Question> ownedQuestions
    ) {
        List<QuestionnaireItemPlan> plans = new ArrayList<>(itemRequests.size());
        for (int position = 0; position < itemRequests.size(); position++) {
            QuestionnaireItemRequest itemRequest = itemRequests.get(position);
            Question question = ownedQuestions.get(itemRequest.questionId());
            List<QuestionnaireBranchRulePlan> rulePlans = buildRulePlans(
                    itemRequest.branchRules(),
                    question,
                    position,
                    itemRequests.size()
            );
            plans.add(new QuestionnaireItemPlan(itemRequest.questionId(), rulePlans));
        }
        return plans;
    }

    private List<QuestionnaireBranchRulePlan> buildRulePlans(
            List<QuestionnaireBranchRuleRequest> ruleRequests,
            Question question,
            int sourcePosition,
            int itemCount
    ) {
        if (ruleRequests == null || ruleRequests.isEmpty()) {
            return List.of();
        }
        // Only these two types resolve one selected answer to exactly one trigger value;
        // MULTI_CHOICE could select several options at once with conflicting targets,
        // which would make the "answer -> next item" jump table ambiguous (NFR-15).
        if (question.getType() != QuestionType.SINGLE_CHOICE && question.getType() != QuestionType.SCALE) {
            throw new InvalidQuestionnaireDataException(
                    "Branch rules are only supported for SINGLE_CHOICE and SCALE questions: "
                            + question.getId()
            );
        }

        Set<UUID> validOptionIds = question.getType() == QuestionType.SINGLE_CHOICE
                ? question.getOptions().stream().map(QuestionOption::getId).collect(Collectors.toSet())
                : Set.of();

        Set<Object> triggers = new HashSet<>();
        List<QuestionnaireBranchRulePlan> rulePlans = new ArrayList<>(ruleRequests.size());
        for (QuestionnaireBranchRuleRequest ruleRequest : ruleRequests) {
            Object trigger = validateTrigger(ruleRequest, question, validOptionIds);
            if (!triggers.add(trigger)) {
                throw new InvalidQuestionnaireDataException(
                        "Duplicate branch rule trigger " + trigger + " on question " + question.getId()
                );
            }
            validateTarget(ruleRequest.targetPosition(), sourcePosition, itemCount);
            rulePlans.add(new QuestionnaireBranchRulePlan(
                    ruleRequest.sourceOptionId(),
                    ruleRequest.sourceScaleValue(),
                    ruleRequest.targetPosition()
            ));
        }
        return rulePlans;
    }

    // Returns the validated trigger value (an option ID or a scale value), used by the
    // caller to detect duplicate triggers on the same item.
    private Object validateTrigger(
            QuestionnaireBranchRuleRequest ruleRequest,
            Question question,
            Set<UUID> validOptionIds
    ) {
        boolean hasOption = ruleRequest.sourceOptionId() != null;
        boolean hasScale = ruleRequest.sourceScaleValue() != null;
        if (hasOption == hasScale) {
            throw new InvalidQuestionnaireDataException(
                    "Exactly one of sourceOptionId or sourceScaleValue is required"
            );
        }

        if (question.getType() == QuestionType.SINGLE_CHOICE) {
            if (!hasOption) {
                throw new InvalidQuestionnaireDataException(
                        "SINGLE_CHOICE branch rules require sourceOptionId: " + question.getId()
                );
            }
            if (!validOptionIds.contains(ruleRequest.sourceOptionId())) {
                throw new InvalidQuestionnaireDataException(
                        "Option does not belong to question " + question.getId()
                                + ": " + ruleRequest.sourceOptionId()
                );
            }
            return ruleRequest.sourceOptionId();
        }

        // SCALE is the only type left after buildRulePlans' type guard.
        if (!hasScale) {
            throw new InvalidQuestionnaireDataException(
                    "SCALE branch rules require sourceScaleValue: " + question.getId()
            );
        }
        int value = ruleRequest.sourceScaleValue();
        if (value < question.getScaleMin() || value > question.getScaleMax()) {
            throw new InvalidQuestionnaireDataException(
                    "Scale value " + value + " is outside [" + question.getScaleMin()
                            + ", " + question.getScaleMax() + "] for question " + question.getId()
            );
        }
        return ruleRequest.sourceScaleValue();
    }

    private void validateTarget(int targetPosition, int sourcePosition, int itemCount) {
        if (targetPosition < 0 || targetPosition >= itemCount) {
            throw new InvalidQuestionnaireDataException(
                    "Branch target position " + targetPosition + " is out of range"
            );
        }
        if (targetPosition == sourcePosition) {
            throw new InvalidQuestionnaireDataException("A branch rule cannot target its own item");
        }
    }

    private QuestionnaireResponse emptyResponse(UUID studyId) {
        return new QuestionnaireResponse(null, studyId, List.of(), null, null);
    }

    private QuestionnaireResponse toResponse(Questionnaire questionnaire) {
        List<UUID> questionIds = questionnaire.getItems().stream()
                .map(QuestionnaireItem::getQuestionId)
                .distinct()
                .toList();
        Map<UUID, Question> questions = questionRepository.findAllById(questionIds).stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));
        return toResponse(questionnaire, questions);
    }

    private QuestionnaireResponse toResponse(Questionnaire questionnaire, Map<UUID, Question> questions) {
        List<QuestionnaireItem> items = questionnaire.getItems();
        Map<UUID, Integer> positionByItemId = items.stream()
                .collect(Collectors.toMap(QuestionnaireItem::getId, QuestionnaireItem::getItemOrder));

        List<QuestionnaireItemResponse> itemResponses = items.stream()
                .map(item -> toItemResponse(item, questions.get(item.getQuestionId()), positionByItemId))
                .toList();

        return new QuestionnaireResponse(
                questionnaire.getId(),
                questionnaire.getStudyId(),
                itemResponses,
                questionnaire.getCreatedAt(),
                questionnaire.getUpdatedAt()
        );
    }

    private QuestionnaireItemResponse toItemResponse(
            QuestionnaireItem item,
            Question question,
            Map<UUID, Integer> positionByItemId
    ) {
        QuestionResponse questionResponse = question == null ? null : QuestionResponse.from(question);
        List<QuestionnaireBranchRuleResponse> branchRuleResponses = item.getBranchRules().stream()
                .map(rule -> toBranchRuleResponse(rule, positionByItemId))
                .toList();

        return new QuestionnaireItemResponse(
                item.getId(),
                item.getItemOrder(),
                questionResponse,
                branchRuleResponses
        );
    }

    private QuestionnaireBranchRuleResponse toBranchRuleResponse(
            QuestionnaireBranchRule rule,
            Map<UUID, Integer> positionByItemId
    ) {
        UUID targetItemId = rule.getTargetItem().getId();
        return new QuestionnaireBranchRuleResponse(
                rule.getId(),
                rule.getSourceOptionId(),
                rule.getSourceScaleValue(),
                targetItemId,
                positionByItemId.get(targetItemId)
        );
    }
}
