package com.cs_42_3.surveyplatformbackend.survey.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for one ordered choice in a survey question.
 */
public record QuestionOptionRequest(
        @NotBlank(message = "Option text is required")
        String optionText
) {}
