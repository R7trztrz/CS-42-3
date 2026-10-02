package com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto;

/** Identifies whether a questionnaire response came from mutable draft data or publication content. */
public enum QuestionnaireContentSource {
    LIVE_DRAFT,
    PUBLISHED_SNAPSHOT
}
