package com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Complete ordered questionnaire draft returned to a researcher. */
public record QuestionnaireResponse(
        UUID questionnaireId,
        @Schema(description = "Publication snapshot UUID; null for a live draft", nullable = true)
        UUID snapshotId,
        UUID studyId,
        QuestionnaireContentSource contentSource,
        @Schema(description = "Items in display order, including deleted-question placeholders")
        List<QuestionnaireItemResponse> items,
        @Schema(description = "Optimistic content version to submit as expectedVersion on the next change")
        Long version,
        Instant updatedAt,
        @Schema(description = "Publication time; null for a live draft", nullable = true)
        Instant publishedAt,
        boolean valid,
        List<QuestionnaireValidationIssue> validationIssues
) {
    public QuestionnaireResponse {
        contentSource = contentSource == null
                ? QuestionnaireContentSource.LIVE_DRAFT
                : contentSource;
        items = items == null ? List.of() : List.copyOf(items);
        validationIssues = validationIssues == null ? List.of() : List.copyOf(validationIssues);
    }

    public QuestionnaireResponse(
            UUID questionnaireId,
            UUID studyId,
            List<QuestionnaireItemResponse> items,
            Long version,
            Instant updatedAt
    ) {
        this(
                questionnaireId,
                null,
                studyId,
                QuestionnaireContentSource.LIVE_DRAFT,
                items,
                version,
                updatedAt,
                null,
                true,
                List.of()
        );
    }

    public QuestionnaireResponse(
            UUID questionnaireId,
            UUID studyId,
            List<QuestionnaireItemResponse> items,
            Long version,
            Instant updatedAt,
            boolean valid,
            List<QuestionnaireValidationIssue> validationIssues
    ) {
        this(
                questionnaireId,
                null,
                studyId,
                QuestionnaireContentSource.LIVE_DRAFT,
                items,
                version,
                updatedAt,
                null,
                valid,
                validationIssues
        );
    }
}
