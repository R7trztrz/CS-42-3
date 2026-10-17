package com.cs_42_3.surveyplatformbackend.study.service;

import java.util.UUID;

/**
 * Permanently deletes an owned study and schedules durable file cleanup for FR-18.
 *
 * @author Simon Tian
 */
public interface StudyDeletionService {
    void delete(UUID ownerId, UUID studyId, long expectedVersion);
}
