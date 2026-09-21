package com.cs_42_3.surveyplatformbackend.study.service;

import com.cs_42_3.surveyplatformbackend.study.domain.Study;

import java.util.UUID;
import org.springframework.data.domain.Page;

/**
 * Defines study management operations.
 *
 * @author Simon Tian
 */
public interface StudyService {
    /** Returns an owner's studies in stable creation order using bounded pagination. */
    Page<Study> listStudies(UUID ownerId, int page, int size);

    /** Returns an owned study or throws StudyNotFoundException without exposing other owners. */
    Study getStudy(UUID ownerId, UUID studyId);

    /**
     * Creates a draft study for an authenticated researcher.
     * The caller must derive the owner ID from authenticated identity, never from
     * a client-supplied ownership field. The service enforces the RESEARCHER role
     * when invoked through its Spring-managed proxy.
     *
     * @param ownerId the authenticated researcher's identifier
     * @param title the nonblank study title, at most 255 characters
     * @param description the optional study description
     * @return the persisted draft study
     * @throws jakarta.validation.ConstraintViolationException if the study is invalid
     */
    Study createStudy(UUID ownerId, String title, String description);
}
