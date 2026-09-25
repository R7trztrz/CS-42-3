package com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Complete desired questionnaire state supplied by a client. */
public record SaveQuestionnaireRequest(
        @Schema(
                description = "Version returned by the last GET. Use null only for initial creation or its exact replay.",
                example = "3",
                nullable = true
        )
        @PositiveOrZero Long expectedVersion,
        @Schema(
                description = "Complete final questionnaire order. An empty array saves an empty draft; null is invalid."
        )
        @NotNull List<@NotNull @Valid SaveQuestionnaireItemRequest> items
) {
    public SaveQuestionnaireRequest {
        items = items == null
                ? null
                : Collections.unmodifiableList(new ArrayList<>(items));
    }
}
