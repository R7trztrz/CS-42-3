package com.cs_42_3.surveyplatformbackend.questionnaire.exception;

import java.util.UUID;

/**
 * Indicates that a study is absent or is not owned by the current researcher.
 */
public class QuestionnaireStudyNotFoundException extends RuntimeException {

    public QuestionnaireStudyNotFoundException(UUID studyId) {
        super("Study not found: " + studyId);
    }
}
