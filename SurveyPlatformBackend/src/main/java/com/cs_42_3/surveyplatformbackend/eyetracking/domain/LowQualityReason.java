package com.cs_42_3.surveyplatformbackend.eyetracking.domain;

/** Markers carried with a calibration attempt and with every gaze event recorded under it. */
public enum LowQualityReason {
    ABANDONED,
    NOT_MEASURED,
    INSUFFICIENT_SAMPLES,
    HIGH_RESIDUAL,
    VIEWPORT_CHANGED,
    ENGINE_UNAVAILABLE
}
