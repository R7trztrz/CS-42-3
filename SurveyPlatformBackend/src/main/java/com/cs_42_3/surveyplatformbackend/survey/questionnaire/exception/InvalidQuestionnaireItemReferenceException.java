package com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception;

import com.cs_42_3.surveyplatformbackend.survey.api.dto.SurveyErrorDetail;

import java.util.List;

/** Indicates item IDs that do not belong to the questionnaire being updated. */
public class InvalidQuestionnaireItemReferenceException extends QuestionnaireValidationException {
    public InvalidQuestionnaireItemReferenceException(List<SurveyErrorDetail> details) {
        super("One or more questionnaire item references are invalid", details);
    }
}
