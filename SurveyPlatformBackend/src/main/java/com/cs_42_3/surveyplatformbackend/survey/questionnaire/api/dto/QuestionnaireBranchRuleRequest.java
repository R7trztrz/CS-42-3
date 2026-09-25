package com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.PositiveOrZero;

import java.util.UUID;

/** One answer-triggered transition supplied as part of a whole questionnaire save. */
public record QuestionnaireBranchRuleRequest(
        @Schema(description = "SINGLE_CHOICE option trigger; mutually exclusive with sourceScaleValue")
        UUID sourceOptionId,
        @Schema(description = "SCALE value trigger; mutually exclusive with sourceOptionId")
        Integer sourceScaleValue,
        @PositiveOrZero
        @Schema(description = "Zero-based index in the final items array")
        Integer targetPosition
) {}
