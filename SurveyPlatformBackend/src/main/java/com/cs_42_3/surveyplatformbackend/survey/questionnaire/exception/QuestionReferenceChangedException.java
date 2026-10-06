package com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception;

/** Indicates that a validated question was removed before the questionnaire committed. */
public class QuestionReferenceChangedException extends RuntimeException {
    public QuestionReferenceChangedException() {
        super("A referenced question changed while the questionnaire was being saved");
    }
}
