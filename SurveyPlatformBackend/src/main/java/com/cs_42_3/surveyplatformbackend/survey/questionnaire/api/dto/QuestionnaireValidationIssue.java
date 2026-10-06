package com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto;

import java.util.UUID;

/** A persisted draft-integrity issue exposed without dropping stable item or rule references. */
public record QuestionnaireValidationIssue(
        String code,
        Integer itemIndex,
        UUID itemId,
        Integer ruleIndex,
        String message
) {}
