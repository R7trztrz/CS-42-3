package com.cs_42_3.surveyplatformbackend.eyetracking.service;

import com.cs_42_3.surveyplatformbackend.eyetracking.exception.EyeTrackingRequestException;

import java.time.Instant;
import java.time.format.DateTimeParseException;

/**
 * Parses browser timestamps ({@code Date.prototype.toISOString()} output).
 *
 * <p>Participant clocks may be wrong, so no tight window is enforced; the bounds only keep
 * values that the browser could not have produced for a real session out of the database.
 */
final class EyeTrackingTimestamps {
    private static final Instant EARLIEST = Instant.parse("2000-01-01T00:00:00Z");
    private static final Instant LATEST = Instant.parse("2100-01-01T00:00:00Z");

    private EyeTrackingTimestamps() {
    }

    static Instant parse(String value, String field) {
        Instant instant;
        try {
            instant = Instant.parse(value);
        } catch (DateTimeParseException | NullPointerException exception) {
            throw EyeTrackingRequestException.invalid(field + " must be an ISO-8601 UTC timestamp");
        }
        if (instant.isBefore(EARLIEST) || !instant.isBefore(LATEST)) {
            throw EyeTrackingRequestException.invalid(field + " is outside the accepted range");
        }
        return instant;
    }
}
