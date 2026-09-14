package com.cs_42_3.surveyplatformbackend.survey.exception;

/**
 * Indicates that question data violates a type-specific business rule.
 */
public class InvalidQuestionDataException extends RuntimeException {

    public InvalidQuestionDataException(String message) {
        super(message);
    }
}
