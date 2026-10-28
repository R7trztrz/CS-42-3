package com.cs_42_3.surveyplatformbackend.study.repository;

import com.cs_42_3.surveyplatformbackend.participation.domain.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Internal session projection that deliberately excludes token hashes and raw research records.
 * Device JSON must be mapped to an allow-listed response before leaving the service.
 *
 * @author Simon Tian
 */
public record StudyParticipantDetailRecord(
        UUID sessionId,
        UUID anonymousParticipantId,
        ParticipantSessionStatus status,
        ParticipantSessionPhase phase,
        ParticipantAbandonmentReason abandonmentReason,
        Instant enteredAt,
        Instant consentedAt,
        Instant calibrationCompletedAt,
        Instant browsingCompletedAt,
        Instant completedAt,
        Instant abandonedAt,
        Instant lastActivityAt,
        String deviceInfo
) {}
