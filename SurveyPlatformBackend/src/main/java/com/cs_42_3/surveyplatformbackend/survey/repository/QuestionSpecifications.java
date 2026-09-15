package com.cs_42_3.surveyplatformbackend.survey.repository;

import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;

import org.springframework.data.jpa.domain.Specification;

import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

/**
 * Builds JPA specifications for researcher-owned question searches.
 */
public final class QuestionSpecifications {

    private QuestionSpecifications() {}

    /**
     * Builds an ownership specification with optional type and text filters.
     *
     * @param researcherId owner of the returned questions
     * @param type optional exact question type
     * @param keyword optional normalized question-text fragment
     * @return specification containing only the supplied filters
     */
    public static Specification<Question> forSearch(
            UUID researcherId,
            QuestionType type,
            String keyword
    ) {
        Specification<Question> specification = ownedBy(researcherId);

        if (type != null) {
            specification = specification.and(hasType(type));
        }
        if (keyword != null) {
            specification = specification.and(questionTextContains(keyword));
        }

        return specification;
    }

    private static Specification<Question> ownedBy(UUID researcherId) {
        UUID requiredResearcherId = Objects.requireNonNull(
                researcherId,
                "Researcher ID is required"
        );
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                root.get("researcherId"),
                requiredResearcherId
        );
    }

    private static Specification<Question> hasType(QuestionType type) {
        QuestionType requiredType = Objects.requireNonNull(type, "Question type is required");
        return (root, query, criteriaBuilder) -> criteriaBuilder.equal(
                root.get("type"),
                requiredType
        );
    }

    private static Specification<Question> questionTextContains(String keyword) {
        String requiredKeyword = Objects.requireNonNull(keyword, "Keyword is required");
        String pattern = "%" + requiredKeyword.toLowerCase(Locale.ROOT) + "%";
        return (root, query, criteriaBuilder) -> criteriaBuilder.like(
                criteriaBuilder.lower(root.get("questionText")),
                pattern
        );
    }
}
