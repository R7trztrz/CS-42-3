package com.cs_42_3.surveyplatformbackend.participation.api.dto;

import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantAbandonmentReason;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSessionPhase;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSessionStatus;

import java.time.Instant;
import java.util.UUID;

public record ParticipantSessionResponse(
        UUID sessionId,
        ParticipantSessionStatus status,
        ParticipantSessionPhase phase,
        ParticipantAbandonmentReason abandonmentReason,
        UUID currentQuestionItemId,
        boolean questionnaireReadyToSubmit,
        boolean studyAvailable,
        String studyTitle,
        String studyDescription,
        boolean eyeTrackingEnabled,
        boolean questionnaireEnabled,
        ConsentDocumentResponse consentDocument,
        Instant enteredAt,
        Instant consentedAt,
        Instant calibrationCompletedAt,
        Instant browsingCompletedAt,
        Instant completedAt,
        Instant abandonedAt,
        Instant lastActivityAt
) {
}
