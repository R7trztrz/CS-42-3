package com.cs_42_3.surveyplatformbackend.survey.repository;

import com.cs_42_3.surveyplatformbackend.survey.entity.Question;
import com.cs_42_3.surveyplatformbackend.survey.entity.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    /**
     * UC-21: list + optional type/keyword filtering, always scoped to the requesting
     * researcher (UC-04 ownership constraint enforced here at the query level, not just
     * checked after the fact).
     *
     * A single JPQL query with null-checked optional parameters is used instead of the
     * JPA Specification API — the filter set is small and fixed (type + keyword), so the
     * extra abstraction isn't earning its complexity for v1.
     */
    @Query("""
            SELECT q FROM Question q
            WHERE q.researcherId = :researcherId
              AND (:type IS NULL OR q.type = :type)
              AND (:keyword IS NULL OR LOWER(q.questionText) LIKE LOWER(CONCAT('%', :keyword, '%')))
            ORDER BY q.updatedAt DESC
            """)
    List<Question> search(
            @Param("researcherId") Long researcherId,
            @Param("type") QuestionType type,
            @Param("keyword") String keyword
    );

    /**
     * Ownership-scoped single lookup — used by edit/delete so a researcher can never
     * fetch (or find out the existence of) a question that isn't theirs.
     */
    Optional<Question> findByIdAndResearcherId(Long id, Long researcherId);
}
