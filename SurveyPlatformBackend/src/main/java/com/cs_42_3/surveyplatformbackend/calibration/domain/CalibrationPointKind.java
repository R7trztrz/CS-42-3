package com.cs_42_3.surveyplatformbackend.calibration.domain;

/**
 * Whether a residual was measured on a target the model was trained on.
 *
 * @author Shuo Gu
 */
public enum CalibrationPointKind {

    /** Target the participant clicked; its samples trained the model, so its residual is in-sample. */
    CALIBRATION,

    /** Target withheld from training; its residual is out-of-sample. */
    VALIDATION
}
