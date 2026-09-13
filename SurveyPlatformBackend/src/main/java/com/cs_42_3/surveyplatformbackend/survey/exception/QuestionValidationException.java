package com.cs_42_3.surveyplatformbackend.survey.exception;

/**
 * Raised for validation rules that depend on the question's type (e.g. "single-choice
 * needs >=2 options", "scaleMin must be less than scaleMax") — rules plain Bean Validation
 * annotations on QuestionRequest can't express since they're conditional on another field.
 */
public class QuestionValidationException extends RuntimeException {
    public QuestionValidationException(String message) {
        super(message);
    }
}
