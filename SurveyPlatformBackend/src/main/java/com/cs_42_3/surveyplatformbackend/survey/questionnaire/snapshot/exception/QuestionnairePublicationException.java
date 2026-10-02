package com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.exception;

import com.cs_42_3.surveyplatformbackend.survey.api.dto.SurveyErrorDetail;

import java.util.List;

/** Client-correctable reason that an enabled questionnaire cannot be published. */
public class QuestionnairePublicationException extends RuntimeException {

    private final List<SurveyErrorDetail> details;

    public QuestionnairePublicationException(String message, List<SurveyErrorDetail> details) {
        super(message);
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public List<SurveyErrorDetail> getDetails() {
        return details;
    }
}
