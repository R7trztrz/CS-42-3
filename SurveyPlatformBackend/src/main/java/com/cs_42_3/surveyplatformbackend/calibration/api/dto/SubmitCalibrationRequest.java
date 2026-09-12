package com.cs_42_3.surveyplatformbackend.calibration.api.dto;

import com.cs_42_3.surveyplatformbackend.calibration.domain.CalibrationOutcome;
import com.cs_42_3.surveyplatformbackend.calibration.domain.EyeTrackingUnavailableReason;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

import java.time.Instant;
import java.util.List;

/**
 * A finished calibration attempt submitted by the participant client. The session is taken from
 * the participant token, so this request cannot assign one.
 *
 * @author Shuo Gu
 */
@Schema(description = "A finished calibration attempt. The session is derived from the participant token.")
public record SubmitCalibrationRequest(

        @Schema(description = "Ordinal of this attempt within the session, starting at one. Resubmitting the same number returns the stored attempt unchanged.",
                example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
        @Positive
        int attemptNumber,

        @Schema(description = "How the attempt ended.", example = "COMPLETED",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        CalibrationOutcome outcome,

        @Schema(description = "Why the camera was unusable. Required when the outcome is UNAVAILABLE and rejected otherwise.",
                example = "PERMISSION_DENIED", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        EyeTrackingUnavailableReason unavailableReason,

        @Schema(description = "Median residual over calibration targets, in CSS pixels. Required when the outcome is COMPLETED.",
                example = "82.4", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @PositiveOrZero
        Double residualMedianPx,

        @Schema(description = "Median residual over validation targets, in CSS pixels. Omit when validation targets are not presented.",
                example = "119.7", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        @PositiveOrZero
        Double validationMedianPx,

        @Schema(description = "Viewport width in CSS pixels at calibration time.", example = "2268",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @Positive
        int viewportWidth,

        @Schema(description = "Viewport height in CSS pixels at calibration time.", example = "1293",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @Positive
        int viewportHeight,

        @Schema(description = "Device pixel ratio at calibration time.", example = "2",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @Positive
        double devicePixelRatio,

        @Schema(description = "When the attempt began.", format = "date-time",
                example = "2026-09-12T04:11:02.310Z", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Instant startedAt,

        @Schema(description = "When the attempt ended; must not precede startedAt.", format = "date-time",
                example = "2026-09-12T04:12:18.774Z", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        Instant finishedAt,

        @Schema(description = "Per-target residuals. Must be empty when the outcome is UNAVAILABLE.",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        List<@Valid @NotNull PointResidualRequest> pointResiduals
) {
}
