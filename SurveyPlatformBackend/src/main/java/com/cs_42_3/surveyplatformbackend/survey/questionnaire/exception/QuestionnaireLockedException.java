package com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception;

import java.util.UUID;

/** Indicates that a questionnaire cannot be edited after its study leaves draft state. */
public class QuestionnaireLockedException extends RuntimeException {
    public QuestionnaireLockedException(UUID studyId) {
        super("Questionnaire is locked because study is not in DRAFT state: " + studyId);
    }
}
