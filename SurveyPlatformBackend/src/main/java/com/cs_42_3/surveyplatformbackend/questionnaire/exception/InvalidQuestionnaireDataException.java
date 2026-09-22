package com.cs_42_3.surveyplatformbackend.questionnaire.exception;

/**
 * Indicates that a questionnaire save request violates a structural or branching rule.
 */
public class InvalidQuestionnaireDataException extends RuntimeException {

    public InvalidQuestionnaireDataException(String message) {
        super(message);
    }
}
