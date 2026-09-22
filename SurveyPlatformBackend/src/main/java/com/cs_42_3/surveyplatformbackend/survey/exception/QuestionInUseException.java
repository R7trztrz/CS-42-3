package com.cs_42_3.surveyplatformbackend.survey.exception;

import java.util.UUID;

/**
 * Indicates that a question cannot be edited or deleted because a questionnaire
 * currently enables it; remove it from every questionnaire first (FR-37).
 */
public class QuestionInUseException extends RuntimeException {

    public QuestionInUseException(UUID questionId) {
        super("Question is enabled in a questionnaire and cannot be changed: " + questionId);
    }
}
