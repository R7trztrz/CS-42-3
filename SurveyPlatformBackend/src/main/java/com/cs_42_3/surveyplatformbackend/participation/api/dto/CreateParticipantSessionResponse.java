package com.cs_42_3.surveyplatformbackend.participation.api.dto;

import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSessionPhase;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSessionStatus;

import java.time.Instant;
import java.util.UUID;

public record CreateParticipantSessionResponse(
        UUID sessionId,
        String sessionToken,
        ParticipantSessionStatus status,
        ParticipantSessionPhase phase,
        String studyTitle,
        String studyDescription,
        boolean eyeTrackingEnabled,
        boolean questionnaireEnabled,
        ConsentDocumentResponse consentDocument,
        Instant enteredAt
) {
}
