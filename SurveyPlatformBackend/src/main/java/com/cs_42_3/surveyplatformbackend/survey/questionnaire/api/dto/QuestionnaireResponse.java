package com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Complete ordered questionnaire draft returned to a researcher. */
public record QuestionnaireResponse(
        UUID id,
        UUID studyId,
        @Schema(description = "Items in display order, including deleted-question placeholders")
        List<QuestionnaireItemResponse> items,
        @Schema(description = "Optimistic content version to submit as expectedVersion on the next change")
        Long version,
        Instant updatedAt,
        boolean valid,
        List<QuestionnaireValidationIssue> validationIssues
) {
    public QuestionnaireResponse {
        items = items == null ? List.of() : List.copyOf(items);
        validationIssues = validationIssues == null ? List.of() : List.copyOf(validationIssues);
    }

    public QuestionnaireResponse(
            UUID id,
            UUID studyId,
            List<QuestionnaireItemResponse> items,
            Long version,
            Instant updatedAt
    ) {
        this(id, studyId, items, version, updatedAt, true, List.of());
    }
}
