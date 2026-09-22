package com.cs_42_3.surveyplatformbackend.questionnaire.api.dto;

import java.util.UUID;

/**
 * Response model for one per-answer jump rule.
 */
public record QuestionnaireBranchRuleResponse(
        UUID id,
        UUID sourceOptionId,
        Integer sourceScaleValue,
        UUID targetItemId,
        int targetPosition
) {}
