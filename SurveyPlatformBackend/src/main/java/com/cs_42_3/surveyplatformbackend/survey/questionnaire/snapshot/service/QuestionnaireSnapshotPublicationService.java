package com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.service;

import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.SurveyErrorDetail;
import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionOption;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.domain.Questionnaire;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.domain.QuestionnaireBranchRule;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.domain.QuestionnaireItem;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireValidationException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.repository.QuestionnaireRepository;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.service.QuestionnaireFlowValidator;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.domain.QuestionnaireSnapshot;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.domain.QuestionnaireSnapshotPayload;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.exception.QuestionnairePublicationException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.repository.QuestionnaireSnapshotRepository;
import com.cs_42_3.surveyplatformbackend.survey.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Locks, validates, and captures a complete questionnaire inside the publication transaction. */
@Service
@RequiredArgsConstructor
public class QuestionnaireSnapshotPublicationService {

    private final QuestionnaireRepository questionnaireRepository;
    private final QuestionRepository questionRepository;
    private final QuestionnaireSnapshotRepository snapshotRepository;
    private final QuestionnaireFlowValidator flowValidator;
    private final ObjectMapper objectMapper;

    public QuestionnaireSnapshot createSnapshot(Study study, Instant publishedAt) {
        if (snapshotRepository.existsByStudyId(study.getId())) {
            throw invalid("A questionnaire publication already exists for this study", List.of());
        }

        Questionnaire questionnaire = questionnaireRepository.findByStudyIdForUpdate(study.getId())
                .orElseThrow(() -> invalid(
                        "Questionnaire is enabled but no questionnaire exists",
                        List.of(detail("questionnaire", null, null, null,
                                "QUESTIONNAIRE_REQUIRED", "Create a questionnaire before publishing"))
                ));
        questionnaireRepository.findByStudyId(study.getId());

        List<QuestionnaireItem> items = questionnaire.getItems().stream()
                .sorted(Comparator.comparingInt(QuestionnaireItem::getPosition))
                .toList();
        if (items.isEmpty()) {
            throw invalid(
                    "Questionnaire must contain at least one item",
                    List.of(detail("items", null, null, null,
                            "QUESTIONNAIRE_EMPTY", "Add at least one question before publishing"))
            );
        }

        Set<UUID> questionIds = items.stream()
                .map(QuestionnaireItem::getQuestionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (!questionIds.isEmpty()) {
            questionRepository.lockAllOwnedByIdsForUpdate(study.getOwnerId(), questionIds);
        }
        Map<UUID, Question> questionsById = questionRepository
                .findAllByResearcherIdAndIdIn(study.getOwnerId(), questionIds)
                .stream()
                .collect(Collectors.toMap(Question::getId, Function.identity()));

        validateQuestionData(items, questionsById);
        List<List<QuestionnaireFlowValidator.FlowRule>> flowRules = validateBranchRules(
                items,
                questionsById
        );
        validateFlow(items, questionsById, flowRules);

        QuestionnaireSnapshotPayload payload = toPayload(questionnaire, items, questionsById);
        QuestionnaireSnapshot snapshot = QuestionnaireSnapshot.create(
                study.getId(),
                questionnaire.getId(),
                questionnaire.getLockVersion(),
                publishedAt,
                objectMapper.writeValueAsString(payload)
        );
        return snapshotRepository.saveAndFlush(snapshot);
    }

    private void validateQuestionData(
            List<QuestionnaireItem> items,
            Map<UUID, Question> questionsById
    ) {
        List<SurveyErrorDetail> details = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            QuestionnaireItem item = items.get(index);
            Question question = item.getQuestionId() == null
                    ? null
                    : questionsById.get(item.getQuestionId());
            if (question == null) {
                details.add(detail(
                        "items[" + index + "].questionId",
                        index,
                        item.getId(),
                        null,
                        "MISSING_QUESTION",
                        "Questionnaire item no longer resolves to an owned question"
                ));
                continue;
            }

            boolean valid = switch (question.getType()) {
                case SINGLE_CHOICE, MULTI_CHOICE -> question.getOptions().size() >= 2
                        && question.getOptions().stream().allMatch(option ->
                                option.getId() != null
                                        && option.getOptionText() != null
                                        && !option.getOptionText().isBlank());
                case SCALE -> question.getScaleMin() != null
                        && question.getScaleMax() != null
                        && question.getScaleMin() < question.getScaleMax()
                        && question.getOptions().isEmpty();
                case TEXT -> question.getScaleMin() == null
                        && question.getScaleMax() == null
                        && question.getOptions().isEmpty();
            };
            if (!valid) {
                details.add(detail(
                        "items[" + index + "].question",
                        index,
                        item.getId(),
                        null,
                        "INVALID_QUESTION_DATA",
                        "Question data is invalid for type " + question.getType()
                ));
            }
        }
        if (!details.isEmpty()) {
            throw invalid("Questionnaire contains missing or invalid questions", details);
        }
    }

    private List<List<QuestionnaireFlowValidator.FlowRule>> validateBranchRules(
            List<QuestionnaireItem> items,
            Map<UUID, Question> questionsById
    ) {
        Map<QuestionnaireItem, Integer> positions = new HashMap<>();
        for (int index = 0; index < items.size(); index++) {
            positions.put(items.get(index), index);
        }

        List<SurveyErrorDetail> details = new ArrayList<>();
        List<List<QuestionnaireFlowValidator.FlowRule>> result = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            QuestionnaireItem item = items.get(index);
            Question question = questionsById.get(item.getQuestionId());
            Set<UUID> optionIds = question.getOptions().stream()
                    .map(QuestionOption::getId)
                    .collect(Collectors.toSet());
            Set<Object> triggers = new HashSet<>();
            List<QuestionnaireFlowValidator.FlowRule> rules = new ArrayList<>();
            List<QuestionnaireBranchRule> sortedRules = item.getBranchRules().stream()
                    .sorted(Comparator.comparing(this::ruleSortKey))
                    .toList();
            for (int ruleIndex = 0; ruleIndex < sortedRules.size(); ruleIndex++) {
                QuestionnaireBranchRule rule = sortedRules.get(ruleIndex);
                Integer targetPosition = positions.get(rule.getTargetItem());
                String code = null;
                String message = null;
                Object trigger = rule.getSourceOptionId() != null
                        ? rule.getSourceOptionId()
                        : rule.getSourceScaleValue();

                if (question.getType() != QuestionType.SINGLE_CHOICE
                        && question.getType() != QuestionType.SCALE) {
                    code = "BRANCH_TYPE_UNSUPPORTED";
                    message = "Only SINGLE_CHOICE and SCALE items may have conditional branches";
                } else if ((rule.getSourceOptionId() == null) == (rule.getSourceScaleValue() == null)) {
                    code = "BRANCH_TRIGGER_REQUIRED";
                    message = "Exactly one branch trigger is required";
                } else if (question.getType() == QuestionType.SINGLE_CHOICE
                        && !optionIds.contains(rule.getSourceOptionId())) {
                    code = "INVALID_OPTION_TRIGGER";
                    message = "Branch option does not belong to the current source question";
                } else if (question.getType() == QuestionType.SCALE
                        && (rule.getSourceScaleValue() == null
                        || rule.getSourceScaleValue() < question.getScaleMin()
                        || rule.getSourceScaleValue() > question.getScaleMax())) {
                    code = "SCALE_TRIGGER_OUT_OF_RANGE";
                    message = "Branch scale value is outside the current source range";
                } else if (!triggers.add(trigger)) {
                    code = "DUPLICATE_BRANCH_TRIGGER";
                    message = "A source answer can have only one target";
                } else if (targetPosition == null) {
                    code = "INVALID_BRANCH_TARGET";
                    message = "Branch target is outside this questionnaire";
                } else if (targetPosition == index) {
                    code = "BRANCH_SELF_LOOP";
                    message = "A branch cannot target its source item";
                }

                if (code != null) {
                    details.add(detail(
                            "items[" + index + "].branchRules[" + ruleIndex + "]",
                            index,
                            item.getId(),
                            ruleIndex,
                            code,
                            message
                    ));
                } else {
                    rules.add(new QuestionnaireFlowValidator.FlowRule(
                            rule.getSourceOptionId(),
                            rule.getSourceScaleValue(),
                            targetPosition,
                            ruleIndex
                    ));
                }
            }
            result.add(List.copyOf(rules));
        }
        if (!details.isEmpty()) {
            throw invalid("Questionnaire contains invalid branch rules", details);
        }
        return List.copyOf(result);
    }

    private void validateFlow(
            List<QuestionnaireItem> items,
            Map<UUID, Question> questionsById,
            List<List<QuestionnaireFlowValidator.FlowRule>> rulesByPosition
    ) {
        List<QuestionnaireFlowValidator.FlowItem> flowItems = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            QuestionnaireItem item = items.get(index);
            flowItems.add(new QuestionnaireFlowValidator.FlowItem(
                    item.getId(),
                    questionsById.get(item.getQuestionId()),
                    rulesByPosition.get(index)
            ));
        }
        try {
            flowValidator.validate(flowItems);
        } catch (QuestionnaireValidationException exception) {
            throw invalid(exception.getMessage(), exception.getDetails());
        }
    }

    private QuestionnaireSnapshotPayload toPayload(
            Questionnaire questionnaire,
            List<QuestionnaireItem> items,
            Map<UUID, Question> questionsById
    ) {
        List<QuestionnaireSnapshotPayload.Item> snapshotItems = new ArrayList<>();
        for (int index = 0; index < items.size(); index++) {
            QuestionnaireItem item = items.get(index);
            Question question = questionsById.get(item.getQuestionId());
            UUID defaultNextItemId = index + 1 < items.size() ? items.get(index + 1).getId() : null;
            List<QuestionnaireSnapshotPayload.Option> options = question.getOptions().stream()
                    .map(option -> new QuestionnaireSnapshotPayload.Option(
                            option.getId(),
                            option.getOptionText(),
                            option.getOptionOrder()
                    ))
                    .toList();
            List<QuestionnaireSnapshotPayload.BranchRule> rules = item.getBranchRules().stream()
                    .sorted(Comparator.comparing(this::ruleSortKey))
                    .map(rule -> new QuestionnaireSnapshotPayload.BranchRule(
                            rule.getId(),
                            rule.getSourceOptionId(),
                            rule.getSourceScaleValue(),
                            rule.getTargetItem().getId()
                    ))
                    .toList();
            snapshotItems.add(new QuestionnaireSnapshotPayload.Item(
                    item.getId(),
                    question.getId(),
                    index,
                    question.getType(),
                    question.getQuestionText(),
                    question.isRequired(),
                    options,
                    question.getScaleMin(),
                    question.getScaleMax(),
                    question.getScaleMinLabel(),
                    question.getScaleMaxLabel(),
                    question.getCreatedAt(),
                    question.getUpdatedAt(),
                    defaultNextItemId,
                    rules
            ));
        }
        return new QuestionnaireSnapshotPayload(
                questionnaire.getId(),
                questionnaire.getStudyId(),
                questionnaire.getLockVersion(),
                questionnaire.getUpdatedAt(),
                snapshotItems
        );
    }

    private String ruleSortKey(QuestionnaireBranchRule rule) {
        return rule.getSourceOptionId() == null
                ? "S:" + rule.getSourceScaleValue()
                : "O:" + rule.getSourceOptionId();
    }

    private QuestionnairePublicationException invalid(
            String message,
            List<SurveyErrorDetail> details
    ) {
        return new QuestionnairePublicationException(message, details);
    }

    private SurveyErrorDetail detail(
            String field,
            Integer index,
            UUID itemId,
            Integer ruleIndex,
            String code,
            String message
    ) {
        return new SurveyErrorDetail(field, index, itemId, ruleIndex, code, message);
    }
}
