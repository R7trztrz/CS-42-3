package com.cs_42_3.surveyplatformbackend.eyetracking.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Upper bounds for participant-supplied eye-tracking data.
 *
 * @param maxGazeEventsPerBatch largest accepted batch; the browser cuts 200 by default
 * @param maxGazePayloadChars largest accepted serialized event array, checked before parsing
 * @param maxCalibrationAttemptsPerSession attempts accepted per session through the record
 *        endpoint; the browser allows at most 10 per page load, and a reload starts again
 */
@Validated
@ConfigurationProperties(prefix = "app.eye-tracking")
public record EyeTrackingProperties(
        @Min(1) @Max(100_000) int maxGazeEventsPerBatch,
        @Min(1_000) @Max(50_000_000) int maxGazePayloadChars,
        @Min(1) @Max(1_000) int maxCalibrationAttemptsPerSession
) {
}
