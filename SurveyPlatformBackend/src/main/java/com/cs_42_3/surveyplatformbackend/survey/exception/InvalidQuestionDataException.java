package com.cs_42_3.surveyplatformbackend.survey.exception;

/**
 * Indicates that question data violates a type-specific business rule.
 */
public class InvalidQuestionDataException extends RuntimeException {

    private final String code;

    public InvalidQuestionDataException(String message) {
        this("QUESTION_VALIDATION_ERROR", message);
    }

    public InvalidQuestionDataException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
