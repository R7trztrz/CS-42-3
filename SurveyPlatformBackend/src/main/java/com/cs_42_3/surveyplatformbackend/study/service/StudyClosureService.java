package com.cs_42_3.surveyplatformbackend.study.service;

import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import java.util.UUID;

/**
 * Permanently closes an owned study and terminates its active participation sessions.
 *
 * @author Simon Tian
 */
public interface StudyClosureService {
    /** Returns an already closed study unchanged so a lost response can be safely retried. */
    Study close(UUID ownerId, UUID studyId, long expectedVersion);
}
