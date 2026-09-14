package com.cs_42_3.surveyplatformbackend.survey.api.dto;

import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * Request payload for replacing the editable fields of a survey question.
 */
public record UpdateQuestionRequest(
        @NotNull(message = "Question type is required")
        QuestionType type,
        @NotBlank(message = "Question text is required")
        String questionText,
        boolean required,
        List<@Valid QuestionOptionRequest> options,
        Integer scaleMin,
        Integer scaleMax,
        String scaleMinLabel,
        String scaleMaxLabel
) {}
