package com.cs_42_3.surveyplatformbackend.survey.api.dto;

import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;

import java.time.Instant;
import java.util.UUID;

/**
 * Compact response model used when listing reusable survey questions.
 */
public record QuestionSummaryResponse(
        UUID questionId,
        QuestionType type,
        String questionText,
        Instant updatedAt
) {
    public static QuestionSummaryResponse from(
            com.cs_42_3.surveyplatformbackend.survey.domain.Question question
    ) {
        return new QuestionSummaryResponse(
                question.getId(),
                question.getType(),
                question.getQuestionText(),
                question.getUpdatedAt()
        );
    }
}
