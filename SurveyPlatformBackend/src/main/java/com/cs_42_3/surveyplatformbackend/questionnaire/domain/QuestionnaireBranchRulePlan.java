package com.cs_42_3.surveyplatformbackend.questionnaire.domain;

import java.util.UUID;

/**
 * One validated branch rule supplied to {@link Questionnaire#replaceItems}.
 * <p>
 * Exactly one of {@code sourceOptionId} or {@code sourceScaleValue} is set, matching the
 * enclosing item's question type; the service layer validates this before building plans.
 *
 * @param sourceOptionId triggering option, for SINGLE_CHOICE source items
 * @param sourceScaleValue triggering scale value, for SCALE source items
 * @param targetPosition index within the enclosing plan list of the jump target
 */
public record QuestionnaireBranchRulePlan(
        UUID sourceOptionId,
        Integer sourceScaleValue,
        int targetPosition
) {}
