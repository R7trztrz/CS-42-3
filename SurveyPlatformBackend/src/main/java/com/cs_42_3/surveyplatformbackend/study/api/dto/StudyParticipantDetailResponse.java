package com.cs_42_3.surveyplatformbackend.study.api.dto;

import com.cs_42_3.surveyplatformbackend.eyetracking.api.dto.EyeTrackingSessionSummaryResponse;
import com.cs_42_3.surveyplatformbackend.participation.domain.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * Returns the FR-20 operational session details, without questionnaire answers or raw events.
 *
 * @author Simon Tian
 */
public record StudyParticipantDetailResponse(
        UUID studyId,
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
        @Schema(description = "Allow-listed device metadata; null if not supplied or cleared after consent refusal.", nullable = true)
        StudyParticipantDeviceResponse deviceInfo,
        EyeTrackingSessionSummaryResponse eyeTracking
) {}
