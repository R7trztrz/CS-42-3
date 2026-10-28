package com.cs_42_3.surveyplatformbackend.behavior.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.*;

/**
 * Immutable retry envelope; session identity is obtained exclusively from participant authentication.
 *
 * @author Simon Tian
 */
public record BehaviorBatchRequest(
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = {"1"}) Integer schemaVersion,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Stable UUID reused for every retry of this batch.") UUID batchId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "New UUID for each page collection instance.") UUID streamId,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, minimum = "1", maximum = "9007199254740991") Long sequence,
        @Schema(requiredMode = Schema.RequiredMode.REQUIRED, description = "Nonempty array; default maximum 1000 events.") List<BehaviorEventRequest> events) {
    public BehaviorBatchRequest {
        events = events == null ? null : List.copyOf(events);
    }
}
