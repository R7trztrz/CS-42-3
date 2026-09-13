package com.cs_42_3.surveyplatformbackend.survey.service;

import com.cs_42_3.surveyplatformbackend.survey.dto.QuestionOptionRequest;
import com.cs_42_3.surveyplatformbackend.survey.dto.QuestionOptionResponse;
import com.cs_42_3.surveyplatformbackend.survey.dto.QuestionRequest;
import com.cs_42_3.surveyplatformbackend.survey.dto.QuestionResponse;
import com.cs_42_3.surveyplatformbackend.survey.entity.Question;
import com.cs_42_3.surveyplatformbackend.survey.entity.QuestionOption;
import com.cs_42_3.surveyplatformbackend.survey.entity.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.exception.QuestionNotFoundException;
import com.cs_42_3.surveyplatformbackend.survey.exception.QuestionValidationException;
import com.cs_42_3.surveyplatformbackend.survey.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;

    // ---------------- UC-21: list + search ----------------

    @Transactional(readOnly = true)
    public List<QuestionResponse> listQuestions(QuestionType type, String keyword) {
        Long researcherId = currentResearcherId();
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        return questionRepository.search(researcherId, type, normalizedKeyword).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuestionResponse getQuestion(Long id) {
        return toResponse(findOwnedOrThrow(id));
    }

    // ---------------- UC-22: create ----------------

    @Transactional
    public QuestionResponse createQuestion(QuestionRequest request) {
        validateForType(request);

        Question question = Question.builder()
                .researcherId(currentResearcherId())
                .type(request.getType())
                .questionText(request.getQuestionText())
                .scaleMin(request.getScaleMin())
                .scaleMax(request.getScaleMax())
                .scaleMinLabel(request.getScaleMinLabel())
                .scaleMaxLabel(request.getScaleMaxLabel())
                .build();

        question.replaceOptions(buildOptions(request, question));

        return toResponse(questionRepository.save(question));
    }

    // ---------------- UC-23: edit ----------------

    @Transactional
    public QuestionResponse updateQuestion(Long id, QuestionRequest request) {
        validateForType(request);

        Question question = findOwnedOrThrow(id);
        question.setType(request.getType());
        question.setQuestionText(request.getQuestionText());
        question.setScaleMin(request.getScaleMin());
        question.setScaleMax(request.getScaleMax());
        question.setScaleMinLabel(request.getScaleMinLabel());
        question.setScaleMaxLabel(request.getScaleMaxLabel());
        question.replaceOptions(buildOptions(request, question));

        return toResponse(questionRepository.save(question));
    }

    // ---------------- UC-23: delete ----------------

    /**
     * v1 simplification per the requirements doc: no reference-check/blocking against
     * unpublished surveys that might enable this question. A survey referencing a deleted
     * question is expected to surface a "missing question" marker on its own side — that
     * behaviour belongs to the survey/questionnaire layer (UC-24), not here.
     */
    @Transactional
    public void deleteQuestion(Long id) {
        Question question = findOwnedOrThrow(id);
        questionRepository.delete(question);
    }

    // ---------------- Internal helpers ----------------

    private Question findOwnedOrThrow(Long id) {
        return questionRepository.findByIdAndResearcherId(id, currentResearcherId())
                .orElseThrow(() -> new QuestionNotFoundException(id));
    }

    /**
     * Type-conditional validation that plain Bean Validation annotations on QuestionRequest
     * can't express (see QuestionRequest javadoc). Each branch below corresponds to one
     * acceptance criterion in UC-22 ("each type can be correctly entered via the form").
     */
    private void validateForType(QuestionRequest request) {
        switch (request.getType()) {
            case SINGLE_CHOICE, MULTI_CHOICE -> {
                if (request.getOptions() == null || request.getOptions().size() < 2) {
                    throw new QuestionValidationException(
                            "options: at least 2 options are required for " + request.getType());
                }
                boolean hasBlank = request.getOptions().stream()
                        .anyMatch(opt -> opt.getOptionText() == null || opt.getOptionText().isBlank());
                if (hasBlank) {
                    throw new QuestionValidationException("options: option text must not be blank");
                }
            }
            case SCALE -> {
                if (request.getScaleMin() == null || request.getScaleMax() == null) {
                    throw new QuestionValidationException("scaleMin/scaleMax: both are required for SCALE questions");
                }
                if (request.getScaleMin() >= request.getScaleMax()) {
                    throw new QuestionValidationException("scaleMin: must be less than scaleMax");
                }
            }
            case TEXT -> {
                // No extra fields required — options/scale values, if present, are ignored below.
            }
        }
    }

    /** Builds the option entity list for SINGLE_CHOICE/MULTI_CHOICE; empty otherwise. */
    private List<QuestionOption> buildOptions(QuestionRequest request, Question owner) {
        List<QuestionOption> result = new ArrayList<>();
        if (request.getType() == QuestionType.SINGLE_CHOICE || request.getType() == QuestionType.MULTI_CHOICE) {
            List<QuestionOptionRequest> options = request.getOptions();
            for (int i = 0; i < options.size(); i++) {
                result.add(QuestionOption.builder()
                        .question(owner)
                        .optionText(options.get(i).getOptionText())
                        .optionOrder(i)
                        .build());
            }
        }
        return result;
    }

    private QuestionResponse toResponse(Question q) {
        List<QuestionOptionResponse> options = q.getOptions().stream()
                .map(o -> new QuestionOptionResponse(o.getId(), o.getOptionText(), o.getOptionOrder()))
                .collect(Collectors.toList());

        return QuestionResponse.builder()
                .id(q.getId())
                .type(q.getType())
                .questionText(q.getQuestionText())
                .options(options)
                .scaleMin(q.getScaleMin())
                .scaleMax(q.getScaleMax())
                .scaleMinLabel(q.getScaleMinLabel())
                .scaleMaxLabel(q.getScaleMaxLabel())
                .createdAt(q.getCreatedAt())
                .updatedAt(q.getUpdatedAt())
                .build();
    }

    /**
     * Reads the current researcher's id off the authenticated JWT principal.
     *
     * ASSUMPTION TO CONFIRM WITH M1: this assumes the JWT carries a numeric researcher id
     * in a claim named "researcherId". If M1's token instead uses "sub" as the id, or a
     * different claim name entirely, update ONLY this method — nothing else in this module
     * depends on how the id is extracted. See module README, "Integration points".
     */
    private Long currentResearcherId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new IllegalStateException("No authenticated researcher in security context");
        }
        return jwt.getClaim("researcherId");
    }
}
