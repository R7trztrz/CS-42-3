package com.cs_42_3.surveyplatformbackend.behavior.api.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Stable acknowledgement returned unchanged for an identical accepted replay.
 *
 * @author Simon Tian
 */
public record BehaviorBatchResponse(UUID sessionId, UUID batchId, UUID streamId, long sequence,
                                    int eventCount, Instant receivedAt) { }
