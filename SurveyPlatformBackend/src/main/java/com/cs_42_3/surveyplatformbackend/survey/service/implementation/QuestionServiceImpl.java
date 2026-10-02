package com.cs_42_3.surveyplatformbackend.survey.service.implementation;

import com.cs_42_3.surveyplatformbackend.survey.api.dto.CreateQuestionRequest;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.CreateQuestionOptionRequest;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.QuestionResponse;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.QuestionPageResponse;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.UpdateQuestionRequest;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.UpdateQuestionOptionRequest;
import com.cs_42_3.surveyplatformbackend.survey.api.mapper.QuestionResponseMapper;
import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.exception.InvalidQuestionDataException;
import com.cs_42_3.surveyplatformbackend.survey.exception.QuestionNotFoundException;
import com.cs_42_3.surveyplatformbackend.survey.repository.QuestionRepository;
import com.cs_42_3.surveyplatformbackend.security.CurrentResearcher;
import com.cs_42_3.surveyplatformbackend.survey.service.QuestionService;
import com.cs_42_3.surveyplatformbackend.survey.service.QuestionnaireQuestionReferencePort;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
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
    private final CurrentResearcher currentResearcher;
    private final QuestionResponseMapper questionResponseMapper;
    private final QuestionnaireQuestionReferencePort questionnaireReferencePort;

    @Override
    @Transactional(readOnly = true)
    public QuestionPageResponse listQuestions(
            QuestionType type,
            String search,
            int page,
            int size
    ) {
        if (page < 0 || size < 1 || size > 100) {
            throw new InvalidQuestionDataException(
                    "INVALID_PAGINATION",
                    "Page must be nonnegative and size must be between 1 and 100"
            );
        }
        UUID researcherId = currentResearcher.getId();
        var pageable = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("updatedAt"), Sort.Order.desc("id"))
        );
        return QuestionPageResponse.from(questionRepository.searchQuestions(
                researcherId,
                type,
                normalizeKeyword(search),
                pageable
        ));
    }

    @Override
    @Transactional(readOnly = true)
    public QuestionResponse getQuestion(UUID questionId) {
        UUID researcherId = currentResearcher.getId();
        return questionResponseMapper.toResponse(findOwnedQuestion(questionId, researcherId));
    }

    @Override
    @Transactional
    public QuestionResponse createQuestion(CreateQuestionRequest request) {
        NormalizedQuestionData data = normalizeQuestionData(request);
        UUID researcherId = currentResearcher.getId();

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
        question.replaceOptions(data.options().stream()
                .map(NormalizedOptionData::optionText)
                .toList());

        return questionResponseMapper.toResponse(questionRepository.saveAndFlush(question));
    }

    @Override
    @Transactional
    public QuestionResponse updateQuestion(UUID questionId, UpdateQuestionRequest request) {
        UUID researcherId = currentResearcher.getId();
        findOwnedQuestion(questionId, researcherId);
        NormalizedQuestionData data = normalizeQuestionData(request);
        QuestionnaireQuestionReferencePort.ReferenceLock referenceLock =
                questionnaireReferencePort.lockReferences(questionId);
        Question question = questionRepository.findOwnedByIdForUpdate(questionId, researcherId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
        validateOptionIdentities(question, data.options(), request.replaceAllOptions());
        questionnaireReferencePort.validateUpdate(
                question,
                toProposedDefinition(data),
                referenceLock
        );

        question.update(
                data.type(),
                data.questionText(),
                data.required(),
                data.scaleMin(),
                data.scaleMax(),
                data.scaleMinLabel(),
                data.scaleMaxLabel()
        );
        question.synchronizeOptions(data.options().stream()
                .map(option -> new Question.OptionPlacement(option.optionId(), option.optionText()))
                .toList());

        return questionResponseMapper.toResponse(questionRepository.saveAndFlush(question));
    }

    @Override
    @Transactional
    public void deleteQuestion(UUID questionId) {
        UUID researcherId = currentResearcher.getId();
        findOwnedQuestion(questionId, researcherId);
        QuestionnaireQuestionReferencePort.ReferenceLock referenceLock =
                questionnaireReferencePort.lockReferences(questionId);
        Question question = questionRepository.findOwnedByIdForUpdate(questionId, researcherId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
        questionnaireReferencePort.prepareDelete(question, referenceLock);
        questionRepository.delete(question);
    }

    private QuestionnaireQuestionReferencePort.ProposedQuestionDefinition toProposedDefinition(
            NormalizedQuestionData data
    ) {
        Set<UUID> retainedOptionIds = data.options().stream()
                .map(NormalizedOptionData::optionId)
                .filter(java.util.Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        return new QuestionnaireQuestionReferencePort.ProposedQuestionDefinition(
                data.type(),
                data.required(),
                retainedOptionIds,
                data.options().size(),
                data.scaleMin(),
                data.scaleMax()
        );
    }

    private Question findOwnedQuestion(UUID questionId, UUID researcherId) {
        return questionRepository.findByIdAndResearcherId(questionId, researcherId)
                .orElseThrow(() -> new QuestionNotFoundException(questionId));
    }

    private NormalizedQuestionData normalizeQuestionData(CreateQuestionRequest request) {
        if (request == null) {
            throw new InvalidQuestionDataException("Question data is required");
        }
        List<NormalizedOptionData> options = normalizeCreateOptions(request.options());
        return normalizeQuestionData(
                request.type(),
                request.questionText(),
                request.required(),
                options,
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
        List<NormalizedOptionData> options = normalizeUpdateOptions(request.options());
        return normalizeQuestionData(
                request.type(),
                request.questionText(),
                request.required(),
                options,
                request.scaleMin(),
                request.scaleMax(),
                request.scaleMinLabel(),
                request.scaleMaxLabel()
        );
    }

    private List<NormalizedOptionData> normalizeCreateOptions(
            List<CreateQuestionOptionRequest> options
    ) {
        if (options == null) {
            return List.of();
        }
        return options.stream()
                .map(option -> {
                    if (option == null) {
                        return (NormalizedOptionData) null;
                    }
                    if (option.optionId() != null) {
                        throw new InvalidQuestionDataException(
                                "CREATE_OPTION_ID_NOT_ALLOWED",
                                "Option ID must be omitted when creating a question"
                        );
                    }
                    return new NormalizedOptionData(null, option.optionText());
                })
                .toList();
    }

    private List<NormalizedOptionData> normalizeUpdateOptions(
            List<UpdateQuestionOptionRequest> options
    ) {
        if (options == null) {
            return List.of();
        }
        return options.stream()
                .map(option -> option == null
                        ? null
                        : new NormalizedOptionData(option.optionId(), option.optionText()))
                .toList();
    }

    private void validateOptionIdentities(
            Question question,
            List<NormalizedOptionData> requestedOptions,
            boolean replaceAllOptions
    ) {
        Set<UUID> existingOptionIds = question.getOptions().stream()
                .map(option -> option.getId())
                .filter(id -> id != null)
                .collect(java.util.stream.Collectors.toSet());
        Set<UUID> retainedOptionIds = new HashSet<>();
        for (NormalizedOptionData option : requestedOptions) {
            UUID optionId = option.optionId();
            if (optionId == null) {
                continue;
            }
            if (!retainedOptionIds.add(optionId)) {
                throw new InvalidQuestionDataException(
                        "DUPLICATE_OPTION_ID",
                        "Option ID is duplicated: " + optionId
                );
            }
            if (!existingOptionIds.contains(optionId)) {
                throw new InvalidQuestionDataException(
                        "INVALID_OPTION_REFERENCE",
                        "Option does not belong to this question: " + optionId
                );
            }
        }

        if (!question.getOptions().isEmpty()
                && retainedOptionIds.isEmpty()
                && !replaceAllOptions) {
            throw new InvalidQuestionDataException(
                    "OPTION_IDENTITIES_REQUIRED",
                    "Retain existing option IDs or set replaceAllOptions to true"
            );
        }
    }

    private NormalizedQuestionData normalizeQuestionData(
            QuestionType type,
            String questionText,
            boolean required,
            List<NormalizedOptionData> options,
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
            List<NormalizedOptionData> options,
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

    private List<NormalizedOptionData> normalizeChoiceOptions(List<NormalizedOptionData> options) {
        return options.stream()
                .map(option -> new NormalizedOptionData(
                        option.optionId(),
                        option.optionText().trim()
                ))
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

    private record NormalizedQuestionData(
            QuestionType type,
            String questionText,
            boolean required,
            List<NormalizedOptionData> options,
            Integer scaleMin,
            Integer scaleMax,
            String scaleMinLabel,
            String scaleMaxLabel
    ) {

        private NormalizedQuestionData {
            options = List.copyOf(options);
        }
    }

    private record NormalizedOptionData(UUID optionId, String optionText) {}
}
