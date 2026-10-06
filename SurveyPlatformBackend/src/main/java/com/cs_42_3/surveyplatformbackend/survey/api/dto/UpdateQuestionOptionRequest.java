package com.cs_42_3.surveyplatformbackend.survey.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

/** One desired option while updating a question. Null identity creates a new option. */
public record UpdateQuestionOptionRequest(
        @Schema(
                description = "Existing option UUID to retain; null creates a new option",
                nullable = true
        )
        UUID optionId,
        @NotBlank(message = "Option text is required")
        String optionText
) {}
