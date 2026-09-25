package com.cs_42_3.surveyplatformbackend.survey.service.implementation;

import com.cs_42_3.surveyplatformbackend.survey.api.dto.CreateQuestionRequest;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.CreateQuestionOptionRequest;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.UpdateQuestionRequest;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.UpdateQuestionOptionRequest;
import com.cs_42_3.surveyplatformbackend.survey.api.mapper.QuestionResponseMapper;
import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.exception.InvalidQuestionDataException;
import com.cs_42_3.surveyplatformbackend.survey.exception.QuestionNotFoundException;
import com.cs_42_3.surveyplatformbackend.survey.exception.QuestionReferenceConflictException;
import com.cs_42_3.surveyplatformbackend.survey.repository.QuestionRepository;
import com.cs_42_3.surveyplatformbackend.security.CurrentResearcher;
import com.cs_42_3.surveyplatformbackend.survey.service.QuestionnaireQuestionReferencePort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link QuestionServiceImpl} business rules and ownership boundaries.
 */
@ExtendWith(MockitoExtension.class)
class QuestionServiceImplTest {

    private static final UUID RESEARCHER_ID = UUID.fromString("3bb20714-c45d-4cac-b86f-f50e64078678");
    private static final UUID QUESTION_ID = UUID.fromString("6e03ddf3-07ed-4499-99d5-e5566377d489");

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private CurrentResearcher currentResearcher;

    @Mock
    private QuestionnaireQuestionReferencePort questionnaireReferencePort;

    private QuestionServiceImpl questionService;

    @BeforeEach
    void setUp() {
        questionService = new QuestionServiceImpl(
                questionRepository,
                currentResearcher,
                new QuestionResponseMapper(),
                questionnaireReferencePort
        );
        lenient().when(currentResearcher.getId()).thenReturn(RESEARCHER_ID);
        lenient().when(questionnaireReferencePort.lockReferences(any(UUID.class)))
                .thenReturn(new QuestionnaireQuestionReferencePort.ReferenceLock(
                        java.util.Set.of(),
                        java.util.Set.of(),
                        false
                ));
        lenient().when(questionRepository.saveAndFlush(any(Question.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void listQuestionsPassesOwnerTypeAndTrimmedKeywordAndReturnsSummaries() {
        Question question = mock(Question.class);
        when(question.getId()).thenReturn(QUESTION_ID);
        when(question.getType()).thenReturn(QuestionType.TEXT);
        when(question.getQuestionText()).thenReturn("Participant background");
        when(question.getUpdatedAt()).thenReturn(Instant.parse("2026-09-14T06:00:00Z"));
        when(questionRepository.searchQuestions(
                RESEARCHER_ID,
                QuestionType.TEXT,
                "background"
        )).thenReturn(List.of(question));

        var responses = questionService.listQuestions(QuestionType.TEXT, "  background  ");

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).questionText()).isEqualTo("Participant background");
        verify(questionRepository).searchQuestions(
                RESEARCHER_ID,
                QuestionType.TEXT,
                "background"
        );
        verify(question, never()).getOptions();
    }

    @Test
    void listQuestionsConvertsBlankKeywordToNoFilter() {
        when(questionRepository.searchQuestions(RESEARCHER_ID, null, null)).thenReturn(List.of());

        questionService.listQuestions(null, "   ");

        verify(questionRepository).searchQuestions(RESEARCHER_ID, null, null);
    }

    @Test
    void getQuestionReturnsOnlyOwnedQuestion() {
        Question question = textQuestion("Owned question");
        when(questionRepository.findByIdAndResearcherId(QUESTION_ID, RESEARCHER_ID))
                .thenReturn(Optional.of(question));

        var response = questionService.getQuestion(QUESTION_ID);

        assertThat(response.questionText()).isEqualTo("Owned question");
        verify(questionRepository).findByIdAndResearcherId(QUESTION_ID, RESEARCHER_ID);
    }

    @Test
    void getQuestionRejectsMissingOrUnownedQuestion() {
        when(questionRepository.findByIdAndResearcherId(QUESTION_ID, RESEARCHER_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> questionService.getQuestion(QUESTION_ID))
                .isInstanceOf(QuestionNotFoundException.class)
                .hasMessageContaining(QUESTION_ID.toString());
    }

    @Test
    void createSingleChoiceTrimsOptionsAndPreservesRequiredSetting() {
        CreateQuestionRequest request = new CreateQuestionRequest(
                QuestionType.SINGLE_CHOICE,
                "  Choose one  ",
                true,
                List.of(
                        new CreateQuestionOptionRequest("  First  "),
                        new CreateQuestionOptionRequest("Second")
                ),
                1,
                5,
                "Low",
                "High"
        );

        var response = questionService.createQuestion(request);

        assertThat(response.questionText()).isEqualTo("Choose one");
        assertThat(response.required()).isTrue();
        assertThat(response.options())
                .extracting(option -> option.optionText())
                .containsExactly("First", "Second");
        assertThat(response.options())
                .extracting(option -> option.optionOrder())
                .containsExactly(0, 1);
        assertThat(response.scaleMin()).isNull();
        assertThat(response.scaleMax()).isNull();
    }

    @Test
    void createMultiChoiceAcceptsOrderedOptions() {
        CreateQuestionRequest request = new CreateQuestionRequest(
                QuestionType.MULTI_CHOICE,
                "Choose any",
                false,
                List.of(
                        new CreateQuestionOptionRequest("Alpha"),
                        new CreateQuestionOptionRequest("Beta"),
                        new CreateQuestionOptionRequest("Gamma")
                ),
                null,
                null,
                null,
                null
        );

        var response = questionService.createQuestion(request);

        assertThat(response.type()).isEqualTo(QuestionType.MULTI_CHOICE);
        assertThat(response.options())
                .extracting(option -> option.optionOrder())
                .containsExactly(0, 1, 2);
    }

    @Test
    void createScaleClearsOptionsAndTrimsOptionalLabels() {
        CreateQuestionRequest request = new CreateQuestionRequest(
                QuestionType.SCALE,
                "Rate the session",
                true,
                List.of(
                        new CreateQuestionOptionRequest("Ignored A"),
                        new CreateQuestionOptionRequest("Ignored B")
                ),
                1,
                7,
                "  Low  ",
                "  High  "
        );

        var response = questionService.createQuestion(request);

        assertThat(response.options()).isEmpty();
        assertThat(response.scaleMin()).isEqualTo(1);
        assertThat(response.scaleMax()).isEqualTo(7);
        assertThat(response.scaleMinLabel()).isEqualTo("Low");
        assertThat(response.scaleMaxLabel()).isEqualTo("High");
    }

    @Test
    void createTextClearsChoiceAndScaleFields() {
        CreateQuestionRequest request = new CreateQuestionRequest(
                QuestionType.TEXT,
                "Additional feedback",
                false,
                List.of(
                        new CreateQuestionOptionRequest("Ignored A"),
                        new CreateQuestionOptionRequest("Ignored B")
                ),
                1,
                5,
                "Low",
                "High"
        );

        var response = questionService.createQuestion(request);

        assertThat(response.options()).isEmpty();
        assertThat(response.scaleMin()).isNull();
        assertThat(response.scaleMax()).isNull();
        assertThat(response.scaleMinLabel()).isNull();
        assertThat(response.scaleMaxLabel()).isNull();
    }

    @Test
    void createChoiceRejectsFewerThanTwoOptionsWithoutSaving() {
        CreateQuestionRequest request = new CreateQuestionRequest(
                QuestionType.SINGLE_CHOICE,
                "Choose one",
                false,
                List.of(new CreateQuestionOptionRequest("Only option")),
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> questionService.createQuestion(request))
                .isInstanceOf(InvalidQuestionDataException.class)
                .hasMessageContaining("At least two options");
        verify(questionRepository, never()).saveAndFlush(any(Question.class));
    }

    @Test
    void createChoiceRejectsBlankOptionWithoutSaving() {
        CreateQuestionRequest request = new CreateQuestionRequest(
                QuestionType.MULTI_CHOICE,
                "Choose any",
                false,
                List.of(
                        new CreateQuestionOptionRequest("Valid"),
                        new CreateQuestionOptionRequest("   ")
                ),
                null,
                null,
                null,
                null
        );

        assertThatThrownBy(() -> questionService.createQuestion(request))
                .isInstanceOf(InvalidQuestionDataException.class)
                .hasMessageContaining("must not be blank");
        verify(questionRepository, never()).saveAndFlush(any(Question.class));
    }

    @Test
    void createScaleRejectsMissingOrInvalidBoundsWithoutSaving() {
        CreateQuestionRequest missingMaximum = new CreateQuestionRequest(
                QuestionType.SCALE,
                "Rate the session",
                false,
                List.of(),
                1,
                null,
                null,
                null
        );
        CreateQuestionRequest reversedBounds = new CreateQuestionRequest(
                QuestionType.SCALE,
                "Rate the session",
                false,
                List.of(),
                5,
                5,
                null,
                null
        );

        assertThatThrownBy(() -> questionService.createQuestion(missingMaximum))
                .isInstanceOf(InvalidQuestionDataException.class)
                .hasMessageContaining("minimum and maximum are required");
        assertThatThrownBy(() -> questionService.createQuestion(reversedBounds))
                .isInstanceOf(InvalidQuestionDataException.class)
                .hasMessageContaining("less than scale maximum");
        verify(questionRepository, never()).saveAndFlush(any(Question.class));
    }

    @Test
    void updateQuestionReplacesOwnedQuestionFieldsAndOptions() {
        Question question = textQuestion("Old text");
        stubOwnedQuestionForWrite(question);
        UpdateQuestionRequest request = new UpdateQuestionRequest(
                QuestionType.SINGLE_CHOICE,
                "New text",
                true,
                List.of(
                        new UpdateQuestionOptionRequest(null, "Yes"),
                        new UpdateQuestionOptionRequest(null, "No")
                ),
                null,
                null,
                null,
                null,
                false
        );

        var response = questionService.updateQuestion(QUESTION_ID, request);

        assertThat(response.type()).isEqualTo(QuestionType.SINGLE_CHOICE);
        assertThat(response.questionText()).isEqualTo("New text");
        assertThat(response.required()).isTrue();
        assertThat(response.options())
                .extracting(option -> option.optionText())
                .containsExactly("Yes", "No");
        verify(questionRepository).saveAndFlush(question);
    }

    @Test
    void updateQuestionRejectsMissingOrUnownedQuestion() {
        when(questionRepository.findByIdAndResearcherId(QUESTION_ID, RESEARCHER_ID))
                .thenReturn(Optional.empty());
        UpdateQuestionRequest request = new UpdateQuestionRequest(
                QuestionType.TEXT,
                "Updated text",
                false,
                List.of(),
                null,
                null,
                null,
                null,
                false
        );

        assertThatThrownBy(() -> questionService.updateQuestion(QUESTION_ID, request))
                .isInstanceOf(QuestionNotFoundException.class);
        verify(questionRepository, never()).saveAndFlush(any(Question.class));
    }

    @Test
    void updateQuestionPreservesOptionIdsWhileRenamingAndReordering() {
        UUID firstOptionId = UUID.fromString("ae4e3353-1756-44e3-9ec5-a93f3e47633e");
        UUID secondOptionId = UUID.fromString("67166959-d033-4a75-aebb-20cff7acb337");
        Question question = choiceQuestion(firstOptionId, secondOptionId);
        stubOwnedQuestionForWrite(question);

        UpdateQuestionRequest request = new UpdateQuestionRequest(
                QuestionType.SINGLE_CHOICE,
                "Updated choice",
                false,
                List.of(
                        new UpdateQuestionOptionRequest(secondOptionId, "Second renamed"),
                        new UpdateQuestionOptionRequest(firstOptionId, "First")
                ),
                null,
                null,
                null,
                null,
                false
        );

        var response = questionService.updateQuestion(QUESTION_ID, request);

        assertThat(response.options())
                .extracting(option -> option.id())
                .containsExactly(secondOptionId, firstOptionId);
        assertThat(response.options())
                .extracting(option -> option.optionText())
                .containsExactly("Second renamed", "First");
    }

    @Test
    void updateQuestionRequiresExplicitWholeOptionReplacement() {
        Question question = choiceQuestion(UUID.randomUUID(), UUID.randomUUID());
        stubOwnedQuestionForWrite(question);
        UpdateQuestionRequest request = new UpdateQuestionRequest(
                QuestionType.SINGLE_CHOICE,
                "Updated choice",
                false,
                List.of(
                        new UpdateQuestionOptionRequest(null, "Replacement A"),
                        new UpdateQuestionOptionRequest(null, "Replacement B")
                ),
                null,
                null,
                null,
                null,
                false
        );

        assertThatThrownBy(() -> questionService.updateQuestion(QUESTION_ID, request))
                .isInstanceOfSatisfying(InvalidQuestionDataException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("OPTION_IDENTITIES_REQUIRED"));
        verify(questionRepository, never()).saveAndFlush(any(Question.class));
    }

    @Test
    void updateQuestionAllowsExplicitWholeOptionReplacement() {
        Question question = choiceQuestion(UUID.randomUUID(), UUID.randomUUID());
        stubOwnedQuestionForWrite(question);
        UpdateQuestionRequest request = new UpdateQuestionRequest(
                QuestionType.SINGLE_CHOICE,
                "Updated choice",
                false,
                List.of(
                        new UpdateQuestionOptionRequest(null, "Replacement A"),
                        new UpdateQuestionOptionRequest(null, "Replacement B")
                ),
                null,
                null,
                null,
                null,
                true
        );

        var response = questionService.updateQuestion(QUESTION_ID, request);

        assertThat(response.options())
                .extracting(option -> option.optionText())
                .containsExactly("Replacement A", "Replacement B");
    }

    @Test
    void updateQuestionRejectsOptionIdFromAnotherQuestion() {
        Question question = choiceQuestion(UUID.randomUUID(), UUID.randomUUID());
        stubOwnedQuestionForWrite(question);
        UpdateQuestionRequest request = new UpdateQuestionRequest(
                QuestionType.SINGLE_CHOICE,
                "Updated choice",
                false,
                List.of(
                        new UpdateQuestionOptionRequest(UUID.randomUUID(), "Foreign"),
                        new UpdateQuestionOptionRequest(null, "New")
                ),
                null,
                null,
                null,
                null,
                false
        );

        assertThatThrownBy(() -> questionService.updateQuestion(QUESTION_ID, request))
                .isInstanceOfSatisfying(InvalidQuestionDataException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo("INVALID_OPTION_REFERENCE"));
    }

    @Test
    void updateQuestionDoesNotMutateOrSaveWhenQuestionnaireReferencesRejectIt() {
        Question question = textQuestion("Original text");
        stubOwnedQuestionForWrite(question);
        doThrow(new QuestionReferenceConflictException(
                "QUESTION_UPDATE_INVALIDATES_FLOW",
                "Flow would become invalid"
        )).when(questionnaireReferencePort).validateUpdate(any(), any(), any());
        UpdateQuestionRequest request = new UpdateQuestionRequest(
                QuestionType.TEXT,
                "Changed text",
                false,
                List.of(),
                null,
                null,
                null,
                null,
                false
        );

        assertThatThrownBy(() -> questionService.updateQuestion(QUESTION_ID, request))
                .isInstanceOf(QuestionReferenceConflictException.class);

        assertThat(question.getQuestionText()).isEqualTo("Original text");
        verify(questionRepository, never()).saveAndFlush(any(Question.class));
    }

    @Test
    void deleteQuestionDeletesOnlyOwnedQuestion() {
        Question question = textQuestion("Delete me");
        stubOwnedQuestionForWrite(question);

        questionService.deleteQuestion(QUESTION_ID);

        verify(questionRepository).delete(question);
    }

    @Test
    void createQuestionUsesResearcherIdFromProvider() {
        CreateQuestionRequest request = new CreateQuestionRequest(
                QuestionType.TEXT,
                "Identity check",
                false,
                List.of(),
                null,
                null,
                null,
                null
        );
        ArgumentCaptor<Question> questionCaptor = ArgumentCaptor.forClass(Question.class);

        questionService.createQuestion(request);

        verify(questionRepository).saveAndFlush(questionCaptor.capture());
        assertThat(questionCaptor.getValue().getResearcherId()).isEqualTo(RESEARCHER_ID);
    }

    private Question textQuestion(String questionText) {
        return Question.create(
                RESEARCHER_ID,
                QuestionType.TEXT,
                questionText,
                false,
                null,
                null,
                null,
                null
        );
    }

    private Question choiceQuestion(UUID firstOptionId, UUID secondOptionId) {
        Question question = Question.create(
                RESEARCHER_ID,
                QuestionType.SINGLE_CHOICE,
                "Existing choice",
                false,
                null,
                null,
                null,
                null
        );
        question.replaceOptions(List.of("First", "Second"));
        ReflectionTestUtils.setField(question.getOptions().get(0), "id", firstOptionId);
        ReflectionTestUtils.setField(question.getOptions().get(1), "id", secondOptionId);
        return question;
    }

    private void stubOwnedQuestionForWrite(Question question) {
        ReflectionTestUtils.setField(question, "id", QUESTION_ID);
        when(questionRepository.findByIdAndResearcherId(QUESTION_ID, RESEARCHER_ID))
                .thenReturn(Optional.of(question));
        when(questionRepository.findOwnedByIdForUpdate(QUESTION_ID, RESEARCHER_ID))
                .thenReturn(Optional.of(question));
    }
}
