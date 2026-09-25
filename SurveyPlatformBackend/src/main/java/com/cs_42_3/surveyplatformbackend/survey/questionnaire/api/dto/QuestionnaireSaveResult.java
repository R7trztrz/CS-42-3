package com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto;

import java.util.Objects;

/** Distinguishes initial creation from update and idempotent replay. */
public record QuestionnaireSaveResult(
        QuestionnaireResponse response,
        boolean created
) {
    public QuestionnaireSaveResult {
        Objects.requireNonNull(response, "Questionnaire response is required");
    }
}
