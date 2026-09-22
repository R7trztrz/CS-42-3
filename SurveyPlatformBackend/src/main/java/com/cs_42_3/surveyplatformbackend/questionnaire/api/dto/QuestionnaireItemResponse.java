package com.cs_42_3.surveyplatformbackend.questionnaire.api.dto;

import com.cs_42_3.surveyplatformbackend.survey.api.dto.QuestionResponse;

import java.util.List;
import java.util.UUID;

/**
 * Response model for one enabled item, including the live bank content it currently
 * resolves to so the editor can render it without a second request.
 */
public record QuestionnaireItemResponse(
        UUID id,
        int position,
        QuestionResponse question,
        List<QuestionnaireBranchRuleResponse> branchRules
) {

    public QuestionnaireItemResponse {
        branchRules = branchRules == null ? List.of() : List.copyOf(branchRules);
    }
}
