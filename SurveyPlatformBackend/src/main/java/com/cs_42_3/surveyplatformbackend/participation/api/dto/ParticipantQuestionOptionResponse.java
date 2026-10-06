package com.cs_42_3.surveyplatformbackend.participation.api.dto;

import java.util.UUID;

public record ParticipantQuestionOptionResponse(
        UUID optionId,
        String text,
        int order
) {
}
