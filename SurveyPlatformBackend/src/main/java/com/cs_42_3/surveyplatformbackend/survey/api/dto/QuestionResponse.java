package com.cs_42_3.surveyplatformbackend.survey.api.dto;

import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Detailed response model for a reusable survey question.
 */
public record QuestionResponse(
        UUID id,
        QuestionType type,
        String questionText,
        boolean required,
        List<QuestionOptionResponse> options,
        Integer scaleMin,
        Integer scaleMax,
        String scaleMinLabel,
        String scaleMaxLabel,
        Instant createdAt,
        Instant updatedAt
) {

    public QuestionResponse {
        options = options == null ? List.of() : List.copyOf(options);
    }

    /**
     * Maps a persisted question to its API representation.
     * <p>
     * Reused outside the survey module (see the questionnaire module) wherever an
     * enabled item's live bank content needs to be rendered.
     *
     * @param question the persisted question
     * @return the response representation
     */
    public static QuestionResponse from(Question question) {
        List<QuestionOptionResponse> options = question.getOptions().stream()
                .map(QuestionOptionResponse::from)
                .toList();

        return new QuestionResponse(
                question.getId(),
                question.getType(),
                question.getQuestionText(),
                question.isRequired(),
                options,
                question.getScaleMin(),
                question.getScaleMax(),
                question.getScaleMinLabel(),
                question.getScaleMaxLabel(),
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }
}
