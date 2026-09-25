package com.cs_42_3.surveyplatformbackend.survey.questionnaire.service.implementation;

import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.cs_42_3.surveyplatformbackend.security.CurrentResearcher;
import com.cs_42_3.surveyplatformbackend.survey.api.mapper.QuestionResponseMapper;
import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.QuestionnaireBranchRuleRequest;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.SaveQuestionnaireItemRequest;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.SaveQuestionnaireRequest;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.domain.Questionnaire;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.InvalidQuestionReferenceException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.InvalidQuestionnaireItemReferenceException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireLockedException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireValidationException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireVersionConflictException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.repository.QuestionnaireRepository;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.service.QuestionnaireFlowValidator;
import com.cs_42_3.surveyplatformbackend.survey.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Unit coverage for the questionnaire ownership, version, and idempotency protocol. */
@ExtendWith(MockitoExtension.class)
class QuestionnaireServiceImplTest {

    private static final UUID RESEARCHER_ID = UUID.fromString("15c66977-6fdb-4e40-b967-c01e65cd155b");
    private static final UUID STUDY_ID = UUID.fromString("95973010-b93c-4735-8dbf-e476382c114c");
    private static final UUID QUESTION_ONE_ID = UUID.fromString("8873e071-96b5-411e-b06f-9ca3bb9863a9");
    private static final UUID QUESTION_TWO_ID = UUID.fromString("382779e0-2f12-4996-a51c-180cedf796ac");

    @Mock
    private QuestionnaireRepository questionnaireRepository;
    @Mock
    private StudyRepository studyRepository;
    @Mock
    private QuestionRepository questionRepository;
    @Mock
    private CurrentResearcher currentResearcher;

    private final Map<UUID, Question> questionBank = new LinkedHashMap<>();
    private QuestionnaireServiceImpl service;
    private Study draftStudy;

    @BeforeEach
    void setUp() {
        service = new QuestionnaireServiceImpl(
                questionnaireRepository,
                studyRepository,
                questionRepository,
                currentResearcher,
                new QuestionResponseMapper(),
                new QuestionnaireFlowValidator()
        );
        draftStudy = new Study(RESEARCHER_ID, "Owned study", null);
        questionBank.put(QUESTION_ONE_ID, question(QUESTION_ONE_ID, "First"));
        questionBank.put(QUESTION_TWO_ID, question(QUESTION_TWO_ID, "Second"));

        lenient().when(currentResearcher.getId()).thenReturn(RESEARCHER_ID);
        lenient().when(studyRepository.findOwnedStudyForQuestionnaireUpdate(STUDY_ID, RESEARCHER_ID))
                .thenReturn(Optional.of(draftStudy));
        lenient().when(questionRepository.lockAllOwnedByIdsForUpdate(
                        eq(RESEARCHER_ID),
                        anyCollection()
                ))
                .thenAnswer(invocation -> {
                    Collection<UUID> ids = invocation.getArgument(1);
                    return ids.stream()
                            .filter(questionBank::containsKey)
                            .map(questionBank::get)
                            .toList();
                });
        lenient().when(questionRepository.findAllByResearcherIdAndIdIn(
                        eq(RESEARCHER_ID),
                        anyCollection()
                ))
                .thenAnswer(invocation -> {
                    Collection<UUID> ids = invocation.getArgument(1);
                    return ids.stream()
                            .filter(questionBank::containsKey)
                            .map(questionBank::get)
                            .toList();
                });
        lenient().when(questionnaireRepository.saveAndFlush(any(Questionnaire.class)))
                .thenAnswer(invocation -> simulateFlush(invocation.getArgument(0)));
    }

    @Test
    void createsQuestionnaireAndAllowsSeveralNewItemsWithNullItemIds() {
        when(questionnaireRepository.findByStudyIdForUpdate(STUDY_ID)).thenReturn(Optional.empty());
        SaveQuestionnaireRequest request = request(
                null,
                item(null, QUESTION_ONE_ID),
                item(null, QUESTION_TWO_ID)
        );

        var result = service.saveQuestionnaire(STUDY_ID, request);

        assertThat(result.created()).isTrue();
        assertThat(result.response().version()).isZero();
        assertThat(result.response().items())
                .extracting(item -> item.position())
                .containsExactly(0, 1);
        assertThat(result.response().items())
                .extracting(item -> item.itemId())
                .doesNotContainNull();
        verify(studyRepository).findOwnedStudyForQuestionnaireUpdate(STUDY_ID, RESEARCHER_ID);
    }

    @Test
    void rejectsAllDuplicateItemAndQuestionPositionsBeforeTakingTheStudyLock() {
        UUID duplicateItemId = UUID.randomUUID();
        SaveQuestionnaireRequest request = request(
                null,
                item(duplicateItemId, QUESTION_ONE_ID),
                item(duplicateItemId, QUESTION_ONE_ID)
        );

        assertThatThrownBy(() -> service.saveQuestionnaire(STUDY_ID, request))
                .isInstanceOf(QuestionnaireValidationException.class)
                .satisfies(exception -> assertThat(
                        ((QuestionnaireValidationException) exception).getDetails()
                ).extracting(detail -> detail.code())
                        .containsExactlyInAnyOrder(
                                "DUPLICATE_ITEM_ID",
                                "DUPLICATE_ITEM_ID",
                                "DUPLICATE_QUESTION_REFERENCE",
                                "DUPLICATE_QUESTION_REFERENCE"
                        ));
        verify(studyRepository, never()).findOwnedStudyForQuestionnaireUpdate(any(), any());
    }

    @Test
    void rejectsExpectedVersionForMissingQuestionnaireBeforeQuestionLookup() {
        when(questionnaireRepository.findByStudyIdForUpdate(STUDY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveQuestionnaire(
                STUDY_ID,
                request(0L, item(null, QUESTION_ONE_ID))
        )).isInstanceOf(QuestionnaireVersionConflictException.class);
        verify(questionRepository, never()).lockAllOwnedByIdsForUpdate(any(), anyCollection());
    }

    @Test
    void rejectsFutureVersionBeforeQuestionLookup() {
        Questionnaire questionnaire = existingQuestionnaire(2L, QUESTION_ONE_ID);
        when(questionnaireRepository.findByStudyIdForUpdate(STUDY_ID)).thenReturn(Optional.of(questionnaire));

        assertThatThrownBy(() -> service.saveQuestionnaire(
                STUDY_ID,
                request(3L, item(questionnaire.getItems().get(0).getId(), QUESTION_ONE_ID))
        )).isInstanceOf(QuestionnaireVersionConflictException.class);
        verify(questionRepository, never()).lockAllOwnedByIdsForUpdate(any(), anyCollection());
    }

    @Test
    void sameCurrentVersionUpdatesChangedContentAndPreservesExistingItemId() {
        Questionnaire questionnaire = existingQuestionnaire(2L, QUESTION_ONE_ID);
        UUID existingItemId = questionnaire.getItems().get(0).getId();
        when(questionnaireRepository.findByStudyIdForUpdate(STUDY_ID)).thenReturn(Optional.of(questionnaire));

        var result = service.saveQuestionnaire(
                STUDY_ID,
                request(2L, item(existingItemId, QUESTION_TWO_ID))
        );

        assertThat(result.created()).isFalse();
        assertThat(result.response().version()).isEqualTo(3L);
        assertThat(result.response().items().get(0).itemId()).isEqualTo(existingItemId);
        assertThat(result.response().items().get(0).question().id()).isEqualTo(QUESTION_TWO_ID);
        verify(questionnaireRepository).saveAndFlush(questionnaire);
    }

    @Test
    void reorderAndRemovalKeepTheRetainedItemIdAndContinuousPositions() {
        Questionnaire questionnaire = existingQuestionnaire(
                5L,
                QUESTION_ONE_ID,
                QUESTION_TWO_ID
        );
        UUID removedItemId = questionnaire.getItems().get(0).getId();
        UUID retainedItemId = questionnaire.getItems().get(1).getId();
        when(questionnaireRepository.findByStudyIdForUpdate(STUDY_ID)).thenReturn(Optional.of(questionnaire));

        var result = service.saveQuestionnaire(
                STUDY_ID,
                request(5L, item(retainedItemId, QUESTION_TWO_ID))
        );

        assertThat(result.response().items()).hasSize(1);
        assertThat(result.response().items().get(0).itemId()).isEqualTo(retainedItemId);
        assertThat(result.response().items().get(0).itemId()).isNotEqualTo(removedItemId);
        assertThat(result.response().items().get(0).position()).isZero();
    }

    @Test
    void identicalCurrentVersionRequestDoesNotWriteOrAdvanceVersion() {
        Questionnaire questionnaire = existingQuestionnaire(2L, QUESTION_ONE_ID);
        Instant previousUpdatedAt = questionnaire.getUpdatedAt();
        when(questionnaireRepository.findByStudyIdForUpdate(STUDY_ID)).thenReturn(Optional.of(questionnaire));

        var result = service.saveQuestionnaire(
                STUDY_ID,
                request(2L, item(questionnaire.getItems().get(0).getId(), QUESTION_ONE_ID))
        );

        assertThat(result.response().version()).isEqualTo(2L);
        assertThat(result.response().updatedAt()).isEqualTo(previousUpdatedAt);
        verify(questionnaireRepository, never()).saveAndFlush(any());
    }

    @Test
    void olderVersionIsAcceptedOnlyForIdenticalSafeRetry() {
        Questionnaire questionnaire = existingQuestionnaire(2L, QUESTION_ONE_ID);
        UUID itemId = questionnaire.getItems().get(0).getId();
        when(questionnaireRepository.findByStudyIdForUpdate(STUDY_ID)).thenReturn(Optional.of(questionnaire));

        var retry = service.saveQuestionnaire(
                STUDY_ID,
                request(1L, item(itemId, QUESTION_ONE_ID))
        );
        assertThat(retry.response().version()).isEqualTo(2L);
        verify(questionnaireRepository, never()).saveAndFlush(any());

        assertThatThrownBy(() -> service.saveQuestionnaire(
                STUDY_ID,
                request(1L, item(itemId, QUESTION_TWO_ID))
        )).isInstanceOf(QuestionnaireVersionConflictException.class);
    }

    @Test
    void nullVersionReplaysInitialCreationOnlyWhenContentMatches() {
        Questionnaire questionnaire = existingQuestionnaire(0L, QUESTION_ONE_ID);
        when(questionnaireRepository.findByStudyIdForUpdate(STUDY_ID)).thenReturn(Optional.of(questionnaire));

        var retry = service.saveQuestionnaire(
                STUDY_ID,
                request(null, item(null, QUESTION_ONE_ID))
        );
        assertThat(retry.created()).isFalse();
        assertThat(retry.response().items().get(0).itemId())
                .isEqualTo(questionnaire.getItems().get(0).getId());

        assertThatThrownBy(() -> service.saveQuestionnaire(
                STUDY_ID,
                request(null, item(null, QUESTION_TWO_ID))
        )).isInstanceOf(QuestionnaireVersionConflictException.class);
    }

    @Test
    void invalidQuestionIsReportedWithoutLeakingWhetherItExistsElsewhere() {
        UUID unavailableQuestionId = UUID.randomUUID();
        when(questionnaireRepository.findByStudyIdForUpdate(STUDY_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.saveQuestionnaire(
                STUDY_ID,
                request(null, item(null, unavailableQuestionId))
        )).isInstanceOf(InvalidQuestionReferenceException.class)
                .satisfies(exception -> assertThat(
                        ((InvalidQuestionReferenceException) exception).getDetails().get(0).code()
                ).isEqualTo("INVALID_QUESTION_REFERENCE"));
        verify(questionnaireRepository, never()).saveAndFlush(any());
    }

    @Test
    void itemFromAnotherQuestionnaireIsRejected() {
        Questionnaire questionnaire = existingQuestionnaire(0L, QUESTION_ONE_ID);
        when(questionnaireRepository.findByStudyIdForUpdate(STUDY_ID)).thenReturn(Optional.of(questionnaire));

        assertThatThrownBy(() -> service.saveQuestionnaire(
                STUDY_ID,
                request(0L, item(UUID.randomUUID(), QUESTION_ONE_ID))
        )).isInstanceOf(InvalidQuestionnaireItemReferenceException.class);
    }

    @Test
    void nonDraftStudyIsLockedBeforeQuestionnaireOrQuestionQueries() {
        ReflectionTestUtils.setField(draftStudy, "status", StudyStatus.COLLECTING);

        assertThatThrownBy(() -> service.saveQuestionnaire(
                STUDY_ID,
                request(null, item(null, QUESTION_ONE_ID))
        )).isInstanceOf(QuestionnaireLockedException.class);
        verify(questionnaireRepository, never()).findByStudyIdForUpdate(any());
        verify(questionRepository, never()).lockAllOwnedByIdsForUpdate(any(), anyCollection());
    }

    @Test
    void getMapsDeletedQuestionReferenceToMissingItem() {
        Questionnaire questionnaire = existingQuestionnaire(4L, QUESTION_ONE_ID);
        ReflectionTestUtils.setField(questionnaire.getItems().get(0), "questionId", null);
        when(studyRepository.findByIdAndOwnerId(STUDY_ID, RESEARCHER_ID))
                .thenReturn(Optional.of(draftStudy));
        when(questionnaireRepository.findByStudyId(STUDY_ID)).thenReturn(Optional.of(questionnaire));

        var response = service.getQuestionnaire(STUDY_ID);

        assertThat(response.items().get(0).missing()).isTrue();
        assertThat(response.items().get(0).question()).isNull();
        verify(questionRepository, never()).findAllByResearcherIdAndIdIn(any(), anyCollection());
    }

    @Test
    void getKeepsIncomingRuleAndReportsMissingTargetWithRuleIndex() {
        Questionnaire questionnaire = existingQuestionnaire(
                4L,
                QUESTION_ONE_ID,
                QUESTION_TWO_ID
        );
        questionnaire.replaceBranchRules(List.of(
                List.of(new Questionnaire.BranchRulePlacement(UUID.randomUUID(), null, 1)),
                List.of()
        ));
        UUID targetItemId = questionnaire.getItems().get(1).getId();
        ReflectionTestUtils.setField(questionnaire.getItems().get(1), "questionId", null);
        when(studyRepository.findByIdAndOwnerId(STUDY_ID, RESEARCHER_ID))
                .thenReturn(Optional.of(draftStudy));
        when(questionnaireRepository.findByStudyId(STUDY_ID)).thenReturn(Optional.of(questionnaire));

        var response = service.getQuestionnaire(STUDY_ID);

        assertThat(response.valid()).isFalse();
        assertThat(response.items().get(0).branchRules()).singleElement()
                .satisfies(rule -> {
                    assertThat(rule.targetItemId()).isEqualTo(targetItemId);
                    assertThat(rule.targetPosition()).isEqualTo(1);
                });
        assertThat(response.validationIssues())
                .filteredOn(issue -> issue.code().equals("BRANCH_TARGET_MISSING_QUESTION"))
                .singleElement()
                .satisfies(issue -> {
                    assertThat(issue.itemIndex()).isZero();
                    assertThat(issue.ruleIndex()).isZero();
                });
    }

    @Test
    void missingItemCanBeReplacedOrRemovedUsingItsStableItemId() {
        Questionnaire questionnaire = existingQuestionnaire(4L, QUESTION_ONE_ID);
        UUID missingItemId = questionnaire.getItems().get(0).getId();
        ReflectionTestUtils.setField(questionnaire.getItems().get(0), "questionId", null);
        when(questionnaireRepository.findByStudyIdForUpdate(STUDY_ID)).thenReturn(Optional.of(questionnaire));

        var replaced = service.saveQuestionnaire(
                STUDY_ID,
                request(4L, item(missingItemId, QUESTION_TWO_ID))
        );
        assertThat(replaced.response().items().get(0).itemId()).isEqualTo(missingItemId);
        assertThat(replaced.response().items().get(0).missing()).isFalse();

        var removed = service.saveQuestionnaire(
                STUDY_ID,
                request(5L)
        );
        assertThat(removed.response().items()).isEmpty();
    }

    @Test
    void savesChoiceBranchAndReturnsItsStableTargetItemIdentity() {
        UUID choiceId = UUID.randomUUID();
        UUID firstOptionId = UUID.randomUUID();
        questionBank.put(
                choiceId,
                singleChoiceQuestion(choiceId, false, firstOptionId, UUID.randomUUID())
        );
        when(questionnaireRepository.findByStudyIdForUpdate(STUDY_ID)).thenReturn(Optional.empty());

        var result = service.saveQuestionnaire(
                STUDY_ID,
                request(
                        null,
                        item(
                                null,
                                choiceId,
                                new QuestionnaireBranchRuleRequest(firstOptionId, null, 2)
                        ),
                        item(null, QUESTION_ONE_ID),
                        item(null, QUESTION_TWO_ID)
                )
        );

        assertThat(result.response().valid()).isTrue();
        assertThat(result.response().validationIssues()).isEmpty();
        assertThat(result.response().items().get(0).branchRules()).singleElement()
                .satisfies(rule -> {
                    assertThat(rule.sourceOptionId()).isEqualTo(firstOptionId);
                    assertThat(rule.sourceScaleValue()).isNull();
                    assertThat(rule.targetPosition()).isEqualTo(2);
                    assertThat(rule.targetItemId())
                            .isEqualTo(result.response().items().get(2).itemId());
                });
    }

    @Test
    void identicalBranchRulesAreASafeRetryEvenWithAnOlderVersion() {
        UUID choiceId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        questionBank.put(choiceId, singleChoiceQuestion(choiceId, false, optionId, UUID.randomUUID()));
        Questionnaire questionnaire = existingQuestionnaire(
                2L,
                choiceId,
                QUESTION_ONE_ID
        );
        questionnaire.replaceBranchRules(List.of(
                List.of(new Questionnaire.BranchRulePlacement(optionId, null, 1)),
                List.of()
        ));
        when(questionnaireRepository.findByStudyIdForUpdate(STUDY_ID))
                .thenReturn(Optional.of(questionnaire));

        var result = service.saveQuestionnaire(
                STUDY_ID,
                request(
                        1L,
                        item(
                                questionnaire.getItems().get(0).getId(),
                                choiceId,
                                new QuestionnaireBranchRuleRequest(optionId, null, 1)
                        ),
                        item(questionnaire.getItems().get(1).getId(), QUESTION_ONE_ID)
                )
        );

        assertThat(result.response().version()).isEqualTo(2L);
        verify(questionnaireRepository, never()).saveAndFlush(any());
        verify(questionnaireRepository, never()).flush();
    }

    @Test
    void changedRulesReplaceTheWholeManagedRuleSet() {
        UUID choiceId = UUID.randomUUID();
        UUID oldOptionId = UUID.randomUUID();
        UUID newOptionId = UUID.randomUUID();
        questionBank.put(
                choiceId,
                singleChoiceQuestion(choiceId, false, oldOptionId, newOptionId)
        );
        Questionnaire questionnaire = existingQuestionnaire(
                2L,
                choiceId,
                QUESTION_ONE_ID,
                QUESTION_TWO_ID
        );
        questionnaire.replaceBranchRules(List.of(
                List.of(new Questionnaire.BranchRulePlacement(oldOptionId, null, 2)),
                List.of(),
                List.of()
        ));
        when(questionnaireRepository.findByStudyIdForUpdate(STUDY_ID))
                .thenReturn(Optional.of(questionnaire));

        service.saveQuestionnaire(
                STUDY_ID,
                request(
                        2L,
                        item(
                                questionnaire.getItems().get(0).getId(),
                                choiceId,
                                new QuestionnaireBranchRuleRequest(newOptionId, null, 2)
                        ),
                        item(questionnaire.getItems().get(1).getId(), QUESTION_ONE_ID),
                        item(questionnaire.getItems().get(2).getId(), QUESTION_TWO_ID)
                )
        );

        assertThat(questionnaire.getItems().get(0).getBranchRules()).singleElement()
                .satisfies(rule -> assertThat(rule.getSourceOptionId()).isEqualTo(newOptionId));
        verify(questionnaireRepository).flush();
        verify(questionnaireRepository).saveAndFlush(questionnaire);
    }

    @Test
    void invalidReplacementLeavesExistingRulesUntouched() {
        UUID choiceId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        questionBank.put(choiceId, singleChoiceQuestion(choiceId, false, optionId, UUID.randomUUID()));
        Questionnaire questionnaire = existingQuestionnaire(
                2L,
                choiceId,
                QUESTION_ONE_ID
        );
        questionnaire.replaceBranchRules(List.of(
                List.of(new Questionnaire.BranchRulePlacement(optionId, null, 1)),
                List.of()
        ));
        when(questionnaireRepository.findByStudyIdForUpdate(STUDY_ID))
                .thenReturn(Optional.of(questionnaire));

        assertRuleError(
                request(
                        2L,
                        item(
                                questionnaire.getItems().get(0).getId(),
                                choiceId,
                                new QuestionnaireBranchRuleRequest(optionId, null, 9)
                        ),
                        item(questionnaire.getItems().get(1).getId(), QUESTION_ONE_ID)
                ),
                "BRANCH_TARGET_OUT_OF_RANGE",
                0
        );

        assertThat(questionnaire.getItems().get(0).getBranchRules()).singleElement()
                .satisfies(rule -> assertThat(rule.getSourceOptionId()).isEqualTo(optionId));
        verify(questionnaireRepository, never()).flush();
        verify(questionnaireRepository, never()).saveAndFlush(any());
    }

    @Test
    void savesScaleBranchRules() {
        UUID scaleId = UUID.randomUUID();
        questionBank.put(scaleId, scaleQuestion(scaleId, false, 1, 5));
        when(questionnaireRepository.findByStudyIdForUpdate(STUDY_ID)).thenReturn(Optional.empty());

        var result = service.saveQuestionnaire(
                STUDY_ID,
                request(
                        null,
                        item(
                                null,
                                scaleId,
                                new QuestionnaireBranchRuleRequest(null, 5, 1)
                        ),
                        item(null, QUESTION_ONE_ID)
                )
        );

        assertThat(result.response().items().get(0).branchRules()).singleElement()
                .satisfies(rule -> {
                    assertThat(rule.sourceOptionId()).isNull();
                    assertThat(rule.sourceScaleValue()).isEqualTo(5);
                    assertThat(rule.targetPosition()).isEqualTo(1);
                });
    }

    @Test
    void rejectsBranchRulesForUnsupportedQuestionTypes() {
        when(questionnaireRepository.findByStudyIdForUpdate(STUDY_ID)).thenReturn(Optional.empty());

        assertRuleError(
                request(
                        null,
                        item(
                                null,
                                QUESTION_ONE_ID,
                                new QuestionnaireBranchRuleRequest(null, 1, 1)
                        ),
                        item(null, QUESTION_TWO_ID)
                ),
                "BRANCH_TYPE_UNSUPPORTED",
                0
        );
    }

    @Test
    void requiresExactlyOneTriggerPerBranchRule() {
        UUID choiceId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        questionBank.put(choiceId, singleChoiceQuestion(choiceId, false, optionId));
        when(questionnaireRepository.findByStudyIdForUpdate(STUDY_ID)).thenReturn(Optional.empty());

        assertRuleError(
                request(
                        null,
                        item(
                                null,
                                choiceId,
                                new QuestionnaireBranchRuleRequest(optionId, 1, 1)
                        ),
                        item(null, QUESTION_ONE_ID)
                ),
                "BRANCH_TRIGGER_REQUIRED",
                0
        );
        assertRuleError(
                request(
                        null,
                        item(
                                null,
                                choiceId,
                                new QuestionnaireBranchRuleRequest(null, null, 1)
                        ),
                        item(null, QUESTION_ONE_ID)
                ),
                "BRANCH_TRIGGER_REQUIRED",
                0
        );
    }

    @Test
    void validatesChoiceOptionOwnershipAndScaleBounds() {
        UUID choiceId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        UUID scaleId = UUID.randomUUID();
        questionBank.put(choiceId, singleChoiceQuestion(choiceId, false, optionId));
        questionBank.put(scaleId, scaleQuestion(scaleId, false, 1, 5));
        when(questionnaireRepository.findByStudyIdForUpdate(STUDY_ID)).thenReturn(Optional.empty());

        assertRuleError(
                request(
                        null,
                        item(
                                null,
                                choiceId,
                                new QuestionnaireBranchRuleRequest(UUID.randomUUID(), null, 1)
                        ),
                        item(null, QUESTION_ONE_ID)
                ),
                "INVALID_OPTION_TRIGGER",
                0
        );
        assertRuleError(
                request(
                        null,
                        item(
                                null,
                                scaleId,
                                new QuestionnaireBranchRuleRequest(null, 6, 1)
                        ),
                        item(null, QUESTION_ONE_ID)
                ),
                "SCALE_TRIGGER_OUT_OF_RANGE",
                0
        );
    }

    @Test
    void rejectsDuplicateTriggersInvalidTargetsAndSelfLoops() {
        UUID choiceId = UUID.randomUUID();
        UUID optionId = UUID.randomUUID();
        questionBank.put(choiceId, singleChoiceQuestion(choiceId, false, optionId));
        when(questionnaireRepository.findByStudyIdForUpdate(STUDY_ID)).thenReturn(Optional.empty());

        assertRuleError(
                request(
                        null,
                        item(
                                null,
                                choiceId,
                                new QuestionnaireBranchRuleRequest(optionId, null, 1),
                                new QuestionnaireBranchRuleRequest(optionId, null, 1)
                        ),
                        item(null, QUESTION_ONE_ID)
                ),
                "DUPLICATE_BRANCH_TRIGGER",
                1
        );
        assertRuleError(
                request(
                        null,
                        item(
                                null,
                                choiceId,
                                new QuestionnaireBranchRuleRequest(optionId, null, 2)
                        ),
                        item(null, QUESTION_ONE_ID)
                ),
                "BRANCH_TARGET_OUT_OF_RANGE",
                0
        );
        assertRuleError(
                request(
                        null,
                        item(
                                null,
                                choiceId,
                                new QuestionnaireBranchRuleRequest(optionId, null, 0)
                        ),
                        item(null, QUESTION_ONE_ID)
                ),
                "BRANCH_SELF_LOOP",
                0
        );
    }

    private Questionnaire simulateFlush(Questionnaire questionnaire) {
        boolean created = questionnaire.getId() == null;
        if (created) {
            ReflectionTestUtils.setField(questionnaire, "id", UUID.randomUUID());
            ReflectionTestUtils.setField(questionnaire, "lockVersion", 0L);
        } else {
            ReflectionTestUtils.setField(
                    questionnaire,
                    "lockVersion",
                    questionnaire.getLockVersion() + 1
            );
        }
        if (questionnaire.getUpdatedAt() == null) {
            ReflectionTestUtils.setField(questionnaire, "updatedAt", Instant.parse("2026-09-21T10:00:00Z"));
        }
        questionnaire.getItems().stream()
                .filter(item -> item.getId() == null)
                .forEach(item -> ReflectionTestUtils.setField(item, "id", UUID.randomUUID()));
        return questionnaire;
    }

    private Questionnaire existingQuestionnaire(long version, UUID... questionIds) {
        Questionnaire questionnaire = Questionnaire.create(STUDY_ID);
        List<Questionnaire.ItemPlacement> placements = new ArrayList<>();
        for (UUID questionId : questionIds) {
            placements.add(new Questionnaire.ItemPlacement(null, questionId));
        }
        questionnaire.synchronizeItems(placements);
        ReflectionTestUtils.setField(questionnaire, "id", UUID.randomUUID());
        ReflectionTestUtils.setField(questionnaire, "lockVersion", version);
        ReflectionTestUtils.setField(questionnaire, "updatedAt", Instant.parse("2026-09-21T09:00:00Z"));
        questionnaire.getItems().forEach(item ->
                ReflectionTestUtils.setField(item, "id", UUID.randomUUID()));
        return questionnaire;
    }

    private Question question(UUID id, String text) {
        Question question = Question.create(
                RESEARCHER_ID,
                QuestionType.TEXT,
                text,
                false,
                null,
                null,
                null,
                null
        );
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }

    private Question singleChoiceQuestion(
            UUID id,
            boolean required,
            UUID... optionIds
    ) {
        Question question = Question.create(
                RESEARCHER_ID,
                QuestionType.SINGLE_CHOICE,
                "Choose",
                required,
                null,
                null,
                null,
                null
        );
        question.replaceOptions(java.util.stream.IntStream.range(0, optionIds.length)
                .mapToObj(index -> "Option " + index)
                .toList());
        ReflectionTestUtils.setField(question, "id", id);
        for (int index = 0; index < optionIds.length; index++) {
            ReflectionTestUtils.setField(question.getOptions().get(index), "id", optionIds[index]);
        }
        return question;
    }

    private Question scaleQuestion(
            UUID id,
            boolean required,
            int minimum,
            int maximum
    ) {
        Question question = Question.create(
                RESEARCHER_ID,
                QuestionType.SCALE,
                "Rate",
                required,
                minimum,
                maximum,
                null,
                null
        );
        ReflectionTestUtils.setField(question, "id", id);
        return question;
    }

    private SaveQuestionnaireRequest request(
            Long version,
            SaveQuestionnaireItemRequest... items
    ) {
        return new SaveQuestionnaireRequest(version, List.of(items));
    }

    private SaveQuestionnaireItemRequest item(UUID itemId, UUID questionId) {
        return new SaveQuestionnaireItemRequest(itemId, questionId);
    }

    private SaveQuestionnaireItemRequest item(
            UUID itemId,
            UUID questionId,
            QuestionnaireBranchRuleRequest... branchRules
    ) {
        return new SaveQuestionnaireItemRequest(itemId, questionId, List.of(branchRules));
    }

    private void assertRuleError(
            SaveQuestionnaireRequest request,
            String expectedCode,
            int expectedRuleIndex
    ) {
        assertThatThrownBy(() -> service.saveQuestionnaire(STUDY_ID, request))
                .isInstanceOf(QuestionnaireValidationException.class)
                .satisfies(exception -> assertThat(
                        ((QuestionnaireValidationException) exception).getDetails()
                ).singleElement().satisfies(detail -> {
                    assertThat(detail.code()).isEqualTo(expectedCode);
                    assertThat(detail.index()).isZero();
                    assertThat(detail.ruleIndex()).isEqualTo(expectedRuleIndex);
                    assertThat(detail.field()).startsWith("items[0].branchRules");
                }));
    }
}
