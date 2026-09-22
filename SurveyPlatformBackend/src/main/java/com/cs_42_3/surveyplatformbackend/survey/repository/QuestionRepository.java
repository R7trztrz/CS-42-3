package com.cs_42_3.surveyplatformbackend.survey.repository;

import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence operations for reusable survey questions.
 */
public interface QuestionRepository
        extends JpaRepository<Question, UUID>, JpaSpecificationExecutor<Question> {

    /**
     * Finds questions owned by one researcher with optional type and text filters.
     *
     * @param researcherId owner of the returned questions
     * @param type optional question type
     * @param keyword optional normalized case-insensitive text fragment
     * @return matching questions ordered by most recent update
     */
    default List<Question> searchQuestions(
            UUID researcherId,
            QuestionType type,
            String keyword
    ) {
        return findAll(
                QuestionSpecifications.forSearch(researcherId, type, keyword),
                Sort.by(
                        Sort.Order.desc("updatedAt"),
                        Sort.Order.desc("id")
                )
        );
    }

    /**
     * Finds a question only when it belongs to the supplied researcher.
     *
     * @param questionId question identifier
     * @param researcherId expected owner
     * @return the owned question, or an empty result
     */
    Optional<Question> findByIdAndResearcherId(UUID questionId, UUID researcherId);

    /**
     * Finds the subset of the given questions owned by one researcher.
     * <p>
     * Used by the questionnaire module to validate that every question a researcher
     * wants to enable actually exists and belongs to them, in one round trip.
     *
     * @param ids candidate question identifiers
     * @param researcherId expected owner
     * @return the owned questions among the candidates, in no particular order
     */
    List<Question> findAllByIdInAndResearcherId(Collection<UUID> ids, UUID researcherId);
}
