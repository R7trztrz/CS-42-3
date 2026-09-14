package com.cs_42_3.surveyplatformbackend.survey.api.dto;

import java.time.Instant;

/**
 * Error response returned by survey question endpoints.
 */
public record SurveyErrorResponse(
        String code,
        String message,
        Instant timestamp,
        String path
) {}
