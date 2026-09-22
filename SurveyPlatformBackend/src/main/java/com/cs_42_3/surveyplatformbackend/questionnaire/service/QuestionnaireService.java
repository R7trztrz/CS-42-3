package com.cs_42_3.surveyplatformbackend.questionnaire.service;

import com.cs_42_3.surveyplatformbackend.questionnaire.api.dto.QuestionnaireResponse;
import com.cs_42_3.surveyplatformbackend.questionnaire.api.dto.SaveQuestionnaireRequest;

import java.util.UUID;

/**
 * Defines researcher-owned questionnaire editing operations for FR36-FR39.
 */
public interface QuestionnaireService {

    /**
     * Gets the current researcher's questionnaire draft for one study.
     * <p>
     * Returns an empty structure (null id, no items) if nothing has been saved yet;
     * the questionnaire row itself is created lazily by the first
     * {@link #saveQuestionnaire} call, not by this read.
     *
     * @param studyId the owning study
     * @return the questionnaire's current editable structure
     */
    QuestionnaireResponse getQuestionnaire(UUID studyId);

    /**
     * Replaces a study's enabled items, order, and branch rules as one unit
     * (FR-36, FR-37, FR-38, FR-39).
     *
     * @param studyId the owning study
     * @param request the full replacement structure
     * @return the saved questionnaire
     */
    QuestionnaireResponse saveQuestionnaire(UUID studyId, SaveQuestionnaireRequest request);
}
