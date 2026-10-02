package com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto;

import com.cs_42_3.surveyplatformbackend.survey.api.dto.QuestionResponse;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
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
        QuestionResponse question,
        QuestionnaireItemReferenceStatus referenceStatus,
        List<QuestionnaireBranchRuleResponse> branchRules,
        @Schema(description = "Implicit default successor item, or null when this item ends the questionnaire")
        UUID defaultNextItemId
) {
    public QuestionnaireItemResponse {
        referenceStatus = referenceStatus == null
                ? (missing ? QuestionnaireItemReferenceStatus.MISSING_QUESTION
                        : QuestionnaireItemReferenceStatus.VALID)
                : referenceStatus;
        branchRules = branchRules == null ? List.of() : List.copyOf(branchRules);
    }

    public QuestionnaireItemResponse(
            UUID itemId,
            int position,
            boolean missing,
            QuestionResponse question
    ) {
        this(itemId, position, missing, question, null, List.of(), null);
    }

    public QuestionnaireItemResponse(
            UUID itemId,
            int position,
            boolean missing,
            QuestionResponse question,
            QuestionnaireItemReferenceStatus referenceStatus,
            List<QuestionnaireBranchRuleResponse> branchRules
    ) {
        this(itemId, position, missing, question, referenceStatus, branchRules, null);
    }
}
