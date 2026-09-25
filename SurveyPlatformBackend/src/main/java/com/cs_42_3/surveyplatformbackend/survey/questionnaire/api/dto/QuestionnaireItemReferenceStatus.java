package com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto;

/** Whether a stable questionnaire item still resolves to a live question-bank entry. */
public enum QuestionnaireItemReferenceStatus {
    VALID,
    MISSING_QUESTION
}
