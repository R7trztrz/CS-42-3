package com.cs_42_3.surveyplatformbackend.survey.api.dto;

import java.util.UUID;

/**
 * Response model for one ordered choice in a survey question.
 */
public record QuestionOptionResponse(
        UUID id,
        String optionText,
        int optionOrder
) {}
