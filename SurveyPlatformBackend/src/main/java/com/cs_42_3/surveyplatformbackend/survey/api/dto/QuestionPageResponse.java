package com.cs_42_3.surveyplatformbackend.survey.api.dto;

import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

import java.util.List;

/** Stable question-bank pagination contract independent of Spring's Page serialization. */
public record QuestionPageResponse(
        List<QuestionSummaryResponse> content,
        @Schema(description = "Zero-based page index") int page,
        @Schema(description = "Requested page size, from 1 to 100") int size,
        long totalElements,
        int totalPages
) {
    public QuestionPageResponse {
        content = content == null ? List.of() : List.copyOf(content);
    }

    /** Maps one owner-filtered repository page to the public response. */
    public static QuestionPageResponse from(Page<Question> questions) {
        return new QuestionPageResponse(
                questions.getContent().stream()
                        .map(QuestionSummaryResponse::from)
                        .toList(),
                questions.getNumber(),
                questions.getSize(),
                questions.getTotalElements(),
                questions.getTotalPages()
        );
    }
}
