package com.cs_42_3.surveyplatformbackend.participation.service;

import com.cs_42_3.surveyplatformbackend.participation.api.dto.SubmitQuestionnaireAnswerRequest;
import com.cs_42_3.surveyplatformbackend.participation.exception.ParticipationException;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.domain.QuestionnaireSnapshotPayload;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.service.StandardizedQuestionnaireAnswer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ParticipantAnswerNormalizer {
    private final ObjectMapper objectMapper;

    public NormalizedAnswer normalize(
            QuestionnaireSnapshotPayload.Item item,
            SubmitQuestionnaireAnswerRequest request
    ) {
        if (request == null) {
            throw ParticipationException.answerInvalid();
        }
        if (request.isUnanswered()) {
            if (item.required() || hasAnyAnswerField(request)) {
                throw ParticipationException.answerInvalid();
            }
            return new NormalizedAnswer(
                    item.type(),
                    StandardizedQuestionnaireAnswer.unanswered(),
                    null,
                    "UNANSWERED"
            );
        }
        if (providedFieldCount(request) != 1) {
            throw ParticipationException.answerInvalid();
        }
        try {
            return switch (item.type()) {
                case SINGLE_CHOICE -> singleChoice(item, request);
                case MULTI_CHOICE -> multiChoice(item, request);
                case SCALE -> scale(item, request);
                case TEXT -> text(item, request);
            };
        } catch (IllegalArgumentException exception) {
            throw ParticipationException.answerInvalid();
        }
    }

    public String requestHash(UUID itemId, NormalizedAnswer answer) {
        String value = itemId + "|" + answer.canonicalRequest();
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private NormalizedAnswer singleChoice(
            QuestionnaireSnapshotPayload.Item item,
            SubmitQuestionnaireAnswerRequest request
    ) {
        UUID optionId = request.optionId();
        if (optionId == null || !publishedOptions(item).contains(optionId)) {
            throw new IllegalArgumentException("Invalid option");
        }
        return new NormalizedAnswer(
                item.type(),
                StandardizedQuestionnaireAnswer.singleChoice(optionId),
                objectMapper.writeValueAsString(Map.of("optionId", optionId)),
                "SINGLE:" + optionId
        );
    }

    private NormalizedAnswer multiChoice(
            QuestionnaireSnapshotPayload.Item item,
            SubmitQuestionnaireAnswerRequest request
    ) {
        if (request.optionIds() == null || request.optionIds().isEmpty()
                || request.optionIds().stream().anyMatch(java.util.Objects::isNull)) {
            throw new IllegalArgumentException("Invalid options");
        }
        List<UUID> options = request.optionIds().stream()
                .distinct()
                .sorted(Comparator.comparing(UUID::toString))
                .toList();
        if (!publishedOptions(item).containsAll(options)) {
            throw new IllegalArgumentException("Invalid options");
        }
        return new NormalizedAnswer(
                item.type(),
                StandardizedQuestionnaireAnswer.multiChoice(new LinkedHashSet<>(options)),
                objectMapper.writeValueAsString(Map.of("optionIds", options)),
                "MULTI:" + options.stream().map(UUID::toString).collect(Collectors.joining(","))
        );
    }

    private NormalizedAnswer scale(
            QuestionnaireSnapshotPayload.Item item,
            SubmitQuestionnaireAnswerRequest request
    ) {
        Integer value = request.scaleValue();
        if (value == null || item.scaleMin() == null || item.scaleMax() == null
                || value < item.scaleMin() || value > item.scaleMax()) {
            throw new IllegalArgumentException("Invalid scale");
        }
        return new NormalizedAnswer(
                item.type(),
                StandardizedQuestionnaireAnswer.scale(value),
                objectMapper.writeValueAsString(Map.of("value", value)),
                "SCALE:" + value
        );
    }

    private NormalizedAnswer text(
            QuestionnaireSnapshotPayload.Item item,
            SubmitQuestionnaireAnswerRequest request
    ) {
        String value = request.textValue();
        if (value == null || (item.required() && value.isBlank())) {
            throw new IllegalArgumentException("Invalid text");
        }
        String encoded = Base64.getEncoder().encodeToString(value.getBytes(StandardCharsets.UTF_8));
        return new NormalizedAnswer(
                item.type(),
                StandardizedQuestionnaireAnswer.text(value),
                objectMapper.writeValueAsString(Map.of("text", value)),
                "TEXT:" + encoded
        );
    }

    private Set<UUID> publishedOptions(QuestionnaireSnapshotPayload.Item item) {
        return item.options().stream()
                .map(QuestionnaireSnapshotPayload.Option::optionId)
                .collect(Collectors.toSet());
    }

    private boolean hasAnyAnswerField(SubmitQuestionnaireAnswerRequest request) {
        return request.optionId() != null
                || request.optionIds() != null
                || request.scaleValue() != null
                || request.textValue() != null;
    }

    private int providedFieldCount(SubmitQuestionnaireAnswerRequest request) {
        int count = 0;
        count += request.optionId() == null ? 0 : 1;
        count += request.optionIds() == null ? 0 : 1;
        count += request.scaleValue() == null ? 0 : 1;
        count += request.textValue() == null ? 0 : 1;
        return count;
    }

    public record NormalizedAnswer(
            QuestionType questionType,
            StandardizedQuestionnaireAnswer standardized,
            String answerPayload,
            String canonicalRequest
    ) {
        public boolean answered() {
            return answerPayload != null;
        }
    }
}
