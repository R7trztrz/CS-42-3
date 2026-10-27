package com.cs_42_3.surveyplatformbackend.study.service;

import com.cs_42_3.surveyplatformbackend.study.api.dto.StudyParticipationResponse;
import java.util.UUID;

/**
 * Provides the owner-scoped FR-19 participation overview and anonymous progress list.
 *
 * @author Simon Tian
 */
public interface StudyParticipationService {
    StudyParticipationResponse getParticipation(UUID ownerId, UUID studyId,
            int page, int size, String sortBy, String direction);
}
