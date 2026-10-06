package com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception;

import java.util.UUID;

/** Indicates that a study does not yet have a questionnaire. */
public class QuestionnaireNotFoundException extends RuntimeException {
    public QuestionnaireNotFoundException(UUID studyId) {
        super("Questionnaire not found for study: " + studyId);
    }
}
