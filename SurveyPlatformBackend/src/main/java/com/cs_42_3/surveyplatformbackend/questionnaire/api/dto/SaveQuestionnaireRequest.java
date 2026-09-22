package com.cs_42_3.surveyplatformbackend.questionnaire.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request payload that replaces a questionnaire's enabled items, order, and branch
 * rules as one unit (FR-36, FR-37, FR-38, FR-39).
 * <p>
 * List order is the questionnaire's display order and each item's default jump target;
 * branch rules reference their jump target by index into this same list.
 */
public record SaveQuestionnaireRequest(
        @NotNull(message = "Item list is required")
        List<@Valid QuestionnaireItemRequest> items
) {}
