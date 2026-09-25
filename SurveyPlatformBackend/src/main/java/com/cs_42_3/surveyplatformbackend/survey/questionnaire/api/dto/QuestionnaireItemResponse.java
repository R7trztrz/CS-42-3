package com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto;

import com.cs_42_3.surveyplatformbackend.survey.api.dto.QuestionResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** One ordered questionnaire item, including a detectable deleted-question placeholder. */
public record QuestionnaireItemResponse(
        @Schema(description = "Stable identifier used for future reordering and branching")
        UUID itemId,
        @Schema(description = "Zero-based display position")
        int position,
        @Schema(description = "True when the referenced question-bank entry has been deleted")
        boolean missing,
        @Schema(description = "Latest full question data, or null when missing", nullable = true)
        QuestionResponse question
) {}
