package com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception;

import com.cs_42_3.surveyplatformbackend.survey.api.dto.SurveyErrorDetail;

import java.util.List;

/** Indicates one or more client-correctable questionnaire request errors. */
public class QuestionnaireValidationException extends RuntimeException {

    private final List<SurveyErrorDetail> details;

    public QuestionnaireValidationException(String message) {
        this(message, List.of());
    }

    public QuestionnaireValidationException(String message, List<SurveyErrorDetail> details) {
        super(message);
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public List<SurveyErrorDetail> getDetails() {
        return details;
    }
}
