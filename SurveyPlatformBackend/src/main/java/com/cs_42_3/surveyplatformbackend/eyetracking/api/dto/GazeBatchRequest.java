package com.cs_42_3.surveyplatformbackend.eyetracking.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

import java.util.UUID;

/**
 * One FR-50 gaze batch exactly as the browser buffer cuts it (schema version 1).
 *
 * <p>{@code payload} is the JSON array of gaze events the buffer already serialized. It is
 * parsed and validated event by event by {@code GazeBatchService}.
 */
public record GazeBatchRequest(
        @NotNull(message = "schemaVersion is required")
        @Min(value = 1, message = "schemaVersion must be 1")
        @Max(value = 1, message = "schemaVersion must be 1")
        Integer schemaVersion,

        @NotNull(message = "sessionId is required")
        UUID sessionId,

        @NotNull(message = "batchId is required")
        @Pattern(regexp = "[\\x21-\\x7E]{1,128}", message = "batchId is invalid")
        String batchId,

        @NotNull(message = "sequence is required")
        @Min(value = 1, message = "sequence must be a positive safe integer")
        @Max(value = 9007199254740991L, message = "sequence must be a positive safe integer")
        Long sequence,

        @NotNull(message = "eventCount is required")
        @Min(value = 1, message = "eventCount must be positive")
        Integer eventCount,

        @NotNull(message = "droppedEvents is required")
        @Min(value = 0, message = "droppedEvents must not be negative")
        Long droppedEvents,

        @NotNull(message = "droppedEventsTotal is required")
        @Min(value = 0, message = "droppedEventsTotal must not be negative")
        Long droppedEventsTotal,

        @NotNull(message = "payload is required")
        String payload
) {
}
