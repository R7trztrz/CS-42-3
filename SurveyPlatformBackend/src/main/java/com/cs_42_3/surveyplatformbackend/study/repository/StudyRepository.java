package com.cs_42_3.surveyplatformbackend.study.repository;

import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

/**
 * Provides persistence operations for studies.
 * FR-11 uses the inherited save method to persist a new draft study.
 * The service layer is responsible for supplying the authenticated owner.
 *
 * @author Simon Tian
 */
public interface StudyRepository extends JpaRepository<Study, UUID> {

    /**
     * Finds a study only when it belongs to the supplied owner.
     * <p>
     * Used by other modules (e.g. the questionnaire module) that scope their own
     * resources to one study and must not leak another researcher's study by ID.
     *
     * @param id study identifier
     * @param ownerId expected owner
     * @return the owned study, or an empty result
     */
    Optional<Study> findByIdAndOwnerId(UUID id, UUID ownerId);
}
