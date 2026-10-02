package com.cs_42_3.surveyplatformbackend.survey.questionnaire.service.implementation;

import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.repository.QuestionnaireRepository;
import com.cs_42_3.surveyplatformbackend.survey.service.QuestionnaireQuestionReferencePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionnaireQuestionReferenceServiceTest {

    private static final UUID RESEARCHER_ID = UUID.randomUUID();
    private static final UUID STUDY_ID = UUID.randomUUID();
    private static final UUID QUESTION_ID = UUID.randomUUID();

    @Mock
    private QuestionnaireRepository questionnaireRepository;
    @Mock
    private StudyRepository studyRepository;

    private QuestionnaireQuestionReferenceService service;

    @BeforeEach
    void setUp() {
        service = new QuestionnaireQuestionReferenceService(
                questionnaireRepository,
                studyRepository
        );
    }

    @Test
    void locksStudyThenQuestionnaireSoPublicationAndQuestionChangesAreSerialized() {
        UUID questionnaireId = UUID.randomUUID();
        Study collectingStudy = new Study(RESEARCHER_ID, "Collecting", null);
        ReflectionTestUtils.setField(collectingStudy, "id", STUDY_ID);
        ReflectionTestUtils.setField(collectingStudy, "status", StudyStatus.COLLECTING);
        when(questionnaireRepository.findReferencedStudyIds(QUESTION_ID))
                .thenReturn(List.of(STUDY_ID));
        when(studyRepository.lockAllByIdsForQuestionReferenceChange(List.of(STUDY_ID)))
                .thenReturn(List.of(collectingStudy));
        when(questionnaireRepository.findIdsReferencingQuestion(QUESTION_ID))
                .thenReturn(List.of(questionnaireId));

        var lock = service.lockReferences(QUESTION_ID);

        assertThat(lock.studyIds()).containsExactly(STUDY_ID);
        assertThat(lock.questionnaireIds()).containsExactly(questionnaireId);
        assertThat(lock.hasNonDraftReference()).isTrue();
        InOrder order = inOrder(studyRepository, questionnaireRepository);
        order.verify(questionnaireRepository).findReferencedStudyIds(QUESTION_ID);
        order.verify(studyRepository).lockAllByIdsForQuestionReferenceChange(List.of(STUDY_ID));
        order.verify(questionnaireRepository).findIdsReferencingQuestion(QUESTION_ID);
        order.verify(questionnaireRepository).lockAllByIds(List.of(questionnaireId));
    }

    @Test
    void nonDraftReferencesDoNotBlockQuestionUpdatesOrDeletion() {
        Question question = Question.create(
                RESEARCHER_ID,
                QuestionType.TEXT,
                "Current",
                false,
                null,
                null,
                null,
                null
        );
        var lock = new QuestionnaireQuestionReferencePort.ReferenceLock(
                Set.of(STUDY_ID),
                Set.of(UUID.randomUUID()),
                true
        );
        var proposed = new QuestionnaireQuestionReferencePort.ProposedQuestionDefinition(
                QuestionType.SINGLE_CHOICE,
                true,
                Set.of(),
                2,
                null,
                null
        );

        assertThatCode(() -> service.validateUpdate(question, proposed, lock))
                .doesNotThrowAnyException();
        assertThatCode(() -> service.prepareDelete(question, lock))
                .doesNotThrowAnyException();
    }

    @Test
    void unreferencedQuestionDoesNotTakeAnyDatabaseLocks() {
        when(questionnaireRepository.findReferencedStudyIds(QUESTION_ID)).thenReturn(List.of());
        when(questionnaireRepository.findIdsReferencingQuestion(QUESTION_ID)).thenReturn(List.of());

        var lock = service.lockReferences(QUESTION_ID);

        assertThat(lock.studyIds()).isEmpty();
        assertThat(lock.questionnaireIds()).isEmpty();
        assertThat(lock.hasNonDraftReference()).isFalse();
        verifyNoInteractions(studyRepository);
    }
}
