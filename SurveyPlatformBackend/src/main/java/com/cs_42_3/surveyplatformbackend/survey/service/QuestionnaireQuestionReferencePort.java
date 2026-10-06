package com.cs_42_3.surveyplatformbackend.survey.service;

import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Locks questionnaire references while the question bank changes answer domains.
 * The question service depends on this port rather than questionnaire repositories.
 */
public interface QuestionnaireQuestionReferencePort {

    ReferenceLock lockReferences(UUID questionId);

    void validateUpdate(
            Question currentQuestion,
            ProposedQuestionDefinition proposedDefinition,
            ReferenceLock referenceLock
    );

    void prepareDelete(Question currentQuestion, ReferenceLock referenceLock);

    record ReferenceLock(
            Set<UUID> studyIds,
            Set<UUID> questionnaireIds,
            boolean hasNonDraftReference
    ) {
        public ReferenceLock {
            studyIds = Set.copyOf(studyIds);
            questionnaireIds = Set.copyOf(questionnaireIds);
        }
    }

    record ProposedQuestionDefinition(
            QuestionType type,
            boolean required,
            Set<UUID> retainedOptionIds,
            int optionCount,
            Integer scaleMin,
            Integer scaleMax
    ) {
        public ProposedQuestionDefinition {
            retainedOptionIds = Collections.unmodifiableSet(
                    new HashSet<>(retainedOptionIds)
            );
        }
    }
}
