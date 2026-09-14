package com.cs_42_3.surveyplatformbackend.survey.exception;

import java.util.UUID;

/**
 * Indicates that a question is absent or is not owned by the current researcher.
 */
public class QuestionNotFoundException extends RuntimeException {

    public QuestionNotFoundException(UUID questionId) {
        super("Question not found: " + questionId);
    }
}
