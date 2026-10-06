package com.cs_42_3.surveyplatformbackend.participation.service;

import com.cs_42_3.surveyplatformbackend.participation.api.dto.SubmitQuestionnaireAnswerRequest;
import com.cs_42_3.surveyplatformbackend.participation.exception.ParticipationException;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.domain.QuestionnaireSnapshotPayload;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParticipantAnswerNormalizerTest {
    private final ParticipantAnswerNormalizer normalizer =
            new ParticipantAnswerNormalizer(new ObjectMapper());

    @Test
    void normalizesAndStablyHashesMultiChoiceRegardlessOfOrderOrDuplicates() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        var item = item(QuestionType.MULTI_CHOICE, true, List.of(first, second), null, null);
        var left = normalizer.normalize(item, request(null, List.of(second, first, second), null, null, false));
        var right = normalizer.normalize(item, request(null, List.of(first, second), null, null, false));

        assertThat(left.answerPayload()).isEqualTo(right.answerPayload());
        assertThat(normalizer.requestHash(item.itemId(), left))
                .isEqualTo(normalizer.requestHash(item.itemId(), right))
                .matches("[0-9a-f]{64}");
    }

    @Test
    void preservesTextButUsesTrimOnlyForRequiredValidation() {
        var item = item(QuestionType.TEXT, true, List.of(), null, null);
        var normalized = normalizer.normalize(
                item,
                request(null, null, null, "  participant text  ", false)
        );

        assertThat(normalized.answerPayload()).contains("  participant text  ");
        assertThatThrownBy(() -> normalizer.normalize(
                item, request(null, null, null, "   ", false)
        )).isInstanceOf(ParticipationException.class);
    }

    @Test
    void validatesSingleChoiceAndScaleAgainstPublishedContent() {
        UUID option = UUID.randomUUID();
        var single = item(QuestionType.SINGLE_CHOICE, true, List.of(option), null, null);
        var scale = item(QuestionType.SCALE, true, List.of(), 1, 5);

        assertThat(normalizer.normalize(
                single, request(option, null, null, null, false)
        ).answered()).isTrue();
        assertThat(normalizer.normalize(
                scale, request(null, null, 5, null, false)
        ).answered()).isTrue();
        assertThatThrownBy(() -> normalizer.normalize(
                single, request(UUID.randomUUID(), null, null, null, false)
        )).isInstanceOf(ParticipationException.class);
        assertThatThrownBy(() -> normalizer.normalize(
                scale, request(null, null, 6, null, false)
        )).isInstanceOf(ParticipationException.class);
    }

    @Test
    void optionalUnansweredCreatesNoPayloadAndRejectsMixedFields() {
        var optional = item(QuestionType.TEXT, false, List.of(), null, null);
        var required = item(QuestionType.TEXT, true, List.of(), null, null);

        assertThat(normalizer.normalize(
                optional, request(null, null, null, null, true)
        ).answered()).isFalse();
        assertThatThrownBy(() -> normalizer.normalize(
                required, request(null, null, null, null, true)
        )).isInstanceOf(ParticipationException.class);
        assertThatThrownBy(() -> normalizer.normalize(
                optional, request(null, null, null, "mixed", true)
        )).isInstanceOf(ParticipationException.class);
    }

    private SubmitQuestionnaireAnswerRequest request(
            UUID optionId,
            List<UUID> optionIds,
            Integer scale,
            String text,
            boolean unanswered
    ) {
        return new SubmitQuestionnaireAnswerRequest(optionId, optionIds, scale, text, unanswered);
    }

    private QuestionnaireSnapshotPayload.Item item(
            QuestionType type,
            boolean required,
            List<UUID> optionIds,
            Integer scaleMin,
            Integer scaleMax
    ) {
        Instant now = Instant.parse("2026-10-03T00:00:00Z");
        return new QuestionnaireSnapshotPayload.Item(
                UUID.randomUUID(),
                UUID.randomUUID(),
                1,
                type,
                "Question",
                required,
                optionIds.stream()
                        .map(id -> new QuestionnaireSnapshotPayload.Option(id, "Option", 1))
                        .toList(),
                scaleMin,
                scaleMax,
                null,
                null,
                now,
                now,
                null,
                List.of()
        );
    }
}
