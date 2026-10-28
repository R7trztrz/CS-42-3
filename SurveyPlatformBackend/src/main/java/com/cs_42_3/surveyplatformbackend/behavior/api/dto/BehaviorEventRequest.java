package com.cs_42_3.surveyplatformbackend.behavior.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Flat browser event compatible with the existing collector, plus explicit page and shared-clock context.
 *
 * @author Simon Tian
 */
public record BehaviorEventRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED,
                allowableValues = {"click", "keydown", "mousemove", "scroll", "visibilitychange"}) String type,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Epoch milliseconds from the shared session clock.") Long timestamp,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Shared with gaze; finite negative values are allowed.") Double sessionElapsedMs,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, maxLength = 128) String pageId,
        @Schema(description = "Viewport CSS pixels, required only for click/mousemove.") Double x,
        @Schema(description = "Viewport CSS pixels, required only for click/mousemove.") Double y,
        @Schema(description = "KeyboardEvent.code, required only for keydown; never an input value.") String code,
        @Schema(description = "Required only for scroll.") Double scrollX,
        @Schema(description = "Required only for scroll.") Double scrollY,
        @Schema(description = "Document hidden flag, required only for visibilitychange.") Boolean hidden,
        @Schema(maxLength = 255, nullable = true) String componentId,
        @Schema(maxLength = 128, nullable = true) String componentType) { }
