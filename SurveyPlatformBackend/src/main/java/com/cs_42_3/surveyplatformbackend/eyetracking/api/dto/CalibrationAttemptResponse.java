package com.cs_42_3.surveyplatformbackend.eyetracking.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Acknowledges a stored calibration attempt.
 *
 * @param attemptId the browser-assigned attempt UUID
 * @param attemptNumber the session-local ordinal the browser reported
 * @param receivedAt when the server first accepted this attempt; a replay returns the original
 */
public record CalibrationAttemptResponse(UUID attemptId, int attemptNumber, Instant receivedAt) {
}
