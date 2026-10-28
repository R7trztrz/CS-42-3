package com.cs_42_3.surveyplatformbackend.study.service;

import com.cs_42_3.surveyplatformbackend.study.api.dto.StudyParticipationResponse;
import com.cs_42_3.surveyplatformbackend.study.api.dto.StudyParticipantDetailResponse;
import java.util.UUID;

/**
 * Provides the owner-scoped FR-19 overview and FR-20 operational session details.
 *
 * @author Simon Tian
 */
public interface StudyParticipationService {
    StudyParticipantDetailResponse getParticipant(UUID ownerId, UUID studyId, UUID sessionId);

    StudyParticipationResponse getParticipation(UUID ownerId, UUID studyId,
            int page, int size, String sortBy, String direction);
}
