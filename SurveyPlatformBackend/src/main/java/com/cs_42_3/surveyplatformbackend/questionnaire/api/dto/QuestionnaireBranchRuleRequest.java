package com.cs_42_3.surveyplatformbackend.questionnaire.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

/**
 * Request payload for one per-answer jump rule (FR-38).
 * <p>
 * Exactly one of {@code sourceOptionId} or {@code sourceScaleValue} must be set,
 * matching the enclosing item's question type; the service layer validates this
 * against that question's live type and content.
 */
public record QuestionnaireBranchRuleRequest(
        UUID sourceOptionId,
        Integer sourceScaleValue,
        @NotNull(message = "Target position is required")
        @PositiveOrZero(message = "Target position must not be negative")
        Integer targetPosition
) {}
