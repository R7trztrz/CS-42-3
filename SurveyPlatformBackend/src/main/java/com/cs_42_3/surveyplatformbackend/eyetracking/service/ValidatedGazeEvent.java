package com.cs_42_3.surveyplatformbackend.eyetracking.service;

import com.cs_42_3.surveyplatformbackend.eyetracking.domain.CalibrationQuality;
import com.cs_42_3.surveyplatformbackend.eyetracking.domain.GazeAttribution;
import com.cs_42_3.surveyplatformbackend.eyetracking.domain.LowQualityReason;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * One gaze event that passed validation, with its position inside the batch. pageX/pageY are
 * absent on purpose: they were checked to equal x + scrollX and y + scrollY and are not stored.
 */
public record ValidatedGazeEvent(
        int index,
        String pageId,
        double x,
        double y,
        double scrollX,
        double scrollY,
        double viewportWidth,
        double viewportHeight,
        double devicePixelRatio,
        String widgetId,
        GazeAttribution attribution,
        Instant occurredAt,
        double sessionElapsedMs,
        UUID calibrationAttemptId,
        CalibrationQuality calibrationQuality,
        List<LowQualityReason> calibrationLowQualityReasons
) {
}
