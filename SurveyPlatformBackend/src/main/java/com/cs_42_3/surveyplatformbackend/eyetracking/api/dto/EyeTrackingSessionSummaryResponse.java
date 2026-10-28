package com.cs_42_3.surveyplatformbackend.eyetracking.api.dto;

import com.cs_42_3.surveyplatformbackend.eyetracking.domain.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import java.util.UUID;

/**
 * Exposes only final calibration metadata; raw coordinates and per-target measurements stay private.
 *
 * @author Simon Tian
 */
public record EyeTrackingSessionSummaryResponse(
        boolean enabled,
        EyeTrackingCalibrationState calibrationState,
        long calibrationAttemptCount,
        @Schema(description = "Attempt selected at calibration completion; null until one is recorded.", nullable = true)
        UUID completedAttemptId,
        @Schema(description = "Recorded tracker availability, not proof of saved gaze events; null without a final attempt.", nullable = true)
        Boolean trackingAvailable,
        EyeTrackingUnavailableReason unavailableReason,
        CalibrationQuality quality,
        CalibrationQualityBasis qualityBasis,
        List<LowQualityReason> lowQualityReasons
) {
    public EyeTrackingSessionSummaryResponse {
        lowQualityReasons = List.copyOf(lowQualityReasons);
    }
}
