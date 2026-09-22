package com.cs_42_3.surveyplatformbackend.questionnaire.service.implementation;

import com.cs_42_3.surveyplatformbackend.questionnaire.repository.QuestionnaireItemRepository;
import com.cs_42_3.surveyplatformbackend.survey.service.QuestionUsageGuard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Backs {@link QuestionUsageGuard} with the questionnaire module's own enabled-item
 * data, so the survey module can protect bank edits and deletes (NFR-14) without
 * depending on the questionnaire module.
 */
@Component
@RequiredArgsConstructor
public class QuestionUsageGuardImpl implements QuestionUsageGuard {

    private final QuestionnaireItemRepository questionnaireItemRepository;

    @Override
    @Transactional(readOnly = true)
    public boolean isReferencedByAnyQuestionnaire(UUID questionId) {
        return questionnaireItemRepository.existsByQuestionId(questionId);
    }
}
