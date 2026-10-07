package com.cs_42_3.surveyplatformbackend.eyetracking.service;

import com.cs_42_3.surveyplatformbackend.eyetracking.domain.CalibrationQuality;
import com.cs_42_3.surveyplatformbackend.eyetracking.domain.GazeAttribution;
import com.cs_42_3.surveyplatformbackend.eyetracking.domain.LowQualityReason;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** One gaze event that passed validation, with its position inside the batch. */
public record ValidatedGazeEvent(
        int index,
        String pageId,
        double x,
        double y,
        double scrollX,
        double scrollY,
        double pageX,
        double pageY,
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
