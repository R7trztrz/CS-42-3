package com.cs_42_3.surveyplatformbackend.survey.questionnaire.service;

import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.QuestionnaireResponse;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.QuestionnaireSaveResult;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.SaveQuestionnaireRequest;

import java.util.UUID;

/** Researcher operations for reading and replacing questionnaire drafts. */
public interface QuestionnaireService {

    QuestionnaireResponse getQuestionnaire(UUID studyId);

    QuestionnaireSaveResult saveQuestionnaire(UUID studyId, SaveQuestionnaireRequest request);
}
