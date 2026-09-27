package com.cs_42_3.surveyplatformbackend.study.service;

import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import java.util.UUID;

/**
 * Publishes an owned draft exactly once.
 *
 * @author Simon Tian
 */
public interface StudyPublicationService {
    Study publish(UUID ownerId, UUID studyId, long expectedVersion);
}
