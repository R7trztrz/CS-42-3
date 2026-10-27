package com.cs_42_3.surveyplatformbackend.eyetracking.domain;

/**
 * Describes recorded calibration progress, not whether gaze events were successfully delivered.
 * RECORDED includes completed, abandoned and unavailable final attempts.
 *
 * @author Simon Tian
 */
public enum EyeTrackingCalibrationState {
    DISABLED,
    NOT_STARTED,
    IN_PROGRESS,
    RECORDED
}
