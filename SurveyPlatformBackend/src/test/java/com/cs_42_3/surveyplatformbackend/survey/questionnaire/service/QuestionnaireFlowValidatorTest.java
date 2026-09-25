package com.cs_42_3.surveyplatformbackend.survey.questionnaire.service;

import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireValidationException;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionnaireFlowValidatorTest {

    private static final UUID RESEARCHER_ID = UUID.fromString(
            "15c66977-6fdb-4e40-b967-c01e65cd155b"
    );

    private final QuestionnaireFlowValidator validator = new QuestionnaireFlowValidator();

    @Test
    void acceptsForwardBranchesWhenEveryItemIsReachableAndAllPathsTerminate() {
        Question source = singleChoice(true, "A", "B");

        assertThatCode(() -> validator.validate(List.of(
                item(source, rule(0, 1), rule(1, 2)),
                item(text(), List.of()),
                item(text(), List.of())
        ))).doesNotThrowAnyException();
    }

    @Test
    void optionalQuestionRetainsTheDefaultNextTransition() {
        Question source = singleChoice(false, "Skip", "Continue");

        assertThatCode(() -> validator.validate(List.of(
                item(source, rule(0, 2)),
                item(text(), List.of()),
                item(text(), List.of())
        ))).doesNotThrowAnyException();
    }

    @Test
    void optionalScaleRetainsNoAnswerDefaultWhenEveryNumericValueHasARule() {
        Question scale = scale(false, 1, 2);

        assertThatCode(() -> validator.validate(List.of(
                item(scale, scaleRule(1, 2), scaleRule(2, 2)),
                item(text(), List.of()),
                item(text(), List.of())
        ))).doesNotThrowAnyException();
    }

    @Test
    void requiredScaleHasNoDefaultWhenEveryNumericValueHasARule() {
        Question scale = scale(true, 1, 2);

        assertThatThrownBy(() -> validator.validate(List.of(
                item(scale, scaleRule(1, 2), scaleRule(2, 2)),
                item(text(), List.of()),
                item(text(), List.of())
        )))
                .isInstanceOf(QuestionnaireValidationException.class)
                .satisfies(exception -> assertThat(
                        ((QuestionnaireValidationException) exception).getDetails()
                ).extracting(detail -> detail.code()).containsExactly("UNREACHABLE_ITEM"));
    }

    @Test
    void rejectsCyclesLongerThanASelfLoop() {
        assertThatThrownBy(() -> validator.validate(List.of(
                item(singleChoice(true, "Next"), rule(0, 1)),
                item(singleChoice(true, "Back"), rule(0, 0))
        )))
                .isInstanceOf(QuestionnaireValidationException.class)
                .satisfies(exception -> assertThat(
                        ((QuestionnaireValidationException) exception).getDetails()
                ).extracting(detail -> detail.code()).containsExactly("BRANCH_CYCLE"));
    }

    @Test
    void rejectsMultiNodeCycles() {
        assertThatThrownBy(() -> validator.validate(List.of(
                item(singleChoice(true, "B"), rule(0, 1)),
                item(singleChoice(true, "C"), rule(0, 2)),
                item(singleChoice(true, "A"), rule(0, 0))
        )))
                .isInstanceOf(QuestionnaireValidationException.class)
                .hasMessageContaining("[0, 1, 2, 0]");
    }

    @Test
    void rejectsAnItemSkippedByEveryPossibleAnswer() {
        assertThatThrownBy(() -> validator.validate(List.of(
                item(singleChoice(true, "Skip"), rule(0, 2)),
                item(text(), List.of()),
                item(text(), List.of())
        )))
                .isInstanceOf(QuestionnaireValidationException.class)
                .satisfies(exception -> assertThat(
                        ((QuestionnaireValidationException) exception).getDetails()
                ).extracting(detail -> detail.code()).containsExactly("UNREACHABLE_ITEM"));
    }

    @Test
    void rejectsRequiredItemWithoutAnyPossibleTransition() {
        assertThatThrownBy(() -> validator.validate(List.of(
                item(singleChoice(true), List.of())
        )))
                .isInstanceOf(QuestionnaireValidationException.class)
                .satisfies(exception -> assertThat(
                        ((QuestionnaireValidationException) exception).getDetails()
                ).extracting(detail -> detail.code()).containsExactly("NON_TERMINATING_ITEM"));
    }

    @Test
    void handlesTheFullIntegerScaleRangeWithoutEnumeratingAnswersOrOverflowing() {
        Question scale = Question.create(
                RESEARCHER_ID,
                QuestionType.SCALE,
                "Any integer",
                false,
                Integer.MIN_VALUE,
                Integer.MAX_VALUE,
                null,
                null
        );

        assertThatCode(() -> validator.validate(List.of(
                item(scale, scaleRule(Integer.MIN_VALUE, 1)),
                item(text(), List.of())
        ))).doesNotThrowAnyException();
    }

    private QuestionnaireFlowValidator.FlowItem item(
            Question question,
            QuestionnaireFlowValidator.FlowRule... rules
    ) {
        return item(question, List.of(rules));
    }

    private QuestionnaireFlowValidator.FlowItem item(
            Question question,
            List<QuestionnaireFlowValidator.FlowRule> rules
    ) {
        return new QuestionnaireFlowValidator.FlowItem(UUID.randomUUID(), question, rules);
    }

    private QuestionnaireFlowValidator.FlowRule rule(int optionIndex, int targetPosition) {
        return new QuestionnaireFlowValidator.FlowRule(
                UUID.nameUUIDFromBytes(("option-" + optionIndex).getBytes()),
                null,
                targetPosition,
                optionIndex
        );
    }

    private QuestionnaireFlowValidator.FlowRule scaleRule(int value, int targetPosition) {
        return new QuestionnaireFlowValidator.FlowRule(null, value, targetPosition, 0);
    }

    private Question singleChoice(boolean required, String... options) {
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
        question.replaceOptions(List.of(options));
        return question;
    }

    private Question text() {
        return Question.create(
                RESEARCHER_ID,
                QuestionType.TEXT,
                "Explain",
                false,
                null,
                null,
                null,
                null
        );
    }

    private Question scale(boolean required, int minimum, int maximum) {
        return Question.create(
                RESEARCHER_ID,
                QuestionType.SCALE,
                "Rate",
                required,
                minimum,
                maximum,
                null,
                null
        );
    }
}
