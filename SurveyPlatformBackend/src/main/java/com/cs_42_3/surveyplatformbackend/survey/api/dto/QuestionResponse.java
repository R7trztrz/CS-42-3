package com.cs_42_3.surveyplatformbackend.survey.api.dto;

import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Detailed response model for a reusable survey question.
 */
public record QuestionResponse(
        UUID id,
        QuestionType type,
        String questionText,
        boolean required,
        List<QuestionOptionResponse> options,
        Integer scaleMin,
        Integer scaleMax,
        String scaleMinLabel,
        String scaleMaxLabel,
        Instant createdAt,
        Instant updatedAt
) {

    public QuestionResponse {
        options = options == null ? List.of() : List.copyOf(options);
    }
}
