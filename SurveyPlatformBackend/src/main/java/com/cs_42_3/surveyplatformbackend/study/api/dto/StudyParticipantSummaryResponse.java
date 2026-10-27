package com.cs_42_3.surveyplatformbackend.study.api.dto;

import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSessionStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * Projects only the anonymous session fields required by the FR-19 progress list.
 *
 * @author Simon Tian
 */
public record StudyParticipantSummaryResponse(
        @Schema(description = "Session identifier for the subsequent session-details request.") UUID sessionId,
        UUID anonymousParticipantId,
        ParticipantSessionStatus status,
        Instant enteredAt,
        @Schema(description = "Completion time; null unless the session completed.", nullable = true) Instant completedAt
) {}
