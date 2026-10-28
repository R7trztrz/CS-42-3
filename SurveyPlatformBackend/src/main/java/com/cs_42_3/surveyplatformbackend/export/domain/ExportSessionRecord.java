package com.cs_42_3.surveyplatformbackend.export.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Allow-listed session context shared by selected category writers in one read snapshot.
 *
 * @author Simon Tian
 */
public record ExportSessionRecord(UUID studyId, UUID sessionId, UUID anonymousId, String status,
        String phase, String reason, Instant entered, Instant consented, Instant calibrated, Instant browsed,
        Instant completed, Instant abandoned, Instant activity, boolean questionnaireEnabled, boolean eyeTrackingEnabled) {
}
