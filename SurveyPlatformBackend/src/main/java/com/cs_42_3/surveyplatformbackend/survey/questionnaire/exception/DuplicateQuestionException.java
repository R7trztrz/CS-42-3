package com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception;

import com.cs_42_3.surveyplatformbackend.survey.api.dto.SurveyErrorDetail;

import java.util.List;

/** Indicates duplicate question references in one questionnaire request. */
public class DuplicateQuestionException extends QuestionnaireValidationException {
    public DuplicateQuestionException(List<SurveyErrorDetail> details) {
        super("A questionnaire cannot contain the same question more than once", details);
    }
}
