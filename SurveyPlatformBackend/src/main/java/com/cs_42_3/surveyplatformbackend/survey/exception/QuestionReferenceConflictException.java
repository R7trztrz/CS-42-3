package com.cs_42_3.surveyplatformbackend.survey.exception;

/** A requested question edit would invalidate a questionnaire reference or flow. */
public class QuestionReferenceConflictException extends RuntimeException {

    private final String code;

    public QuestionReferenceConflictException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
