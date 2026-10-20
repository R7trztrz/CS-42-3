package com.cs_42_3.surveyplatformbackend.eyetracking.domain;

/** Why the browser eye tracker could not be used; values match the FR-49 frontend contract. */
public enum EyeTrackingUnavailableReason {
    NO_CAMERA_API,
    NO_CAMERA_DEVICE,
    PERMISSION_DENIED,
    INIT_FAILED,
    INIT_CANCELLED,
    INIT_TIMEOUT
}
