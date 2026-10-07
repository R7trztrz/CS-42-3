package com.cs_42_3.surveyplatformbackend.eyetracking.domain;

/** Graded by the browser from residuals; the server stores it and derives no score of its own. */
public enum CalibrationQuality {
    GOOD,
    FAIR,
    POOR,
    NOT_MEASURED
}
