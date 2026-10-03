package com.cs_42_3.surveyplatformbackend.participation.service;

import java.util.UUID;

/** M5 boundary joined by M6's calibration-result transaction. */
public interface ParticipantCalibrationLifecyclePort {
    void completeCalibration(UUID sessionId);
}
