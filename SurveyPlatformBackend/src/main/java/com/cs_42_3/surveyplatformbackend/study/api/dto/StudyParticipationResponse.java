package com.cs_42_3.surveyplatformbackend.study.api.dto;

/**
 * Returns summary counts and a session page from the same database snapshot.
 *
 * @author Simon Tian
 */
public record StudyParticipationResponse(
        StudyParticipationSummaryResponse summary,
        StudyParticipantPageResponse sessions
) {}
