package com.cs_42_3.surveyplatformbackend.calibration.domain;

/**
 * Terminal state of a calibration attempt, persisted by name in the calibration_records table.
 *
 * @author Shuo Gu
 */
public enum CalibrationOutcome {

    /** Every calibration target was fixated and residuals were computed. */
    COMPLETED,

    /** Calibration started but the participant skipped or left before finishing. */
    ABANDONED,

    /** Calibration never started because the camera could not be used. */
    UNAVAILABLE
}
