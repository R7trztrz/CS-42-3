package com.cs_42_3.surveyplatformbackend.calibration.api.dto;

import com.cs_42_3.surveyplatformbackend.calibration.domain.CalibrationOutcome;
import com.cs_42_3.surveyplatformbackend.calibration.domain.CalibrationRecord;
import com.cs_42_3.surveyplatformbackend.calibration.domain.EyeTrackingUnavailableReason;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A stored calibration attempt as returned to the participant client. The session identifier is
 * withheld so that the anonymous client is never handed an identifier it did not already hold.
 *
 * @author Shuo Gu
 */
@Schema(description = "A stored calibration attempt.")
public record CalibrationRecordResponse(

        @Schema(description = "Server-generated attempt identifier. Attach this to gaze events so FR-50 can reference the attempt without duplicating its quality fields.",
                format = "uuid", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,

        @Schema(description = "Ordinal of this attempt within the session.", example = "1")
        int attemptNumber,

        @Schema(description = "How the attempt ended.", example = "COMPLETED")
        CalibrationOutcome outcome,

        @Schema(description = "Why the camera was unusable; null unless the outcome is UNAVAILABLE.")
        EyeTrackingUnavailableReason unavailableReason,

        @Schema(description = "Median residual over calibration targets, in CSS pixels.", example = "82.4")
        Double residualMedianPx,

        @Schema(description = "Median residual over validation targets, in CSS pixels.", example = "119.7")
        Double validationMedianPx,

        @Schema(description = "Number of per-target residuals stored with this attempt.", example = "13")
        int pointResidualCount,

        @Schema(description = "Ingest timestamp in UTC.", format = "date-time",
                example = "2026-09-12T04:12:19.002Z")
        Instant createdAt
) {

    /**
     * Maps a persisted attempt to its API representation.
     *
     * @param record the persisted attempt
     * @return the response representation
     */
    public static CalibrationRecordResponse from(CalibrationRecord record) {
        return new CalibrationRecordResponse(record.getId(), record.getAttemptNumber(),
                record.getOutcome(), record.getUnavailableReason(), record.getResidualMedianPx(),
                record.getValidationMedianPx(), record.getPointResiduals().size(),
                record.getCreatedAt());
    }

    /**
     * Maps several persisted attempts.
     *
     * @param records the persisted attempts
     * @return the response representations, in the given order
     */
    public static List<CalibrationRecordResponse> from(List<CalibrationRecord> records) {
        return records.stream().map(CalibrationRecordResponse::from).toList();
    }
}
