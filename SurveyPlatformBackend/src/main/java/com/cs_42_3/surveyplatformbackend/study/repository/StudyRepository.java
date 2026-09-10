package com.cs_42_3.surveyplatformbackend.study.repository;

import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/**
 * Provides persistence operations for studies.
 * FR-11 uses the inherited save method to persist a new draft study.
 * The service layer is responsible for supplying the authenticated owner.
 *
 * @author Simon Tian
 */
public interface StudyRepository extends JpaRepository<Study, UUID> {
}
