package com.cs_42_3.surveyplatformbackend.survey.api.dto;

import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;

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
        List<@Valid UpdateQuestionOptionRequest> options,
        Integer scaleMin,
        Integer scaleMax,
        String scaleMinLabel,
        String scaleMaxLabel,
        @Schema(
                description = "Explicitly permits replacing every existing option identity when no existing optionId is retained",
                defaultValue = "false"
        )
        Boolean replaceAllOptions
) {
    public UpdateQuestionRequest {
        replaceAllOptions = Boolean.TRUE.equals(replaceAllOptions);
    }
}
