package com.cs_42_3.surveyplatformbackend.survey.questionnaire.service.implementation;

import com.cs_42_3.surveyplatformbackend.security.CurrentResearcher;
import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.SurveyErrorDetail;
import com.cs_42_3.surveyplatformbackend.survey.api.mapper.QuestionResponseMapper;
import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionOption;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.QuestionnaireBranchRuleRequest;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.QuestionnaireBranchRuleResponse;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.QuestionnaireItemReferenceStatus;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.QuestionnaireItemResponse;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.QuestionnaireResponse;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.QuestionnaireSaveResult;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.QuestionnaireValidationIssue;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.SaveQuestionnaireItemRequest;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.SaveQuestionnaireRequest;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.domain.Questionnaire;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.domain.QuestionnaireBranchRule;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.domain.QuestionnaireItem;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.InvalidQuestionReferenceException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.InvalidQuestionnaireItemReferenceException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireLockedException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireNotFoundException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireValidationException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireVersionConflictException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.StudyNotFoundException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.repository.QuestionnaireRepository;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.service.QuestionnaireFlowValidator;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.service.QuestionnaireService;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.service.QuestionnaireSnapshotReadService;
import com.cs_42_3.surveyplatformbackend.survey.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Implements owner-scoped, versioned, whole-draft questionnaire saves including FR38. */
@Service
@RequiredArgsConstructor
public class QuestionnaireServiceImpl implements QuestionnaireService {

    private final QuestionnaireRepository questionnaireRepository;
    private final StudyRepository studyRepository;
    private final QuestionRepository questionRepository;
    private final CurrentResearcher currentResearcher;
    private final QuestionResponseMapper questionResponseMapper;
    private final QuestionnaireFlowValidator flowValidator;
    private final QuestionnaireSnapshotReadService snapshotReadService;

    @Override
    @Transactional(readOnly = true)
    public QuestionnaireResponse getQuestionnaire(UUID studyId) {
        UUID researcherId = currentResearcher.getId();
        Study study = studyRepository.findByIdAndOwnerId(studyId, researcherId)
                .orElseThrow(() -> new StudyNotFoundException(studyId));

        if (study.getStatus() != StudyStatus.DRAFT) {
            return snapshotReadService.getPublishedQuestionnaire(studyId);
        }

        Questionnaire questionnaire = questionnaireRepository.findByStudyId(studyId)
                .orElseThrow(() -> new QuestionnaireNotFoundException(studyId));
        Map<UUID, Question> questionsById = loadOwnedQuestions(
                researcherId,
                questionnaire.getItems().stream()
                        .map(QuestionnaireItem::getQuestionId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet()),
                false
        );
        return toResponse(questionnaire, questionsById);
    }

    @Override
    @Transactional
    public QuestionnaireSaveResult saveQuestionnaire(
            UUID studyId,
            SaveQuestionnaireRequest request
    ) {
        validateRequestStructure(request);

        UUID researcherId = currentResearcher.getId();
        Study study = studyRepository.findOwnedStudyForQuestionnaireUpdate(studyId, researcherId)
                .orElseThrow(() -> new StudyNotFoundException(studyId));
        if (study.getStatus() != StudyStatus.DRAFT) {
            throw new QuestionnaireLockedException(studyId);
        }

        Questionnaire questionnaire = questionnaireRepository.findByStudyIdForUpdate(studyId)
                .orElse(null);
        if (questionnaire != null) {
            // Load the full managed graph only after locking the questionnaire root row.
            questionnaireRepository.findByStudyId(studyId);
        }
        validateEarlyVersionState(questionnaire, request.expectedVersion());

        Set<UUID> questionIdsToLock = request.items().stream()
                .map(SaveQuestionnaireItemRequest::questionId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(HashSet::new));
        if (questionnaire != null) {
            questionnaire.getItems().stream()
                    .map(QuestionnaireItem::getQuestionId)
                    .filter(Objects::nonNull)
                    .forEach(questionIdsToLock::add);
        }
        Map<UUID, Question> lockedQuestions = loadOwnedQuestions(
                researcherId,
                questionIdsToLock,
                true
        );
        validateQuestionReferences(request.items(), lockedQuestions.keySet());
        validateItemReferences(questionnaire, request.items());

        List<Questionnaire.ItemPlacement> itemPlacements = questionnaire == null
                ? request.items().stream()
                        .map(item -> new Questionnaire.ItemPlacement(null, item.questionId()))
                        .toList()
                : resolvePlacements(questionnaire, request.items());
        List<List<Questionnaire.BranchRulePlacement>> rulesByPosition = buildRulePlans(
                request.items(),
                lockedQuestions
        );
        validateFlow(request.items(), itemPlacements, rulesByPosition, lockedQuestions);

        if (questionnaire != null) {
            boolean sameContent = hasSameContent(questionnaire, itemPlacements, rulesByPosition);
            validateRemainingVersionState(
                    questionnaire.getLockVersion(),
                    request.expectedVersion(),
                    sameContent
            );
            if (sameContent) {
                return new QuestionnaireSaveResult(
                        toResponse(questionnaire, lockedQuestions),
                        false
                );
            }

            if (questionnaire.clearBranchRules()) {
                questionnaireRepository.flush();
            }
            questionnaire.synchronizeItems(itemPlacements);
            questionnaire.replaceBranchRules(rulesByPosition);
            questionnaire.markModified();
            Questionnaire saved = questionnaireRepository.saveAndFlush(questionnaire);
            return new QuestionnaireSaveResult(toResponse(saved, lockedQuestions), false);
        }

        Questionnaire created = Questionnaire.create(studyId);
        created.synchronizeItems(itemPlacements);
        created.replaceBranchRules(rulesByPosition);
        created.markModified();
        created = questionnaireRepository.saveAndFlush(created);
        return new QuestionnaireSaveResult(toResponse(created, lockedQuestions), true);
    }

    private void validateRequestStructure(SaveQuestionnaireRequest request) {
        if (request == null) {
            throw new QuestionnaireValidationException("Questionnaire data is required");
        }
        if (request.expectedVersion() != null && request.expectedVersion() < 0) {
            throw new QuestionnaireValidationException("Expected version must be zero or greater");
        }
        if (request.items() == null) {
            throw new QuestionnaireValidationException("Questionnaire items are required");
        }

        List<SurveyErrorDetail> details = new ArrayList<>();
        Map<UUID, List<Integer>> itemIdIndexes = new LinkedHashMap<>();
        Map<UUID, List<Integer>> questionIdIndexes = new LinkedHashMap<>();
        for (int index = 0; index < request.items().size(); index++) {
            SaveQuestionnaireItemRequest item = request.items().get(index);
            if (item == null) {
                details.add(detail(
                        "items[" + index + "]",
                        index,
                        null,
                        null,
                        "ITEM_REQUIRED",
                        "Questionnaire item is required"
                ));
                continue;
            }
            if (item.itemId() != null) {
                itemIdIndexes.computeIfAbsent(item.itemId(), ignored -> new ArrayList<>()).add(index);
            }
            if (item.questionId() == null) {
                details.add(detail(
                        "items[" + index + "].questionId",
                        index,
                        item.itemId(),
                        null,
                        "QUESTION_ID_REQUIRED",
                        "Question ID is required"
                ));
            } else {
                questionIdIndexes.computeIfAbsent(item.questionId(), ignored -> new ArrayList<>())
                        .add(index);
            }
            for (int ruleIndex = 0; ruleIndex < item.branchRules().size(); ruleIndex++) {
                if (item.branchRules().get(ruleIndex) == null) {
                    details.add(detail(
                            "items[" + index + "].branchRules[" + ruleIndex + "]",
                            index,
                            item.itemId(),
                            ruleIndex,
                            "BRANCH_RULE_REQUIRED",
                            "Branch rule is required"
                    ));
                }
            }
        }

        addDuplicateDetails(
                details,
                request.items(),
                itemIdIndexes,
                "itemId",
                "DUPLICATE_ITEM_ID",
                "Questionnaire item ID is duplicated"
        );
        addDuplicateDetails(
                details,
                request.items(),
                questionIdIndexes,
                "questionId",
                "DUPLICATE_QUESTION_REFERENCE",
                "Question is referenced more than once"
        );
        if (!details.isEmpty()) {
            throw new QuestionnaireValidationException(
                    "Questionnaire request contains invalid items",
                    details
            );
        }
    }

    private void addDuplicateDetails(
            List<SurveyErrorDetail> details,
            List<SaveQuestionnaireItemRequest> items,
            Map<UUID, List<Integer>> indexesById,
            String fieldName,
            String code,
            String message
    ) {
        indexesById.values().stream()
                .filter(indexes -> indexes.size() > 1)
                .flatMap(Collection::stream)
                .forEach(index -> details.add(detail(
                        "items[" + index + "]." + fieldName,
                        index,
                        items.get(index).itemId(),
                        null,
                        code,
                        message
                )));
    }

    private void validateEarlyVersionState(Questionnaire questionnaire, Long expectedVersion) {
        if (questionnaire == null && expectedVersion != null) {
            throw new QuestionnaireVersionConflictException(
                    "Questionnaire does not exist at expected version " + expectedVersion
            );
        }
        if (questionnaire != null
                && expectedVersion != null
                && expectedVersion > questionnaire.getLockVersion()) {
            throw new QuestionnaireVersionConflictException(
                    "Expected version is newer than the stored questionnaire version"
            );
        }
    }

    private void validateRemainingVersionState(
            long currentVersion,
            Long expectedVersion,
            boolean sameContent
    ) {
        if (expectedVersion == null) {
            if (!sameContent) {
                throw new QuestionnaireVersionConflictException(
                        "Questionnaire already exists; reload it before saving changes"
                );
            }
            return;
        }
        if (expectedVersion < currentVersion && !sameContent) {
            throw new QuestionnaireVersionConflictException(
                    "Questionnaire was changed after the submitted version"
            );
        }
    }

    private Map<UUID, Question> loadOwnedQuestions(
            UUID researcherId,
            Set<UUID> questionIds,
            boolean forUpdate
    ) {
        List<Question> questions;
        if (questionIds.isEmpty()) {
            questions = List.of();
        } else if (forUpdate) {
            questionRepository.lockAllOwnedByIdsForUpdate(researcherId, questionIds);
            questions = questionRepository.findAllByResearcherIdAndIdIn(researcherId, questionIds);
        } else {
            questions = questionRepository.findAllByResearcherIdAndIdIn(researcherId, questionIds);
        }
        return questions.stream().collect(Collectors.toMap(
                Question::getId,
                Function.identity(),
                (existing, ignored) -> existing,
                HashMap::new
        ));
    }

    private void validateQuestionReferences(
            List<SaveQuestionnaireItemRequest> requestedItems,
            Set<UUID> loadedQuestionIds
    ) {
        List<SurveyErrorDetail> details = new ArrayList<>();
        for (int index = 0; index < requestedItems.size(); index++) {
            SaveQuestionnaireItemRequest item = requestedItems.get(index);
            if (!loadedQuestionIds.contains(item.questionId())) {
                details.add(detail(
                        "items[" + index + "].questionId",
                        index,
                        item.itemId(),
                        null,
                        "INVALID_QUESTION_REFERENCE",
                        "Question does not exist or is not owned by the current researcher"
                ));
            }
        }
        if (!details.isEmpty()) {
            throw new InvalidQuestionReferenceException(details);
        }
    }

    private void validateItemReferences(
            Questionnaire questionnaire,
            List<SaveQuestionnaireItemRequest> requestedItems
    ) {
        Set<UUID> existingItemIds = questionnaire == null
                ? Set.of()
                : questionnaire.getItems().stream()
                        .map(QuestionnaireItem::getId)
                        .collect(Collectors.toSet());
        List<SurveyErrorDetail> details = new ArrayList<>();
        for (int index = 0; index < requestedItems.size(); index++) {
            UUID itemId = requestedItems.get(index).itemId();
            if (itemId != null && !existingItemIds.contains(itemId)) {
                details.add(detail(
                        "items[" + index + "].itemId",
                        index,
                        itemId,
                        null,
                        "INVALID_ITEM_REFERENCE",
                        "Item does not belong to this questionnaire"
                ));
            }
        }
        if (!details.isEmpty()) {
            throw new InvalidQuestionnaireItemReferenceException(details);
        }
    }

    private List<Questionnaire.ItemPlacement> resolvePlacements(
            Questionnaire questionnaire,
            List<SaveQuestionnaireItemRequest> requestedItems
    ) {
        List<QuestionnaireItem> existingItems = questionnaire.getItems();
        Set<UUID> usedItemIds = new HashSet<>();
        List<Questionnaire.ItemPlacement> placements = new ArrayList<>();

        for (SaveQuestionnaireItemRequest requestedItem : requestedItems) {
            UUID itemId = requestedItem.itemId();
            if (itemId == null) {
                itemId = existingItems.stream()
                        .filter(item -> item.getId() != null)
                        .filter(item -> !usedItemIds.contains(item.getId()))
                        .filter(item -> Objects.equals(item.getQuestionId(), requestedItem.questionId()))
                        .map(QuestionnaireItem::getId)
                        .findFirst()
                        .orElse(null);
            }
            if (itemId != null) {
                usedItemIds.add(itemId);
            }
            placements.add(new Questionnaire.ItemPlacement(itemId, requestedItem.questionId()));
        }
        return List.copyOf(placements);
    }

    private List<List<Questionnaire.BranchRulePlacement>> buildRulePlans(
            List<SaveQuestionnaireItemRequest> requestedItems,
            Map<UUID, Question> questionsById
    ) {
        List<List<Questionnaire.BranchRulePlacement>> result = new ArrayList<>();
        for (int sourcePosition = 0; sourcePosition < requestedItems.size(); sourcePosition++) {
            SaveQuestionnaireItemRequest item = requestedItems.get(sourcePosition);
            Question question = questionsById.get(item.questionId());
            List<QuestionnaireBranchRuleRequest> rules = item.branchRules();
            if (!rules.isEmpty()
                    && question.getType() != QuestionType.SINGLE_CHOICE
                    && question.getType() != QuestionType.SCALE) {
                throw invalidRule(
                        item,
                        sourcePosition,
                        0,
                        "BRANCH_TYPE_UNSUPPORTED",
                        "Branch rules are only supported for SINGLE_CHOICE and SCALE questions"
                );
            }

            Set<UUID> validOptionIds = question.getOptions().stream()
                    .map(QuestionOption::getId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
            Set<Object> triggers = new HashSet<>();
            List<Questionnaire.BranchRulePlacement> plans = new ArrayList<>();
            for (int ruleIndex = 0; ruleIndex < rules.size(); ruleIndex++) {
                QuestionnaireBranchRuleRequest rule = rules.get(ruleIndex);
                boolean hasOption = rule.sourceOptionId() != null;
                boolean hasScale = rule.sourceScaleValue() != null;
                if (hasOption == hasScale) {
                    throw invalidRule(
                            item,
                            sourcePosition,
                            ruleIndex,
                            "BRANCH_TRIGGER_REQUIRED",
                            "Exactly one of sourceOptionId or sourceScaleValue is required"
                    );
                }

                Object trigger;
                if (question.getType() == QuestionType.SINGLE_CHOICE) {
                    if (!hasOption) {
                        throw invalidRule(
                                item,
                                sourcePosition,
                                ruleIndex,
                                "OPTION_TRIGGER_REQUIRED",
                                "SINGLE_CHOICE branch rules require sourceOptionId"
                        );
                    }
                    if (!validOptionIds.contains(rule.sourceOptionId())) {
                        throw invalidRule(
                                item,
                                sourcePosition,
                                ruleIndex,
                                "INVALID_OPTION_TRIGGER",
                                "Branch option does not belong to the source question"
                        );
                    }
                    trigger = rule.sourceOptionId();
                } else {
                    if (!hasScale) {
                        throw invalidRule(
                                item,
                                sourcePosition,
                                ruleIndex,
                                "SCALE_TRIGGER_REQUIRED",
                                "SCALE branch rules require sourceScaleValue"
                        );
                    }
                    int scaleValue = rule.sourceScaleValue();
                    if (scaleValue < question.getScaleMin() || scaleValue > question.getScaleMax()) {
                        throw invalidRule(
                                item,
                                sourcePosition,
                                ruleIndex,
                                "SCALE_TRIGGER_OUT_OF_RANGE",
                                "Branch scale value is outside the source question range"
                        );
                    }
                    trigger = scaleValue;
                }

                if (!triggers.add(trigger)) {
                    throw invalidRule(
                            item,
                            sourcePosition,
                            ruleIndex,
                            "DUPLICATE_BRANCH_TRIGGER",
                            "A source answer can have only one branch target"
                    );
                }
                Integer targetPosition = rule.targetPosition();
                if (targetPosition == null
                        || targetPosition < 0
                        || targetPosition >= requestedItems.size()) {
                    throw invalidRule(
                            item,
                            sourcePosition,
                            ruleIndex,
                            "BRANCH_TARGET_OUT_OF_RANGE",
                            "Branch targetPosition is outside the final items array"
                    );
                }
                if (targetPosition == sourcePosition) {
                    throw invalidRule(
                            item,
                            sourcePosition,
                            ruleIndex,
                            "BRANCH_SELF_LOOP",
                            "A branch rule cannot target its own item"
                    );
                }
                plans.add(new Questionnaire.BranchRulePlacement(
                        rule.sourceOptionId(),
                        rule.sourceScaleValue(),
                        targetPosition
                ));
            }
            result.add(List.copyOf(plans));
        }
        return List.copyOf(result);
    }

    private void validateFlow(
            List<SaveQuestionnaireItemRequest> requestedItems,
            List<Questionnaire.ItemPlacement> itemPlacements,
            List<List<Questionnaire.BranchRulePlacement>> rulesByPosition,
            Map<UUID, Question> questionsById
    ) {
        List<QuestionnaireFlowValidator.FlowItem> flowItems = new ArrayList<>();
        for (int position = 0; position < requestedItems.size(); position++) {
            List<QuestionnaireFlowValidator.FlowRule> flowRules = new ArrayList<>();
            List<Questionnaire.BranchRulePlacement> plans = rulesByPosition.get(position);
            for (int ruleIndex = 0; ruleIndex < plans.size(); ruleIndex++) {
                Questionnaire.BranchRulePlacement plan = plans.get(ruleIndex);
                flowRules.add(new QuestionnaireFlowValidator.FlowRule(
                        plan.sourceOptionId(),
                        plan.sourceScaleValue(),
                        plan.targetPosition(),
                        ruleIndex
                ));
            }
            flowItems.add(new QuestionnaireFlowValidator.FlowItem(
                    itemPlacements.get(position).itemId(),
                    questionsById.get(requestedItems.get(position).questionId()),
                    flowRules
            ));
        }
        flowValidator.validate(flowItems);
    }

    private boolean hasSameContent(
            Questionnaire questionnaire,
            List<Questionnaire.ItemPlacement> placements,
            List<List<Questionnaire.BranchRulePlacement>> rulesByPosition
    ) {
        List<QuestionnaireItem> currentItems = questionnaire.getItems();
        if (currentItems.size() != placements.size()) {
            return false;
        }
        Map<QuestionnaireItem, Integer> positionByItem = new HashMap<>();
        for (QuestionnaireItem item : currentItems) {
            positionByItem.put(item, item.getPosition());
        }
        for (int position = 0; position < placements.size(); position++) {
            QuestionnaireItem current = currentItems.get(position);
            Questionnaire.ItemPlacement requested = placements.get(position);
            if (!Objects.equals(current.getId(), requested.itemId())
                    || !Objects.equals(current.getQuestionId(), requested.questionId())
                    || current.getPosition() != position) {
                return false;
            }
            Set<RuleContent> currentRules = current.getBranchRules().stream()
                    .map(rule -> new RuleContent(
                            rule.getSourceOptionId(),
                            rule.getSourceScaleValue(),
                            positionByItem.get(rule.getTargetItem())
                    ))
                    .collect(Collectors.toSet());
            Set<RuleContent> requestedRules = rulesByPosition.get(position).stream()
                    .map(rule -> new RuleContent(
                            rule.sourceOptionId(),
                            rule.sourceScaleValue(),
                            rule.targetPosition()
                    ))
                    .collect(Collectors.toSet());
            if (!currentRules.equals(requestedRules)) {
                return false;
            }
        }
        return true;
    }

    private QuestionnaireResponse toResponse(
            Questionnaire questionnaire,
            Map<UUID, Question> questionsById
    ) {
        List<QuestionnaireItem> items = questionnaire.getItems();
        Map<QuestionnaireItem, Integer> positionByItem = new HashMap<>();
        for (QuestionnaireItem item : items) {
            positionByItem.put(item, item.getPosition());
        }

        List<QuestionnaireValidationIssue> issues = new ArrayList<>();
        List<QuestionnaireItemResponse> itemResponses = new ArrayList<>();
        for (QuestionnaireItem item : items) {
            Question question = item.getQuestionId() == null
                    ? null
                    : questionsById.get(item.getQuestionId());
            boolean missing = question == null;
            if (missing) {
                issues.add(new QuestionnaireValidationIssue(
                        "MISSING_QUESTION",
                        item.getPosition(),
                        item.getId(),
                        null,
                        "Questionnaire item no longer resolves to a question-bank entry"
                ));
            }

            List<QuestionnaireBranchRule> sortedRules = item.getBranchRules().stream()
                    .sorted(Comparator.comparing(this::branchRuleSortKey))
                    .toList();
            List<QuestionnaireBranchRuleResponse> branchRules = new ArrayList<>();
            for (int ruleIndex = 0; ruleIndex < sortedRules.size(); ruleIndex++) {
                QuestionnaireBranchRule rule = sortedRules.get(ruleIndex);
                if (question != null) {
                    addLiveRuleIssue(question, item, rule, ruleIndex, issues);
                }
                QuestionnaireItem target = rule.getTargetItem();
                Integer targetPosition = positionByItem.get(target);
                if (targetPosition == null) {
                    issues.add(new QuestionnaireValidationIssue(
                            "BRANCH_TARGET_OUTSIDE_QUESTIONNAIRE",
                            item.getPosition(),
                            item.getId(),
                            ruleIndex,
                            "Branch target does not belong to this questionnaire"
                    ));
                } else {
                    Question targetQuestion = target.getQuestionId() == null
                            ? null
                            : questionsById.get(target.getQuestionId());
                    if (targetQuestion == null) {
                        issues.add(new QuestionnaireValidationIssue(
                                "BRANCH_TARGET_MISSING_QUESTION",
                                item.getPosition(),
                                item.getId(),
                                ruleIndex,
                                "Branch target item no longer resolves to a question-bank entry"
                        ));
                    }
                }
                branchRules.add(new QuestionnaireBranchRuleResponse(
                        rule.getId(),
                        rule.getSourceOptionId(),
                        rule.getSourceScaleValue(),
                        target.getId(),
                        targetPosition
                ));
            }

            itemResponses.add(new QuestionnaireItemResponse(
                    item.getId(),
                    item.getPosition(),
                    missing,
                    question == null ? null : questionResponseMapper.toResponse(question),
                    missing
                            ? QuestionnaireItemReferenceStatus.MISSING_QUESTION
                            : QuestionnaireItemReferenceStatus.VALID,
                    branchRules,
                    item.getPosition() + 1 < items.size()
                            ? items.get(item.getPosition() + 1).getId()
                            : null
            ));
        }
        return new QuestionnaireResponse(
                questionnaire.getId(),
                questionnaire.getStudyId(),
                itemResponses,
                questionnaire.getLockVersion(),
                questionnaire.getUpdatedAt(),
                issues.isEmpty(),
                issues
        );
    }

    private void addLiveRuleIssue(
            Question question,
            QuestionnaireItem item,
            QuestionnaireBranchRule rule,
            int ruleIndex,
            List<QuestionnaireValidationIssue> issues
    ) {
        String code = null;
        String message = null;
        if (question.getType() == QuestionType.SINGLE_CHOICE) {
            Set<UUID> optionIds = question.getOptions().stream()
                    .map(QuestionOption::getId)
                    .collect(Collectors.toSet());
            if (rule.getSourceOptionId() == null
                    || rule.getSourceScaleValue() != null
                    || !optionIds.contains(rule.getSourceOptionId())) {
                code = "INVALID_OPTION_TRIGGER";
                message = "Branch option no longer belongs to the current source question";
            }
        } else if (question.getType() == QuestionType.SCALE) {
            Integer value = rule.getSourceScaleValue();
            if (rule.getSourceOptionId() != null
                    || value == null
                    || question.getScaleMin() == null
                    || question.getScaleMax() == null
                    || value < question.getScaleMin()
                    || value > question.getScaleMax()) {
                code = "SCALE_TRIGGER_OUT_OF_RANGE";
                message = "Branch scale value is outside the current source range";
            }
        } else {
            code = "BRANCH_TYPE_UNSUPPORTED";
            message = "Only SINGLE_CHOICE and SCALE items may have conditional branches";
        }
        if (code != null) {
            issues.add(new QuestionnaireValidationIssue(
                    code,
                    item.getPosition(),
                    item.getId(),
                    ruleIndex,
                    message
            ));
        }
    }

    private String branchRuleSortKey(QuestionnaireBranchRule rule) {
        return rule.getSourceOptionId() == null
                ? "S:" + rule.getSourceScaleValue()
                : "O:" + rule.getSourceOptionId();
    }

    private QuestionnaireValidationException invalidRule(
            SaveQuestionnaireItemRequest item,
            int itemIndex,
            int ruleIndex,
            String code,
            String message
    ) {
        return new QuestionnaireValidationException(
                message,
                List.of(detail(
                        "items[" + itemIndex + "].branchRules[" + ruleIndex + "]",
                        itemIndex,
                        item.itemId(),
                        ruleIndex,
                        code,
                        message
                ))
        );
    }

    private SurveyErrorDetail detail(
            String field,
            int index,
            UUID itemId,
            Integer ruleIndex,
            String code,
            String message
    ) {
        return new SurveyErrorDetail(field, index, itemId, ruleIndex, code, message);
    }

    private record RuleContent(UUID optionId, Integer scaleValue, Integer targetPosition) {}
}
