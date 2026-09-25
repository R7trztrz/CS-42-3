package com.cs_42_3.surveyplatformbackend.survey.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

/**
 * Error response returned by survey question endpoints.
 */
public record SurveyErrorResponse(
        String code,
        String message,
        Instant timestamp,
        String path,
        @Schema(description = "Structured item-level locations; empty when the error is not item-specific")
        List<SurveyErrorDetail> details
) {
    public SurveyErrorResponse {
        details = details == null ? List.of() : List.copyOf(details);
    }
}
