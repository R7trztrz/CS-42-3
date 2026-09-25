package com.cs_42_3.surveyplatformbackend.study.repository;

import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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

    Optional<Study> findByIdAndOwnerId(UUID studyId, UUID ownerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s
            from Study s
            where s.id = :studyId
              and s.ownerId = :ownerId
            """)
    Optional<Study> findOwnedStudyForQuestionnaireUpdate(
            @Param("studyId") UUID studyId,
            @Param("ownerId") UUID ownerId
    );
}
