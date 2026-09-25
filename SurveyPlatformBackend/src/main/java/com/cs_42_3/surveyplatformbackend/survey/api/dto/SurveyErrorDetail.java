package com.cs_42_3.surveyplatformbackend.survey.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Stable machine-readable location and reason for one request error. */
public record SurveyErrorDetail(
        @Schema(example = "items[2].questionId")
        String field,
        @Schema(description = "Zero-based item index when the error belongs to an array element", nullable = true)
        Integer index,
        @Schema(description = "Submitted item UUID when available", nullable = true)
        UUID itemId,
        @Schema(example = "INVALID_QUESTION_REFERENCE")
        String code,
        String message
) {}
