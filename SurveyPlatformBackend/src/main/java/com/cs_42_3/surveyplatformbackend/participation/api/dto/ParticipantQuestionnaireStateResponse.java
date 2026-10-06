package com.cs_42_3.surveyplatformbackend.participation.api.dto;

public record ParticipantQuestionnaireStateResponse(
        ParticipantQuestionResponse currentQuestion,
        boolean readyToSubmit
) {
}
