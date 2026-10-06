package com.cs_42_3.surveyplatformbackend.survey.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Null;

import java.util.UUID;

/** One option supplied while creating a question. Its identity is always server-generated. */
public record CreateQuestionOptionRequest(
        @Null(message = "Option ID must be omitted when creating a question")
        @Schema(description = "Must be null on create; the server generates the option UUID", nullable = true)
        UUID optionId,
        @NotBlank(message = "Option text is required")
        String optionText
) {
    public CreateQuestionOptionRequest(String optionText) {
        this(null, optionText);
    }
}
