package com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception;

/** Indicates that the submitted questionnaire version cannot be applied safely. */
public class QuestionnaireVersionConflictException extends RuntimeException {
    public QuestionnaireVersionConflictException(String message) {
        super(message);
    }
}
