package com.cs_42_3.surveyplatformbackend.study.api.dto;

import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;
import java.util.List;

/**
 * Stable pagination contract independent of Spring's Page serialization.
 *
 * @author Simon Tian
 */
public record StudyPageResponse(
        List<StudySummaryResponse> content,
        @Schema(description = "Zero-based page index.") int page,
        @Schema(description = "Requested page size, from 1 to 100.") int size,
        long totalElements,
        int totalPages
) {
    /** Maps an empty or populated page to the public response. */
    public static StudyPageResponse from(Page<Study> studies) {
        return new StudyPageResponse(studies.getContent().stream()
                .map(StudySummaryResponse::from).toList(),
                studies.getNumber(), studies.getSize(),
                studies.getTotalElements(), studies.getTotalPages());
    }
}
