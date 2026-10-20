package com.cs_42_3.surveyplatformbackend.eyetracking.api.dto;

import java.time.Instant;

/**
 * Acknowledges a stored gaze batch.
 *
 * @param sequence the batch sequence within the session
 * @param batchId the browser-assigned batch identifier
 * @param eventCount events stored for this batch
 * @param receivedAt when the server first accepted this batch; a replay returns the original
 */
public record GazeBatchResponse(long sequence, String batchId, int eventCount, Instant receivedAt) {
}
