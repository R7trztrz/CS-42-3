package com.cs_42_3.surveyplatformbackend.questionnaire.domain;

import java.util.List;
import java.util.UUID;

/**
 * One validated enabled-item entry supplied to {@link Questionnaire#replaceItems}.
 * <p>
 * Its position within the enclosing list is its display order and its default jump
 * target when a participant's answer matches no branch rule; it is also the index that
 * {@link QuestionnaireBranchRulePlan#targetPosition()} refers to.
 *
 * @param questionId the referenced question-bank question
 * @param branchRules this item's per-answer jump rules
 */
public record QuestionnaireItemPlan(
        UUID questionId,
        List<QuestionnaireBranchRulePlan> branchRules
) {

    public QuestionnaireItemPlan {
        branchRules = branchRules == null ? List.of() : List.copyOf(branchRules);
    }
}
