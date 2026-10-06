package com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.UUID;

/** One requested questionnaire item; a null item ID means add a new item. */
public record SaveQuestionnaireItemRequest(
        @Schema(
                description = "Stable questionnaire-item UUID. Null adds a new item; retain it to reorder or replace an existing item.",
                nullable = true
        )
        UUID itemId,
        @Schema(description = "Owned question-bank UUID. Null question references cannot be saved.")
        @NotNull UUID questionId,
        @Schema(description = "Complete branch-rule set for this source item")
        List<@NotNull @Valid QuestionnaireBranchRuleRequest> branchRules
) {
    public SaveQuestionnaireItemRequest {
        branchRules = branchRules == null
                ? List.of()
                : Collections.unmodifiableList(new ArrayList<>(branchRules));
    }

    public SaveQuestionnaireItemRequest(UUID itemId, UUID questionId) {
        this(itemId, questionId, List.of());
    }
}
