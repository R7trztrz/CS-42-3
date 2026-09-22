package com.cs_42_3.surveyplatformbackend.survey.service.implementation;

import com.cs_42_3.surveyplatformbackend.survey.api.dto.CreateQuestionRequest;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.QuestionOptionRequest;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.QuestionResponse;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.QuestionSummaryResponse;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.UpdateQuestionRequest;
import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.exception.InvalidQuestionDataException;
import com.cs_42_3.surveyplatformbackend.survey.exception.QuestionInUseException;
import com.cs_42_3.surveyplatformbackend.survey.exception.QuestionNotFoundException;
import com.cs_42_3.surveyplatformbackend.survey.repository.QuestionRepository;
import com.cs_42_3.surveyplatformbackend.survey.security.CurrentResearcherProvider;
import com.cs_42_3.surveyplatformbackend.survey.service.QuestionService;
import com.cs_42_3.surveyplatformbackend.survey.service.QuestionUsageGuard;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Implements researcher-owned question-bank operations for FR32-FR35.
 * <p>
 * Enforces ownership, type-specific rules, transaction boundaries, and DTO mapping.
 */
@Service
@RequiredArgsConstructor
public class QuestionServiceImpl implements QuestionService {

    private final QuestionRepository questionRepository;
    private final CurrentResearcherProvider currentResearcherProvider;
    private final QuestionUsageGuard questionUsageGuard;

    @Override
    @Transactional(readOnly = true)
    public List<QuestionSummaryResponse> listQuestions(QuestionType type, String keyword) {
        UUID researcherId = currentResearcherProvider.getCurrentResearcherId();
        return questionRepository.searchQuestions(researcherId, type, normalizeKeyword(keyword)).stream()
                .map(this::toQuestionSummaryResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionResponse getQuestion(UUID questionId) {
        UUID researcherId = currentResearcherProvider.getCurrentResearcherId();
        return toQuestionResponse(findOwnedQuestion(questionId, researcherId));
    }

    @Override
    @Transactional
    public QuestionResponse createQuestion(CreateQuestionRequest request) {
        NormalizedQuestionData data = normalizeQuestionData(request);
        UUID researcherId = currentResearcherProvider.getCurrentResearcherId();

        Question question = Question.create(
                researcherId,
                data.type(),
                data.questionText(),
                data.required(),
                data.scaleMin(),
                data.scaleMax(),
                data.scaleMinLabel(),
                data.scaleMaxLabel()
        );
        question.replaceOptions(data.optionTexts());

        return toQuestionResponse(questionRepository.saveAndFlush(question));
    }

    @Override
    @Transactional
    public QuestionResponse updateQuestion(UUID questionId, UpdateQuestionRequest request) {
        UUID researcherId = currentResearcherProvider.getCurrentResearcherId();
        Question question = findOwnedQuestion(questionId, researcherId);
        requireNotInUse(questionId);
        NormalizedQuestionData data = normalizeQuestionData(request);

        question.update(
                data.type(),
                data.questionText(),
                data.required(),
                data.scaleMin(),
                data.scaleMax(),
                data.scaleMinLabel(),
                data.scaleMaxLabel()
        );
        question.replaceOptions(data.optionTexts());

        return toQuestionResponse(questionRepository.saveAndFlush(question));
    }

    @Override
    @Transactional
    public void deleteQuestion(UUID questionId) {
        UUID researcherId = currentResearcherProvider.getCurrentResearcherId();
        Question question = findOwnedQuestion(questionId, researcherId);
        requireNotInUse(questionId);
        questionRepository.delete(question);
    }

    private Question findOwnedQuestion(UUID questionId, UUID researcherId) {
        return questionRepository.findByIdAndResearcherId(questionId, researcherId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
    }

    // Protects NFR-14: a question enabled in any questionnaire is locked until the
    // researcher removes it from every questionnaire (FR-37). This blocks all edits,
    // not just option/type changes, because replaceOptions() always replaces every
    // option row, which would otherwise silently orphan that questionnaire's branch
    // rules (see questionnaire_branch_rules' FK to question_options).
    private void requireNotInUse(UUID questionId) {
        if (questionUsageGuard.isReferencedByAnyQuestionnaire(questionId)) {
            throw new QuestionInUseException(questionId);
        }
    }

    private NormalizedQuestionData normalizeQuestionData(CreateQuestionRequest request) {
        if (request == null) {
            throw new InvalidQuestionDataException("Question data is required");
        }
        return normalizeQuestionData(
                request.type(),
                request.questionText(),
                request.required(),
                request.options(),
                request.scaleMin(),
                request.scaleMax(),
                request.scaleMinLabel(),
                request.scaleMaxLabel()
        );
    }

    private NormalizedQuestionData normalizeQuestionData(UpdateQuestionRequest request) {
        if (request == null) {
            throw new InvalidQuestionDataException("Question data is required");
        }
        return normalizeQuestionData(
                request.type(),
                request.questionText(),
                request.required(),
                request.options(),
                request.scaleMin(),
                request.scaleMax(),
                request.scaleMinLabel(),
                request.scaleMaxLabel()
        );
    }

    private NormalizedQuestionData normalizeQuestionData(
            QuestionType type,
            String questionText,
            boolean required,
            List<QuestionOptionRequest> options,
            Integer scaleMin,
            Integer scaleMax,
            String scaleMinLabel,
            String scaleMaxLabel
    ) {
        if (type == null) {
            throw new InvalidQuestionDataException("Question type is required");
        }
        if (questionText == null || questionText.isBlank()) {
            throw new InvalidQuestionDataException("Question text is required");
        }
        validateQuestionData(type, options, scaleMin, scaleMax);

        return switch (type) {
            case SINGLE_CHOICE, MULTI_CHOICE -> new NormalizedQuestionData(
                    type,
                    questionText.trim(),
                    required,
                    normalizeChoiceOptions(options),
                    null,
                    null,
                    null,
                    null
            );
            case SCALE -> normalizeScaleQuestion(
                    type,
                    questionText.trim(),
                    required,
                    scaleMin,
                    scaleMax,
                    scaleMinLabel,
                    scaleMaxLabel
            );
            case TEXT -> new NormalizedQuestionData(
                    type,
                    questionText.trim(),
                    required,
                    List.of(),
                    null,
                    null,
                    null,
                    null
            );
        };
    }

    private void validateQuestionData(
            QuestionType type,
            List<QuestionOptionRequest> options,
            Integer scaleMin,
            Integer scaleMax
    ) {
        if (type == QuestionType.SINGLE_CHOICE || type == QuestionType.MULTI_CHOICE) {
            if (options == null || options.size() < 2) {
                throw new InvalidQuestionDataException(
                        "At least two options are required for " + type
                );
            }

            boolean hasBlankOption = options.stream()
                    .anyMatch(option -> option == null
                            || option.optionText() == null
                            || option.optionText().isBlank());
            if (hasBlankOption) {
                throw new InvalidQuestionDataException("Option text must not be blank");
            }
        }

        if (type == QuestionType.SCALE) {
            if (scaleMin == null || scaleMax == null) {
                throw new InvalidQuestionDataException(
                        "Scale minimum and maximum are required for SCALE questions"
                );
            }
            if (scaleMin >= scaleMax) {
                throw new InvalidQuestionDataException(
                        "Scale minimum must be less than scale maximum"
                );
            }
        }
    }

    private List<String> normalizeChoiceOptions(List<QuestionOptionRequest> options) {
        return options.stream()
                .map(option -> option.optionText().trim())
                .toList();
    }

    private NormalizedQuestionData normalizeScaleQuestion(
            QuestionType type,
            String questionText,
            boolean required,
            Integer scaleMin,
            Integer scaleMax,
            String scaleMinLabel,
            String scaleMaxLabel
    ) {
        return new NormalizedQuestionData(
                type,
                questionText,
                required,
                List.of(),
                scaleMin,
                scaleMax,
                normalizeOptionalText(scaleMinLabel),
                normalizeOptionalText(scaleMaxLabel)
        );
    }

    private String normalizeKeyword(String keyword) {
        return normalizeOptionalText(keyword);
    }

    private String normalizeOptionalText(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private QuestionSummaryResponse toQuestionSummaryResponse(Question question) {
        return new QuestionSummaryResponse(
                question.getId(),
                question.getType(),
                question.getQuestionText(),
                question.getUpdatedAt()
        );
    }

    private QuestionResponse toQuestionResponse(Question question) {
        return QuestionResponse.from(question);
    }

    private record NormalizedQuestionData(
            QuestionType type,
            String questionText,
            boolean required,
            List<String> optionTexts,
            Integer scaleMin,
            Integer scaleMax,
            String scaleMinLabel,
            String scaleMaxLabel
    ) {

        private NormalizedQuestionData {
            optionTexts = List.copyOf(optionTexts);
        }
    }
}
