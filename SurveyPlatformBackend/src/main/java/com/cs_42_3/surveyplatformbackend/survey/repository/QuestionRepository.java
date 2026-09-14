package com.cs_42_3.surveyplatformbackend.survey.repository;

import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Persistence operations for reusable survey questions.
 */
public interface QuestionRepository extends JpaRepository<Question, UUID> {

    /**
     * Finds questions owned by one researcher with optional type and text filters.
     *
     * @param researcherId owner of the returned questions
     * @param type optional question type
     * @param keyword optional case-insensitive text fragment
     * @return matching questions ordered by most recent update
     */
    @Query("""
            SELECT q FROM Question q
            WHERE q.researcherId = :researcherId
              AND (:type IS NULL OR q.type = :type)
              AND (:keyword IS NULL OR LOWER(q.questionText) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY q.updatedAt DESC, q.id DESC
            """)
    List<Question> searchQuestions(
            @Param("researcherId") UUID researcherId,
            @Param("type") QuestionType type,
            @Param("keyword") String keyword
    );

    /**
     * Finds a question only when it belongs to the supplied researcher.
     *
     * @param questionId question identifier
     * @param researcherId expected owner
     * @return the owned question, or an empty result
     */
    Optional<Question> findByIdAndResearcherId(UUID questionId, UUID researcherId);
}
