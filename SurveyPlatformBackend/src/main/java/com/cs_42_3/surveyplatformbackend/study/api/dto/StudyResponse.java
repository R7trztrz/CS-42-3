package com.cs_42_3.surveyplatformbackend.study.api.dto;

import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Public study representation, excluding persistence locking metadata.
 */
@Schema(description = "Study details returned by the API. Newly created studies have DRAFT status.")
public record StudyResponse(
        @Schema(description = "Server-generated study identifier.", format = "uuid", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID id,
        @Schema(description = "Study title.", example = "Social media browsing study", maxLength = 255)
        String title,
        @Schema(description = "Optional study description.", example = "Investigate browsing behaviour in a simulated social media feed.")
        String description,
        @Schema(description = "Study lifecycle state. Creation always returns DRAFT.", example = "DRAFT")
        StudyStatus status,
        @Schema(description = "Creation timestamp in UTC.", format = "date-time", example = "2026-09-11T00:00:00Z")
        Instant createdAt,
        @Schema(description = "Last modification timestamp in UTC.", format = "date-time", example = "2026-09-11T00:00:00Z")
        Instant updatedAt
) {
    /**
     * Maps a persisted study to its API representation.
     *
     * @param study the persisted study
     * @return the response representation
     */
    public static StudyResponse from(Study study) {
        return new StudyResponse(study.getId(), study.getTitle(), study.getDescription(),
                study.getStatus(), study.getCreatedAt(), study.getUpdatedAt());
    }
}
