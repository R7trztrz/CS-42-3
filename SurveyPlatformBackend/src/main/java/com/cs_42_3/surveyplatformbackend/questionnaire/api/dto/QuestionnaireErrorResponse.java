package com.cs_42_3.surveyplatformbackend.questionnaire.api.dto;

import java.time.Instant;

/**
 * Error response returned by questionnaire endpoints.
 */
public record QuestionnaireErrorResponse(
        String code,
        String message,
        Instant timestamp,
        String path
) {}
