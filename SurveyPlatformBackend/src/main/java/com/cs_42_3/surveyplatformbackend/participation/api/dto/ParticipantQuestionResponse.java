package com.cs_42_3.surveyplatformbackend.participation.api.dto;

import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;

import java.util.List;
import java.util.UUID;

public record ParticipantQuestionResponse(
        UUID itemId,
        int position,
        QuestionType questionType,
        String text,
        boolean required,
        List<ParticipantQuestionOptionResponse> options,
        Integer scaleMin,
        Integer scaleMax,
        String scaleMinLabel,
        String scaleMaxLabel
) {
    public ParticipantQuestionResponse {
        options = options == null ? List.of() : List.copyOf(options);
    }
}
