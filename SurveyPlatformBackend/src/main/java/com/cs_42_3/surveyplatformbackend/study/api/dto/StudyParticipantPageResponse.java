package com.cs_42_3.surveyplatformbackend.study.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;
import java.util.List;

/**
 * Keeps session pagination independent of Spring's internal Page serialization.
 *
 * @author Simon Tian
 */
public record StudyParticipantPageResponse(
        List<StudyParticipantSummaryResponse> content,
        @Schema(description = "Zero-based page index.") int page,
        @Schema(description = "Requested page size, from 1 to 100.") int size,
        long totalElements,
        int totalPages
) {
    public static StudyParticipantPageResponse from(Page<StudyParticipantSummaryResponse> sessions) {
        return new StudyParticipantPageResponse(sessions.getContent(), sessions.getNumber(), sessions.getSize(),
                sessions.getTotalElements(), sessions.getTotalPages());
    }
}
