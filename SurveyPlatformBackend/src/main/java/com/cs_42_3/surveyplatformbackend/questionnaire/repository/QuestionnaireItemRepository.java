package com.cs_42_3.surveyplatformbackend.questionnaire.repository;

import com.cs_42_3.surveyplatformbackend.questionnaire.domain.QuestionnaireItem;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Read access to enabled questionnaire items.
 * <p>
 * Items are otherwise managed only through their owning
 * {@link com.cs_42_3.surveyplatformbackend.questionnaire.domain.Questionnaire} aggregate;
 * this repository exists so {@code QuestionUsageGuardImpl} can protect question-bank
 * rows that are in active use (NFR-14).
 */
public interface QuestionnaireItemRepository extends JpaRepository<QuestionnaireItem, UUID> {

    /**
     * Checks whether any questionnaire currently enables the given question.
     *
     * @param questionId a question-bank question identifier
     * @return true if at least one questionnaire item references it
     */
    boolean existsByQuestionId(UUID questionId);
}
