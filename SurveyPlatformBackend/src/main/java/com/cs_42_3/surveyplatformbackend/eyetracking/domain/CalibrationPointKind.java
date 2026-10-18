package com.cs_42_3.surveyplatformbackend.eyetracking.domain;

/** Training targets train the model; validation targets only measure it. */
public enum CalibrationPointKind {
    CALIBRATION,
    VALIDATION
}
