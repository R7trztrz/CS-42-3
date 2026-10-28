package com.cs_42_3.surveyplatformbackend.behavior.domain;

import java.util.*;

/**
 * Immutable write plan and semantic digest prepared before taking collection locks.
 *
 * @author Simon Tian
 */
public record ValidatedBehaviorBatch(UUID batchId, UUID streamId, long sequence,
        List<ValidatedBehaviorEvent> events, String requestHash) {
    public ValidatedBehaviorBatch { events = List.copyOf(events); }
}
