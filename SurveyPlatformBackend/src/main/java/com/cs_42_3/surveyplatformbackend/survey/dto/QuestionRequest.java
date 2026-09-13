package com.cs_42_3.surveyplatformbackend.survey.dto;

import com.cs_42_3.surveyplatformbackend.survey.entity.QuestionType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Create/update payload for a question. Only the fields relevant to the request's
 * `type` need to be populated — e.g. `options` for SINGLE_CHOICE/MULTI_CHOICE,
 * `scaleMin`/`scaleMax` for SCALE. Fields irrelevant to the given type are ignored
 * by the service layer rather than rejected, so a stray field left over from switching
 * question type in the UI doesn't block saving.
 *
 * Type-conditional requirements (e.g. "options required and >=2 for choice questions")
 * can't be expressed with plain Bean Validation annotations here since they depend on
 * another field's value — that logic lives in QuestionService.validateForType() instead,
 * which is what lets it return a precise, field-located error message (UC-22 AC2).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionRequest {

    @NotNull(message = "Question type is required")
    private QuestionType type;

    @NotBlank(message = "Question text must not be blank")
    private String questionText;

    /** Required (>=2 entries) when type is SINGLE_CHOICE or MULTI_CHOICE. Ignored otherwise. */
    @Valid
    private List<QuestionOptionRequest> options;

    /** Required when type is SCALE. Ignored otherwise. */
    private Integer scaleMin;
    private Integer scaleMax;
    private String scaleMinLabel;
    private String scaleMaxLabel;
}
