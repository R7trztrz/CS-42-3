package com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception;

import com.cs_42_3.surveyplatformbackend.survey.api.dto.SurveyErrorDetail;

import java.util.List;

/** Hides whether referenced questions are absent or owned by another researcher. */
public class InvalidQuestionReferenceException extends QuestionnaireValidationException {
    public InvalidQuestionReferenceException(List<SurveyErrorDetail> details) {
        super("One or more question references are invalid", details);
    }
}
