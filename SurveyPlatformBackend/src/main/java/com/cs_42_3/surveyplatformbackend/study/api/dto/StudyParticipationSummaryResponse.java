package com.cs_42_3.surveyplatformbackend.study.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Counts persisted sessions for a study, rather than link clicks or unique people.
 *
 * @author Simon Tian
 */
public record StudyParticipationSummaryResponse(
        @Schema(description = "Total created sessions, including sessions that declined consent.") long total,
        long inProgress,
        long completed,
        long abandoned
) {}
