package com.cs_42_3.surveyplatformbackend.eyetracking.api.dto;

import com.cs_42_3.surveyplatformbackend.eyetracking.domain.CalibrationOutcome;
import com.cs_42_3.surveyplatformbackend.eyetracking.domain.CalibrationPointKind;
import com.cs_42_3.surveyplatformbackend.eyetracking.domain.CalibrationQuality;
import com.cs_42_3.surveyplatformbackend.eyetracking.domain.CalibrationQualityBasis;
import com.cs_42_3.surveyplatformbackend.eyetracking.domain.EyeTrackingUnavailableReason;
import com.cs_42_3.surveyplatformbackend.eyetracking.domain.LowQualityReason;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * One finished FR-49 calibration attempt, exactly as the browser's {@code CalibrationAttempt}
 * record serializes it (schema version 1).
 *
 * <p>Bean Validation covers the shape of each field. Cross-field rules, finite numbers and
 * timestamps are checked by {@code CalibrationRecordService} before anything is locked.
 */
public record CalibrationAttemptRequest(
        @NotNull(message = "schemaVersion is required")
        @Min(value = 1, message = "schemaVersion must be 1")
        @Max(value = 1, message = "schemaVersion must be 1")
        Integer schemaVersion,

        @NotNull(message = "algorithmVersion is required")
        @Pattern(regexp = "[A-Za-z0-9._-]{1,32}", message = "algorithmVersion is invalid")
        String algorithmVersion,

        @NotNull(message = "sessionId is required")
        UUID sessionId,

        @NotNull(message = "attemptId is required")
        UUID attemptId,

        @NotNull(message = "attemptNumber is required")
        @Min(value = 1, message = "attemptNumber must be between 1 and 10")
        @Max(value = 10, message = "attemptNumber must be between 1 and 10")
        Integer attemptNumber,

        @NotNull(message = "outcome is required")
        CalibrationOutcome outcome,

        @NotNull(message = "trackingAvailable is required")
        Boolean trackingAvailable,

        EyeTrackingUnavailableReason unavailableReason,

        @NotNull(message = "quality is required")
        CalibrationQuality quality,

        @NotNull(message = "qualityBasis is required")
        CalibrationQualityBasis qualityBasis,

        @NotNull(message = "lowQualityReasons is required")
        @Size(max = 6, message = "lowQualityReasons has too many entries")
        List<@NotNull(message = "lowQualityReasons must not contain null") LowQualityReason> lowQualityReasons,

        @NotNull(message = "qualityThresholds is required")
        @Valid
        QualityThresholds qualityThresholds,

        @NotNull(message = "minSamplesPerTarget is required")
        @Min(value = 1, message = "minSamplesPerTarget must be between 1 and 5")
        @Max(value = 5, message = "minSamplesPerTarget must be between 1 and 5")
        Integer minSamplesPerTarget,

        @NotNull(message = "calibrationTargetsMeasured is required")
        @Min(value = 0, message = "calibrationTargetsMeasured must be between 0 and 9")
        @Max(value = 9, message = "calibrationTargetsMeasured must be between 0 and 9")
        Integer calibrationTargetsMeasured,

        @NotNull(message = "calibrationTargetsRequired is required")
        @Min(value = 0, message = "calibrationTargetsRequired must be between 0 and 9")
        @Max(value = 9, message = "calibrationTargetsRequired must be between 0 and 9")
        Integer calibrationTargetsRequired,

        @NotNull(message = "validationTargetsMeasured is required")
        @Min(value = 0, message = "validationTargetsMeasured must be between 0 and 9")
        @Max(value = 9, message = "validationTargetsMeasured must be between 0 and 9")
        Integer validationTargetsMeasured,

        @NotNull(message = "validationTargetsRequired is required")
        @Min(value = 0, message = "validationTargetsRequired must be between 0 and 9")
        @Max(value = 9, message = "validationTargetsRequired must be between 0 and 9")
        Integer validationTargetsRequired,

        @PositiveOrZero(message = "residualMedianPx must not be negative")
        Double residualMedianPx,

        @PositiveOrZero(message = "validationMedianPx must not be negative")
        Double validationMedianPx,

        @NotNull(message = "viewportWidth is required")
        @Positive(message = "viewportWidth must be positive")
        Double viewportWidth,

        @NotNull(message = "viewportHeight is required")
        @Positive(message = "viewportHeight must be positive")
        Double viewportHeight,

        @NotNull(message = "devicePixelRatio is required")
        @Positive(message = "devicePixelRatio must be positive")
        Double devicePixelRatio,

        @NotNull(message = "startedAt is required")
        @Size(max = 64, message = "startedAt is invalid")
        String startedAt,

        @NotNull(message = "finishedAt is required")
        @Size(max = 64, message = "finishedAt is invalid")
        String finishedAt,

        @NotNull(message = "pointResiduals is required")
        @Size(max = 18, message = "pointResiduals has too many entries")
        List<@NotNull(message = "pointResiduals must not contain null") @Valid PointResidual> pointResiduals
) {

    /** The grading policy that was in force when the browser graded this attempt. */
    public record QualityThresholds(
            @NotNull(message = "qualityThresholds.goodFraction is required")
            Double goodFraction,

            @NotNull(message = "qualityThresholds.fairFraction is required")
            Double fairFraction
    ) {
    }

    /** One measured target: the aggregate predicted centre and its residual in CSS pixels. */
    public record PointResidual(
            @NotNull(message = "pointResiduals.pointKind is required")
            CalibrationPointKind pointKind,

            @NotNull(message = "pointResiduals.pointIndex is required")
            @Min(value = 0, message = "pointResiduals.pointIndex must be between 0 and 8")
            @Max(value = 8, message = "pointResiduals.pointIndex must be between 0 and 8")
            Integer pointIndex,

            @NotNull(message = "pointResiduals.targetX is required")
            Double targetX,

            @NotNull(message = "pointResiduals.targetY is required")
            Double targetY,

            @NotNull(message = "pointResiduals.predictedX is required")
            Double predictedX,

            @NotNull(message = "pointResiduals.predictedY is required")
            Double predictedY,

            @NotNull(message = "pointResiduals.residualPx is required")
            @PositiveOrZero(message = "pointResiduals.residualPx must not be negative")
            Double residualPx,

            @NotNull(message = "pointResiduals.sampleCount is required")
            @Min(value = 1, message = "pointResiduals.sampleCount must be between 1 and 1000")
            @Max(value = 1000, message = "pointResiduals.sampleCount must be between 1 and 1000")
            Integer sampleCount
    ) {
    }
}
