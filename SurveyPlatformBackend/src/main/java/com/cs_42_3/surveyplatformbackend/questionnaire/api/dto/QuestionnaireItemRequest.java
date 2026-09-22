package com.cs_42_3.surveyplatformbackend.questionnaire.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

/**
 * Request payload for one enabled question-bank reference within a questionnaire
 * (FR-36). Its position within the enclosing {@link SaveQuestionnaireRequest#items()}
 * list is its display order (FR-37).
 */
public record QuestionnaireItemRequest(
        @NotNull(message = "Question ID is required")
        UUID questionId,
        List<@Valid QuestionnaireBranchRuleRequest> branchRules
) {}
