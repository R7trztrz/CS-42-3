package com.cs_42_3.surveyplatformbackend.participation.auth;

import java.util.UUID;

public record ParticipantSessionPrincipal(UUID sessionId, UUID studyId) {
}
