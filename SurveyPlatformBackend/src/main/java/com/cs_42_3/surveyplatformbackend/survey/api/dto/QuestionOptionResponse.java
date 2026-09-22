package com.cs_42_3.surveyplatformbackend.survey.api.dto;

import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionOption;

import java.util.UUID;

/**
 * Response model for one ordered choice in a survey question.
 */
public record QuestionOptionResponse(
        UUID id,
        String optionText,
        int optionOrder
) {

    /**
     * Maps a persisted option to its API representation.
     *
     * @param option the persisted option
     * @return the response representation
     */
    public static QuestionOptionResponse from(QuestionOption option) {
        return new QuestionOptionResponse(option.getId(), option.getOptionText(), option.getOptionOrder());
    }
}
