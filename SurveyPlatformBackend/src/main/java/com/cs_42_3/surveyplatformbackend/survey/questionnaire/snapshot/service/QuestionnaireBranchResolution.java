package com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.service;

import java.util.UUID;

/** Deterministic next-step decision for one published answer. */
public record QuestionnaireBranchResolution(UUID nextItemId, Transition transition) {
    public enum Transition {
        CONDITIONAL,
        DEFAULT,
        END
    }
}
