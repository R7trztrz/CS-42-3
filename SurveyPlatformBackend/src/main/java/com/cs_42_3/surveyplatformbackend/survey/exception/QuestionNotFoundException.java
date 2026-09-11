package com.cs_42_3.surveyplatformbackend.survey.exception;

/**
 * Thrown for both "doesn't exist" and "exists but isn't yours" — findByIdAndResearcherId
 * returning empty covers both cases identically, which is the "stealth" 404 strategy
 * discussed for UC-04 (don't let a 403 leak whether a resource exists at all).
 */
public class QuestionNotFoundException extends RuntimeException {
    public QuestionNotFoundException(Long id) {
        super("Question not found: " + id);
    }
}
