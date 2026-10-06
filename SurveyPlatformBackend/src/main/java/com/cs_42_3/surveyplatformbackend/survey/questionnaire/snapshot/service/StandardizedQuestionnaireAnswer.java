package com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.service;

import java.util.Set;
import java.util.UUID;

/** Normalized answer value consumed by the stateless publication flow resolver. */
public record StandardizedQuestionnaireAnswer(
        UUID optionId,
        Set<UUID> optionIds,
        Integer scaleValue,
        String textValue
) {
    public StandardizedQuestionnaireAnswer {
        optionIds = optionIds == null ? Set.of() : Set.copyOf(optionIds);
    }

    public static StandardizedQuestionnaireAnswer singleChoice(UUID optionId) {
        return new StandardizedQuestionnaireAnswer(optionId, Set.of(), null, null);
    }

    public static StandardizedQuestionnaireAnswer multiChoice(Set<UUID> optionIds) {
        return new StandardizedQuestionnaireAnswer(null, optionIds, null, null);
    }

    public static StandardizedQuestionnaireAnswer scale(int value) {
        return new StandardizedQuestionnaireAnswer(null, Set.of(), value, null);
    }

    public static StandardizedQuestionnaireAnswer text(String value) {
        return new StandardizedQuestionnaireAnswer(null, Set.of(), null, value);
    }

    /** Represents an intentionally skipped optional question. */
    public static StandardizedQuestionnaireAnswer unanswered() {
        return new StandardizedQuestionnaireAnswer(null, Set.of(), null, null);
    }
}
