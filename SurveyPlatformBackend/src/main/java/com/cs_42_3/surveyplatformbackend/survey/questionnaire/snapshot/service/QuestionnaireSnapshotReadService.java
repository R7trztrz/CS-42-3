package com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.service;

import com.cs_42_3.surveyplatformbackend.survey.api.dto.QuestionOptionResponse;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.QuestionResponse;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.QuestionnaireBranchRuleResponse;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.QuestionnaireContentSource;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.QuestionnaireItemReferenceStatus;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.QuestionnaireItemResponse;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.QuestionnaireResponse;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireNotFoundException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.domain.QuestionnaireSnapshot;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.domain.QuestionnaireSnapshotPayload;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.repository.QuestionnaireSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Reads published questionnaire content without consulting mutable question-bank rows. */
@Service
@RequiredArgsConstructor
public class QuestionnaireSnapshotReadService {

    private final QuestionnaireSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public QuestionnaireResponse getPublishedQuestionnaire(UUID studyId) {
        QuestionnaireSnapshot snapshot = snapshotRepository.findByStudyId(studyId)
                .orElseThrow(() -> new QuestionnaireNotFoundException(studyId));
        QuestionnaireSnapshotPayload payload = readPayload(snapshot);
        List<QuestionnaireItemResponse> items = payload.items().stream()
                .sorted(Comparator.comparingInt(QuestionnaireSnapshotPayload.Item::position))
                .map(item -> new QuestionnaireItemResponse(
                        item.itemId(),
                        item.position(),
                        false,
                        new QuestionResponse(
                                item.sourceQuestionId(),
                                item.type(),
                                item.questionText(),
                                item.required(),
                                item.options().stream()
                                        .sorted(Comparator.comparingInt(
                                                QuestionnaireSnapshotPayload.Option::optionOrder
                                        ))
                                        .map(option -> new QuestionOptionResponse(
                                                option.optionId(),
                                                option.optionText(),
                                                option.optionOrder()
                                        ))
                                        .toList(),
                                item.scaleMin(),
                                item.scaleMax(),
                                item.scaleMinLabel(),
                                item.scaleMaxLabel(),
                                item.questionCreatedAt(),
                                item.questionUpdatedAt()
                        ),
                        QuestionnaireItemReferenceStatus.VALID,
                        item.branchRules().stream()
                                .map(rule -> new QuestionnaireBranchRuleResponse(
                                        rule.ruleId(),
                                        rule.sourceOptionId(),
                                        rule.sourceScaleValue(),
                                        rule.targetItemId(),
                                        targetPosition(payload, rule.targetItemId())
                                ))
                                .toList(),
                        item.defaultNextItemId()
                ))
                .toList();
        return new QuestionnaireResponse(
                payload.questionnaireId(),
                snapshot.getId(),
                studyId,
                QuestionnaireContentSource.PUBLISHED_SNAPSHOT,
                items,
                snapshot.getQuestionnaireVersion(),
                payload.sourceUpdatedAt(),
                snapshot.getPublishedAt(),
                true,
                List.of()
        );
    }

    public QuestionnaireSnapshotPayload getPayload(UUID studyId) {
        QuestionnaireSnapshot snapshot = snapshotRepository.findByStudyId(studyId)
                .orElseThrow(() -> new QuestionnaireNotFoundException(studyId));
        return readPayload(snapshot);
    }

    private QuestionnaireSnapshotPayload readPayload(QuestionnaireSnapshot snapshot) {
        QuestionnaireSnapshotPayload payload = objectMapper.readValue(
                snapshot.getContent(),
                QuestionnaireSnapshotPayload.class
        );
        if (!snapshot.getStudyId().equals(payload.studyId())) {
            throw new IllegalStateException("Questionnaire snapshot study identity is inconsistent");
        }
        return payload;
    }

    private Integer targetPosition(QuestionnaireSnapshotPayload payload, UUID targetItemId) {
        return payload.items().stream()
                .filter(item -> item.itemId().equals(targetItemId))
                .map(QuestionnaireSnapshotPayload.Item::position)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Snapshot branch target is missing"));
    }
}
