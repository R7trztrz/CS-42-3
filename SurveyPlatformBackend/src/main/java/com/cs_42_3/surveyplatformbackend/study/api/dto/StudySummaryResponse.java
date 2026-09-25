package com.cs_42_3.surveyplatformbackend.study.api.dto;

import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.UUID;

/**
 * Minimal study information for the researcher's list.
 *
 * @author Simon Tian
 */
@Schema(description = "An owned study in the researcher's list.")
public record StudySummaryResponse(UUID id, String title, StudyStatus status, Instant createdAt,
        @Schema(description = "Last change to the study itself; feed and questionnaire edits are excluded.")
        Instant updatedAt) {
    /** Maps an entity without exposing ownership or locking metadata. */
    public static StudySummaryResponse from(Study study) {
        return new StudySummaryResponse(study.getId(), study.getTitle(),
                study.getStatus(), study.getCreatedAt(), study.getUpdatedAt());
    }
}
