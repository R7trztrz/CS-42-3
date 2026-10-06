package com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.service;

import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.domain.QuestionnaireSnapshotPayload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionnaireBranchResolverTest {

    private final QuestionnaireBranchResolver resolver = new QuestionnaireBranchResolver();

    @Test
    void choiceMatchUsesConditionalTargetAndMissUsesDefault() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UUID third = UUID.randomUUID();
        UUID option = UUID.randomUUID();
        QuestionnaireSnapshotPayload snapshot = snapshot(List.of(
                item(first, 0, QuestionType.SINGLE_CHOICE, second,
                        List.of(new QuestionnaireSnapshotPayload.Option(option, "Yes", 0)),
                        List.of(new QuestionnaireSnapshotPayload.BranchRule(
                                UUID.randomUUID(), option, null, third))),
                item(second, 1, QuestionType.TEXT, third, List.of(), List.of()),
                item(third, 2, QuestionType.TEXT, null, List.of(), List.of())
        ));

        assertThat(resolver.resolve(
                snapshot,
                first,
                StandardizedQuestionnaireAnswer.singleChoice(option)
        )).isEqualTo(new QuestionnaireBranchResolution(
                third,
                QuestionnaireBranchResolution.Transition.CONDITIONAL
        ));
        UUID otherOption = UUID.randomUUID();
        QuestionnaireSnapshotPayload.Item expanded = item(
                first,
                0,
                QuestionType.SINGLE_CHOICE,
                second,
                List.of(
                        new QuestionnaireSnapshotPayload.Option(option, "Yes", 0),
                        new QuestionnaireSnapshotPayload.Option(otherOption, "No", 1)
                ),
                snapshot.items().get(0).branchRules()
        );
        assertThat(resolver.resolve(
                snapshot(List.of(expanded, snapshot.items().get(1), snapshot.items().get(2))),
                first,
                StandardizedQuestionnaireAnswer.singleChoice(otherOption)
        )).isEqualTo(new QuestionnaireBranchResolution(
                second,
                QuestionnaireBranchResolution.Transition.DEFAULT
        ));
    }

    @Test
    void scaleMatchesByValueAndLastDefaultEnds() {
        UUID itemId = UUID.randomUUID();
        QuestionnaireSnapshotPayload snapshot = snapshot(List.of(new QuestionnaireSnapshotPayload.Item(
                itemId,
                UUID.randomUUID(),
                0,
                QuestionType.SCALE,
                "Rate",
                true,
                List.of(),
                1,
                5,
                null,
                null,
                Instant.EPOCH,
                Instant.EPOCH,
                null,
                List.of()
        )));

        assertThat(resolver.resolve(snapshot, itemId, StandardizedQuestionnaireAnswer.scale(3)))
                .isEqualTo(new QuestionnaireBranchResolution(
                        null,
                        QuestionnaireBranchResolution.Transition.END
                ));
        assertThatThrownBy(() -> resolver.resolve(
                snapshot,
                itemId,
                StandardizedQuestionnaireAnswer.scale(6)
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void skippedOptionalChoiceUsesItsDefaultPath() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        QuestionnaireSnapshotPayload snapshot = snapshot(List.of(
                item(
                        first,
                        0,
                        QuestionType.SINGLE_CHOICE,
                        second,
                        List.of(new QuestionnaireSnapshotPayload.Option(
                                UUID.randomUUID(),
                                "Continue",
                                0
                        )),
                        List.of()
                ),
                item(second, 1, QuestionType.TEXT, null, List.of(), List.of())
        ));

        assertThat(resolver.resolve(
                snapshot,
                first,
                StandardizedQuestionnaireAnswer.unanswered()
        )).isEqualTo(new QuestionnaireBranchResolution(
                second,
                QuestionnaireBranchResolution.Transition.DEFAULT
        ));
    }

    @Test
    @EnabledIfSystemProperty(named = "survey.performance.tests", matches = "true")
    void resolvesOneHundredItemSnapshotWithinTentativeBudget() {
        List<QuestionnaireSnapshotPayload.Item> items = new ArrayList<>();
        List<UUID> ids = java.util.stream.IntStream.range(0, 100)
                .mapToObj(ignored -> UUID.randomUUID())
                .toList();
        for (int index = 0; index < ids.size(); index++) {
            items.add(item(
                    ids.get(index),
                    index,
                    QuestionType.TEXT,
                    index + 1 < ids.size() ? ids.get(index + 1) : null,
                    List.of(),
                    List.of()
            ));
        }
        QuestionnaireSnapshotPayload snapshot = snapshot(items);

        long started = System.nanoTime();
        for (int repetition = 0; repetition < 10_000; repetition++) {
            resolver.resolve(snapshot, ids.get(repetition % ids.size()),
                    StandardizedQuestionnaireAnswer.text("ok"));
        }
        Duration elapsed = Duration.ofNanos(System.nanoTime() - started);

        assertThat(elapsed).isLessThan(Duration.ofSeconds(5));
    }

    private QuestionnaireSnapshotPayload snapshot(List<QuestionnaireSnapshotPayload.Item> items) {
        return new QuestionnaireSnapshotPayload(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                Instant.EPOCH,
                items
        );
    }

    private QuestionnaireSnapshotPayload.Item item(
            UUID itemId,
            int position,
            QuestionType type,
            UUID defaultNext,
            List<QuestionnaireSnapshotPayload.Option> options,
            List<QuestionnaireSnapshotPayload.BranchRule> rules
    ) {
        return new QuestionnaireSnapshotPayload.Item(
                itemId,
                UUID.randomUUID(),
                position,
                type,
                "Question " + position,
                false,
                options,
                null,
                null,
                null,
                null,
                Instant.EPOCH,
                Instant.EPOCH,
                defaultNext,
                rules
        );
    }
}
