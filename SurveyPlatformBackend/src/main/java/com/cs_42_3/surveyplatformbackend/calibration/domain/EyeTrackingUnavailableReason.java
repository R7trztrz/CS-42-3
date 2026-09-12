package com.cs_42_3.surveyplatformbackend.calibration.domain;

/**
 * Why eye tracking could not run for a session. Recorded so that FR-51 can report
 * availability and M7 can let researchers filter sessions by data quality.
 *
 * @author Shuo Gu
 */
public enum EyeTrackingUnavailableReason {

    /** The browser does not expose a camera capture API. */
    NO_CAMERA_API,

    /** The browser exposes the API but reports no camera device. */
    NO_CAMERA_DEVICE,

    /** The participant declined the camera permission prompt. */
    PERMISSION_DENIED,

    /** The camera was available but the gaze estimator failed to start. */
    INIT_FAILED
}
