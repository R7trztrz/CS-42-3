package com.cs_42_3.surveyplatformbackend.calibration.api.dto;

import com.cs_42_3.surveyplatformbackend.calibration.domain.CalibrationPointKind;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

/**
 * One target's residual as measured by the participant client.
 *
 * @author Shuo Gu
 */
@Schema(description = "Residual between one fixation target and the position predicted for it.")
public record PointResidualRequest(

        @Schema(description = "Whether the target trained the model. VALIDATION targets are withheld from training.",
                example = "CALIBRATION", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull
        CalibrationPointKind pointKind,

        @Schema(description = "Ordinal of the target within its kind, starting at zero.", example = "0",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @PositiveOrZero
        int pointIndex,

        @Schema(description = "Target x coordinate in CSS pixels, viewport origin.", example = "160")
        double targetX,

        @Schema(description = "Target y coordinate in CSS pixels, viewport origin.", example = "120")
        double targetY,

        @Schema(description = "Trimmed-mean predicted x coordinate in CSS pixels.", example = "205")
        double predictedX,

        @Schema(description = "Trimmed-mean predicted y coordinate in CSS pixels.", example = "168")
        double predictedY,

        @Schema(description = "Euclidean distance between target and prediction, in CSS pixels.",
                example = "65.8", requiredMode = Schema.RequiredMode.REQUIRED)
        @PositiveOrZero
        double residualPx,

        @Schema(description = "Number of gaze samples the prediction was averaged over.", example = "47",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @Positive
        int sampleCount
) {
}
