package com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.service;

import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.domain.Questionnaire;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.domain.QuestionnaireSnapshot;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.domain.QuestionnaireSnapshotPayload;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.exception.QuestionnairePublicationException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.repository.QuestionnaireRepository;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.service.QuestionnaireFlowValidator;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.repository.QuestionnaireSnapshotRepository;
import com.cs_42_3.surveyplatformbackend.survey.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionnaireSnapshotPublicationServiceTest {

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID STUDY_ID = UUID.randomUUID();
    private static final Instant PUBLISHED_AT = Instant.parse("2026-10-02T05:30:00Z");

    @Mock
    private QuestionnaireRepository questionnaireRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private QuestionnaireSnapshotRepository snapshotRepository;

    private ObjectMapper objectMapper;
    private QuestionnaireSnapshotPublicationService service;
    private Study study;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        service = new QuestionnaireSnapshotPublicationService(
                questionnaireRepository,
                questionRepository,
                snapshotRepository,
                new QuestionnaireFlowValidator(),
                objectMapper
        );
        study = new Study(OWNER_ID, "Study", null);
        ReflectionTestUtils.setField(study, "id", STUDY_ID);
    }

    @Test
    void rejectsEnabledQuestionnaireWhenNoDraftExists() {
        when(questionnaireRepository.findByStudyIdForUpdate(STUDY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createSnapshot(study, PUBLISHED_AT))
                .isInstanceOfSatisfying(QuestionnairePublicationException.class, exception ->
                        assertThat(exception.getDetails())
                                .extracting(detail -> detail.code())
                                .containsExactly("QUESTIONNAIRE_REQUIRED"));
    }

    @Test
    void rejectsEmptyQuestionnaire() {
        Questionnaire questionnaire = questionnaire();
        stubQuestionnaire(questionnaire);

        assertThatThrownBy(() -> service.createSnapshot(study, PUBLISHED_AT))
                .isInstanceOfSatisfying(QuestionnairePublicationException.class, exception ->
                        assertThat(exception.getDetails())
                                .extracting(detail -> detail.code())
                                .containsExactly("QUESTIONNAIRE_EMPTY"));
    }

    @Test
    void rejectsMissingQuestionReference() {
        Questionnaire questionnaire = questionnaire(UUID.randomUUID());
        stubQuestionnaire(questionnaire);
        when(questionRepository.findAllByResearcherIdAndIdIn(any(), any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.createSnapshot(study, PUBLISHED_AT))
                .isInstanceOfSatisfying(QuestionnairePublicationException.class, exception ->
                        assertThat(exception.getDetails())
                                .extracting(detail -> detail.code())
                                .containsExactly("MISSING_QUESTION"));
    }

    @Test
    void capturesCompleteStableContentAndBranchTargets() {
        UUID choiceId = UUID.randomUUID();
        UUID textId = UUID.randomUUID();
        UUID yesId = UUID.randomUUID();
        UUID noId = UUID.randomUUID();
        Questionnaire questionnaire = questionnaire(choiceId, textId);
        questionnaire.replaceBranchRules(List.of(
                List.of(new Questionnaire.BranchRulePlacement(yesId, null, 1)),
                List.of()
        ));
        ReflectionTestUtils.setField(
                questionnaire.getItems().get(0).getBranchRules().iterator().next(),
                "id",
                UUID.randomUUID()
        );
        Question choice = choiceQuestion(choiceId, yesId, noId);
        Question text = textQuestion(textId);
        stubQuestionnaire(questionnaire);
        when(questionRepository.findAllByResearcherIdAndIdIn(any(), any()))
                .thenReturn(List.of(choice, text));
        when(snapshotRepository.saveAndFlush(any(QuestionnaireSnapshot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        service.createSnapshot(study, PUBLISHED_AT);

        ArgumentCaptor<QuestionnaireSnapshot> captor = ArgumentCaptor.forClass(QuestionnaireSnapshot.class);
        verify(snapshotRepository).saveAndFlush(captor.capture());
        QuestionnaireSnapshot saved = captor.getValue();
        QuestionnaireSnapshotPayload payload = objectMapper.readValue(
                saved.getContent(),
                QuestionnaireSnapshotPayload.class
        );
        assertThat(saved.getStudyId()).isEqualTo(STUDY_ID);
        assertThat(saved.getPublishedAt()).isEqualTo(PUBLISHED_AT);
        assertThat(payload.items()).hasSize(2);
        assertThat(payload.items().get(0).itemId())
                .isEqualTo(questionnaire.getItems().get(0).getId());
        assertThat(payload.items().get(0).options())
                .extracting(QuestionnaireSnapshotPayload.Option::optionId)
                .containsExactly(yesId, noId);
        assertThat(payload.items().get(0).defaultNextItemId())
                .isEqualTo(questionnaire.getItems().get(1).getId());
        assertThat(payload.items().get(0).branchRules().get(0).targetItemId())
                .isEqualTo(questionnaire.getItems().get(1).getId());
    }

    @Test
    void rejectsBranchTriggerInvalidatedByLaterQuestionBankEdit() {
        UUID choiceId = UUID.randomUUID();
        UUID deletedOptionId = UUID.randomUUID();
        Questionnaire questionnaire = questionnaire(choiceId, UUID.randomUUID());
        questionnaire.replaceBranchRules(List.of(
                List.of(new Questionnaire.BranchRulePlacement(deletedOptionId, null, 1)),
                List.of()
        ));
        Question currentChoice = choiceQuestion(choiceId, UUID.randomUUID(), UUID.randomUUID());
        Question text = textQuestion(questionnaire.getItems().get(1).getQuestionId());
        stubQuestionnaire(questionnaire);
        when(questionRepository.findAllByResearcherIdAndIdIn(any(), any()))
                .thenReturn(List.of(currentChoice, text));

        assertThatThrownBy(() -> service.createSnapshot(study, PUBLISHED_AT))
                .isInstanceOfSatisfying(QuestionnairePublicationException.class, exception ->
                        assertThat(exception.getDetails())
                                .extracting(detail -> detail.code())
                                .containsExactly("INVALID_OPTION_TRIGGER"));
    }

    private void stubQuestionnaire(Questionnaire questionnaire) {
        when(questionnaireRepository.findByStudyIdForUpdate(STUDY_ID))
                .thenReturn(Optional.of(questionnaire));
        when(questionnaireRepository.findByStudyId(STUDY_ID))
                .thenReturn(Optional.of(questionnaire));
    }

    private Questionnaire questionnaire(UUID... questionIds) {
        Questionnaire questionnaire = Questionnaire.create(STUDY_ID);
        for (int index = 0; index < questionIds.length; index++) {
            questionnaire.addItem(questionIds[index], index);
        }
        ReflectionTestUtils.setField(questionnaire, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(questionnaire, "lockVersion", 4L);
        ReflectionTestUtils.setField(
                questionnaire,
                "updatedAt",
                Instant.parse("2026-10-02T05:00:00Z")
        );
        questionnaire.getItems().forEach(item ->
                ReflectionTestUtils.setField(item, "id", UUID.randomUUID()));
        return questionnaire;
    }

    private Question choiceQuestion(UUID questionId, UUID... optionIds) {
        Question question = Question.create(
                OWNER_ID,
                QuestionType.SINGLE_CHOICE,
                "Choose",
                false,
                null,
                null,
                null,
                null
        );
        question.replaceOptions(java.util.stream.IntStream.range(0, optionIds.length)
                .mapToObj(index -> "Option " + index)
                .toList());
        initializeQuestion(question, questionId);
        for (int index = 0; index < optionIds.length; index++) {
            ReflectionTestUtils.setField(question.getOptions().get(index), "id", optionIds[index]);
        }
        return question;
    }

    private Question textQuestion(UUID questionId) {
        Question question = Question.create(
                OWNER_ID,
                QuestionType.TEXT,
                "Explain",
                false,
                null,
                null,
                null,
                null
        );
        initializeQuestion(question, questionId);
        return question;
    }

    private void initializeQuestion(Question question, UUID questionId) {
        ReflectionTestUtils.setField(question, "id", questionId);
        ReflectionTestUtils.setField(question, "createdAt", Instant.parse("2026-10-01T01:00:00Z"));
        ReflectionTestUtils.setField(question, "updatedAt", Instant.parse("2026-10-02T01:00:00Z"));
    }
}
