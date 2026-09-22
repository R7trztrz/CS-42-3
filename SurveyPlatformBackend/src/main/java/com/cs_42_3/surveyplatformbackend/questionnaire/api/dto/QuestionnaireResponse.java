package com.cs_42_3.surveyplatformbackend.questionnaire.api.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Response model for a questionnaire's full editable structure.
 * <p>
 * {@code id}, {@code createdAt}, and {@code updatedAt} are null when the study has no
 * saved questionnaire yet; the first {@code saveQuestionnaire} call creates it.
 */
public record QuestionnaireResponse(
        UUID id,
        UUID studyId,
        List<QuestionnaireItemResponse> items,
        Instant createdAt,
        Instant updatedAt
) {

    public QuestionnaireResponse {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
