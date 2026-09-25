package com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto;

import java.util.UUID;

/** Persisted deterministic transition returned with its stable and positional target. */
public record QuestionnaireBranchRuleResponse(
        UUID id,
        UUID sourceOptionId,
        Integer sourceScaleValue,
        UUID targetItemId,
        Integer targetPosition
) {}
