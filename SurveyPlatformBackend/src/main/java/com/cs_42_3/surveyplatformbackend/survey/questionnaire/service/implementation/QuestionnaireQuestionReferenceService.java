package com.cs_42_3.surveyplatformbackend.survey.questionnaire.service.implementation;

import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.repository.QuestionnaireRepository;
import com.cs_42_3.surveyplatformbackend.survey.service.QuestionnaireQuestionReferencePort;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * Serializes question-bank changes with questionnaire saves and publication.
 *
 * <p>Question content is intentionally allowed to change after publication: published studies read
 * their immutable snapshot, while draft questionnaires immediately expose the latest live content.
 */
@Component
@RequiredArgsConstructor
public class QuestionnaireQuestionReferenceService implements QuestionnaireQuestionReferencePort {

    private final QuestionnaireRepository questionnaireRepository;
    private final StudyRepository studyRepository;

    @Override
    public ReferenceLock lockReferences(UUID questionId) {
        List<UUID> studyIds = questionnaireRepository.findReferencedStudyIds(questionId);
        List<Study> studies = studyIds.isEmpty()
                ? List.of()
                : studyRepository.lockAllByIdsForQuestionReferenceChange(studyIds);

        List<UUID> questionnaireIds = questionnaireRepository.findIdsReferencingQuestion(questionId);
        if (!questionnaireIds.isEmpty()) {
            questionnaireRepository.lockAllByIds(questionnaireIds);
        }

        return new ReferenceLock(
                new HashSet<>(studyIds),
                new HashSet<>(questionnaireIds),
                studies.stream().anyMatch(study -> study.getStatus() != StudyStatus.DRAFT)
        );
    }

    @Override
    public void validateUpdate(
            Question currentQuestion,
            ProposedQuestionDefinition proposedDefinition,
            ReferenceLock referenceLock
    ) {
        // The lock is the contract: a draft may become invalid and publication will reject it.
    }

    @Override
    public void prepareDelete(Question currentQuestion, ReferenceLock referenceLock) {
        // V12 keeps a missing item via ON DELETE SET NULL; V14 decouples option UUIDs from live options.
    }
}
