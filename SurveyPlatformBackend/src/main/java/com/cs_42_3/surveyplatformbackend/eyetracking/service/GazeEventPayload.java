package com.cs_42_3.surveyplatformbackend.eyetracking.service;

import java.util.List;

/**
 * One element of a gaze batch payload as the browser serializes a {@code GazeEvent}.
 *
 * <p>Every field is read loosely (boxed numbers, strings for enums and identifiers) so that a
 * missing or malformed value is reported with its position instead of failing the whole parse.
 */
public record GazeEventPayload(
        Integer schemaVersion,
        String sessionId,
        String pageId,
        Double x,
        Double y,
        Double scrollX,
        Double scrollY,
        Double pageX,
        Double pageY,
        Double viewportWidth,
        Double viewportHeight,
        Double devicePixelRatio,
        String widgetId,
        String attribution,
        String timestamp,
        Double sessionElapsedMs,
        String calibrationAttemptId,
        String calibrationQuality,
        List<String> calibrationLowQualityReasons
) {
}
