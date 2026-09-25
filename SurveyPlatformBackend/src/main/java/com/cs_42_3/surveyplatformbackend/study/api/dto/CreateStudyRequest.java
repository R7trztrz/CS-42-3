package com.cs_42_3.surveyplatformbackend.study.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Client-editable fields for creating a study; ownership and status are server-controlled.
 *
 * @author Simon Tian
 */
@Schema(description = "Input for creating a draft study. Ownership and initial status are assigned by the server.")
public record CreateStudyRequest(
        @Schema(description = "Study title; must contain non-whitespace characters.",
                example = "Social media browsing study", maxLength = 255, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Title is required")
        @Size(max = 255, message = "Title must not exceed 255 characters")
        String title,

        @Schema(description = "Optional description of the study.",
                example = "Investigate browsing behaviour in a simulated social media feed.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        String description,

        @Schema(description = "Required system template code. Template content may be unconfigured.",
                example = "blank", allowableValues = {"blank", "facebook", "instagram", "tiktok",
                "x", "threads", "bluesky", "truth-social"}, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "Template code is required")
        @Size(max = 32, message = "Template code must not exceed 32 characters")
        String templateCode
) {
}
