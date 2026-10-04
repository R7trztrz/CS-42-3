package com.cs_42_3.surveyplatformbackend.participation.api.dto;

import java.util.List;
import java.util.UUID;

/** Mutually exclusive answer fields are validated against the published item type. */
public record SubmitQuestionnaireAnswerRequest(
        UUID optionId,
        List<UUID> optionIds,
        Integer scaleValue,
        String textValue,
        Boolean unanswered
) {
    public SubmitQuestionnaireAnswerRequest {
        optionIds = optionIds == null ? null : List.copyOf(optionIds);
    }

    public boolean isUnanswered() {
        return Boolean.TRUE.equals(unanswered);
    }
}
