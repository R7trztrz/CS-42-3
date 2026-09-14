package com.cs_42_3.surveyplatformbackend.survey.exception;

/**
 * Indicates that the authenticated principal has no usable researcher UUID.
 */
public class InvalidResearcherIdentityException extends RuntimeException {

    public InvalidResearcherIdentityException(String message) {
        super(message);
    }
}
