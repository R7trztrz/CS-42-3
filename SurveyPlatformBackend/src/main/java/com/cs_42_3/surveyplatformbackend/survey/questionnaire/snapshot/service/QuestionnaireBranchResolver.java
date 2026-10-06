package com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.service;

import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.domain.QuestionnaireSnapshotPayload;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Stateless and deterministic branch resolver reusable by the M5 session layer. */
@Component
public class QuestionnaireBranchResolver {

    public QuestionnaireBranchResolution resolve(
            QuestionnaireSnapshotPayload snapshot,
            UUID currentItemId,
            StandardizedQuestionnaireAnswer answer
    ) {
        Objects.requireNonNull(snapshot, "Snapshot is required");
        Objects.requireNonNull(currentItemId, "Current item ID is required");
        Objects.requireNonNull(answer, "Answer is required");
        QuestionnaireSnapshotPayload.Item item = snapshot.items().stream()
                .filter(candidate -> currentItemId.equals(candidate.itemId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Current item is not in the snapshot"));

        validateAnswer(item, answer);
        UUID conditionalTarget = switch (item.type()) {
            case SINGLE_CHOICE -> item.branchRules().stream()
                    .filter(rule -> Objects.equals(answer.optionId(), rule.sourceOptionId()))
                    .map(QuestionnaireSnapshotPayload.BranchRule::targetItemId)
                    .findFirst()
                    .orElse(null);
            case SCALE -> item.branchRules().stream()
                    .filter(rule -> Objects.equals(answer.scaleValue(), rule.sourceScaleValue()))
                    .map(QuestionnaireSnapshotPayload.BranchRule::targetItemId)
                    .findFirst()
                    .orElse(null);
            case MULTI_CHOICE, TEXT -> null;
        };
        if (conditionalTarget != null) {
            return new QuestionnaireBranchResolution(
                    conditionalTarget,
                    QuestionnaireBranchResolution.Transition.CONDITIONAL
            );
        }
        if (item.defaultNextItemId() != null) {
            return new QuestionnaireBranchResolution(
                    item.defaultNextItemId(),
                    QuestionnaireBranchResolution.Transition.DEFAULT
            );
        }
        return new QuestionnaireBranchResolution(
                null,
                QuestionnaireBranchResolution.Transition.END
        );
    }

    private void validateAnswer(
            QuestionnaireSnapshotPayload.Item item,
            StandardizedQuestionnaireAnswer answer
    ) {
        Set<UUID> validOptionIds = item.options().stream()
                .map(QuestionnaireSnapshotPayload.Option::optionId)
                .collect(Collectors.toSet());
        if (item.type() == QuestionType.SINGLE_CHOICE) {
            if (answer.optionId() == null) {
                if (!item.required()) {
                    return;
                }
                throw new IllegalArgumentException("SINGLE_CHOICE answer must use a published optionId");
            }
            if (!validOptionIds.contains(answer.optionId())) {
                throw new IllegalArgumentException("SINGLE_CHOICE answer must use a published optionId");
            }
            return;
        }
        if (item.type() == QuestionType.SCALE) {
            if (answer.scaleValue() == null) {
                if (!item.required()) {
                    return;
                }
                throw new IllegalArgumentException("SCALE answer is outside the published range");
            }
            if (answer.scaleValue() < item.scaleMin()
                    || answer.scaleValue() > item.scaleMax()) {
                throw new IllegalArgumentException("SCALE answer is outside the published range");
            }
            return;
        }
        if (item.type() == QuestionType.MULTI_CHOICE) {
            if (!validOptionIds.containsAll(answer.optionIds())
                    || (item.required() && answer.optionIds().isEmpty())) {
                throw new IllegalArgumentException("MULTI_CHOICE answer contains invalid published options");
            }
            return;
        }
        if (item.required() && (answer.textValue() == null || answer.textValue().isBlank())) {
            throw new IllegalArgumentException("Required TEXT answer must not be blank");
        }
    }
}
