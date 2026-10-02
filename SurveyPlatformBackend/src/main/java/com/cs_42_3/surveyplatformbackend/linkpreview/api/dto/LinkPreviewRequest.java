package com.cs_42_3.surveyplatformbackend.linkpreview.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Supplies the webpage to preview.
 *
 * @author Simon Tian
 */
public record LinkPreviewRequest(
        @NotBlank @Size(max = 2048)
        @Schema(example = "https://example.com/article") String url) {}
