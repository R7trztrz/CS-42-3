package com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception;

/** Database fallback when two questionnaire creations bypass normal study locking. */
public class ConcurrentQuestionnaireCreationException extends RuntimeException {
    public ConcurrentQuestionnaireCreationException() {
        super("The questionnaire was created concurrently; reload it and retry");
    }
}
