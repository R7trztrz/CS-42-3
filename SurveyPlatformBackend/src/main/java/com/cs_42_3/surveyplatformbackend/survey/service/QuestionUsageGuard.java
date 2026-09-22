package com.cs_42_3.surveyplatformbackend.survey.service;

import java.util.UUID;

/**
 * Reports whether a question-bank question is currently enabled in any questionnaire.
 * <p>
 * Implemented outside the survey module (see the questionnaire module's
 * {@code QuestionUsageGuardImpl}) so survey stays unaware of how or where questions are
 * consumed; it only depends on this port. {@code QuestionServiceImpl} uses it to protect
 * NFR-14: a question in active use cannot be edited or deleted out from under a
 * questionnaire that references it.
 */
public interface QuestionUsageGuard {

    /**
     * Checks whether a question is referenced by at least one questionnaire item.
     *
     * @param questionId a question-bank question identifier
     * @return true if editing or deleting the question would affect a questionnaire
     */
    boolean isReferencedByAnyQuestionnaire(UUID questionId);
}
