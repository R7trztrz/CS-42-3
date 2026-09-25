package com.cs_42_3.surveyplatformbackend.survey.questionnaire.service.implementation;

import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.cs_42_3.surveyplatformbackend.security.CurrentResearcher;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.SurveyErrorDetail;
import com.cs_42_3.surveyplatformbackend.survey.api.mapper.QuestionResponseMapper;
import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.QuestionnaireItemResponse;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.QuestionnaireResponse;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.QuestionnaireSaveResult;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.SaveQuestionnaireItemRequest;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.SaveQuestionnaireRequest;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.domain.Questionnaire;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.domain.QuestionnaireItem;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.InvalidQuestionReferenceException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.InvalidQuestionnaireItemReferenceException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireLockedException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireNotFoundException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireValidationException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireVersionConflictException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.StudyNotFoundException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.repository.QuestionnaireRepository;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.service.QuestionnaireService;
import com.cs_42_3.surveyplatformbackend.survey.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
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

/** Implements owner-scoped, versioned, whole-draft questionnaire saves. */
@Service
@RequiredArgsConstructor
public class QuestionnaireServiceImpl implements QuestionnaireService {

    private final QuestionnaireRepository questionnaireRepository;
    private final StudyRepository studyRepository;
    private final QuestionRepository questionRepository;
    private final CurrentResearcher currentResearcher;
    private final QuestionResponseMapper questionResponseMapper;

    @Override
    @Transactional(readOnly = true)
    public QuestionnaireResponse getQuestionnaire(UUID studyId) {
        UUID researcherId = currentResearcher.getId();
        studyRepository.findByIdAndOwnerId(studyId, researcherId)
                .orElseThrow(() -> new StudyNotFoundException(studyId));

        Questionnaire questionnaire = questionnaireRepository.findByStudyId(studyId)
                .orElseThrow(() -> new QuestionnaireNotFoundException(studyId));
        Map<UUID, Question> questionsById = loadOwnedQuestions(
                researcherId,
                questionnaire.getItems().stream()
                        .map(QuestionnaireItem::getQuestionId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
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

        Questionnaire questionnaire = questionnaireRepository.findByStudyId(studyId).orElse(null);
        validateEarlyVersionState(questionnaire, request.expectedVersion());

        Set<UUID> requestedQuestionIds = request.items().stream()
                .map(SaveQuestionnaireItemRequest::questionId)
                .collect(Collectors.toSet());
        Map<UUID, Question> questionsById = loadOwnedQuestions(
                researcherId,
                requestedQuestionIds
        );
        validateQuestionReferences(request.items(), questionsById.keySet());

        if (questionnaire != null) {
            validateItemReferences(questionnaire, request.items());
            List<Questionnaire.ItemPlacement> placements = resolvePlacements(
                    questionnaire,
                    request.items()
            );
            boolean sameContent = hasSameContent(questionnaire, placements);
            validateRemainingVersionState(
                    questionnaire.getLockVersion(),
                    request.expectedVersion(),
                    sameContent
            );
            if (sameContent) {
                return new QuestionnaireSaveResult(
                        toResponse(questionnaire, questionsById),
                        false
                );
            }

            boolean changed = questionnaire.synchronizeItems(placements);
            if (changed) {
                questionnaire.markModified();
                questionnaire = questionnaireRepository.saveAndFlush(questionnaire);
            }
            return new QuestionnaireSaveResult(
                    toResponse(questionnaire, questionsById),
                    false
            );
        }

        validateItemReferences(null, request.items());
        Questionnaire newQuestionnaire = Questionnaire.create(studyId);
        List<Questionnaire.ItemPlacement> placements = request.items().stream()
                .map(item -> new Questionnaire.ItemPlacement(null, item.questionId()))
                .toList();
        if (newQuestionnaire.synchronizeItems(placements)) {
            newQuestionnaire.markModified();
        }
        newQuestionnaire = questionnaireRepository.saveAndFlush(newQuestionnaire);
        return new QuestionnaireSaveResult(
                toResponse(newQuestionnaire, questionsById),
                true
        );
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
                        "ITEM_REQUIRED",
                        "Questionnaire item is required"
                ));
                continue;
            }
            if (item.itemId() != null) {
                itemIdIndexes.computeIfAbsent(item.itemId(), ignored -> new ArrayList<>())
                        .add(index);
            }
            if (item.questionId() == null) {
                details.add(detail(
                        "items[" + index + "].questionId",
                        index,
                        item.itemId(),
                        "QUESTION_ID_REQUIRED",
                        "Question ID is required"
                ));
            } else {
                questionIdIndexes.computeIfAbsent(item.questionId(), ignored -> new ArrayList<>())
                        .add(index);
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
            Set<UUID> questionIds
    ) {
        List<Question> questions = questionIds.isEmpty()
                ? List.of()
                : questionRepository.findAllByResearcherIdAndIdIn(researcherId, questionIds);
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
                        .filter(item -> Objects.equals(
                                item.getQuestionId(),
                                requestedItem.questionId()
                        ))
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

    private boolean hasSameContent(
            Questionnaire questionnaire,
            List<Questionnaire.ItemPlacement> placements
    ) {
        List<QuestionnaireItem> currentItems = questionnaire.getItems();
        if (currentItems.size() != placements.size()) {
            return false;
        }
        for (int index = 0; index < placements.size(); index++) {
            QuestionnaireItem current = currentItems.get(index);
            Questionnaire.ItemPlacement requested = placements.get(index);
            if (!Objects.equals(current.getId(), requested.itemId())
                    || !Objects.equals(current.getQuestionId(), requested.questionId())
                    || current.getPosition() != index) {
                return false;
            }
        }
        return true;
    }

    private QuestionnaireResponse toResponse(
            Questionnaire questionnaire,
            Map<UUID, Question> questionsById
    ) {
        List<QuestionnaireItemResponse> itemResponses = questionnaire.getItems().stream()
                .map(item -> {
                    Question question = item.getQuestionId() == null
                            ? null
                            : questionsById.get(item.getQuestionId());
                    return new QuestionnaireItemResponse(
                            item.getId(),
                            item.getPosition(),
                            question == null,
                            question == null ? null : questionResponseMapper.toResponse(question)
                    );
                })
                .toList();
        return new QuestionnaireResponse(
                questionnaire.getId(),
                questionnaire.getStudyId(),
                itemResponses,
                questionnaire.getLockVersion(),
                questionnaire.getUpdatedAt()
        );
    }

    private SurveyErrorDetail detail(
            String field,
            int index,
            UUID itemId,
            String code,
            String message
    ) {
        return new SurveyErrorDetail(field, index, itemId, code, message);
    }
}
