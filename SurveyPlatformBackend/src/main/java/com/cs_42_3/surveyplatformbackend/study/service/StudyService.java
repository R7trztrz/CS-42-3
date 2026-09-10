package com.cs_42_3.surveyplatformbackend.study.service;

import com.cs_42_3.surveyplatformbackend.study.domain.Study;

import java.util.UUID;

/**
 * Defines study management operations.
 *
 * @author Simon Tian
 */
public interface StudyService {

    /**
     * Creates a draft study for an authenticated researcher.
     * The caller must authenticate and authorize the researcher and derive the
     * owner ID from that identity, never from a client-supplied ownership field.
     *
     * @param ownerId the authenticated researcher's identifier
     * @param title the nonblank study title, at most 255 characters
     * @param description the optional study description
     * @return the persisted draft study
     * @throws jakarta.validation.ConstraintViolationException if the study is invalid
     */
    Study createStudy(UUID ownerId, String title, String description);
}
