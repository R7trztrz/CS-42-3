package com.cs_42_3.surveyplatformbackend.study.repository;

import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Provides persistence operations for studies.
 * FR-11 uses the inherited save method to persist a new draft study.
 * The service layer is responsible for supplying the authenticated owner.
 *
 * @author Simon Tian
 */
public interface StudyRepository extends JpaRepository<Study, UUID> {
    /** Looks up the unguessable public entry token without exposing owner identity. */
    Optional<Study> findByParticipationToken(String token);

    /** Serializes feed saves with study lifecycle changes in the same transaction. */
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from Study s where s.id = :id and s.ownerId = :ownerId")
    Optional<Study> findOwnedByIdForUpdate(@org.springframework.data.repository.query.Param("id") UUID id,
                                         @org.springframework.data.repository.query.Param("ownerId") UUID ownerId);
    /** Retrieves only studies owned by the requested researcher. */
    Page<Study> findAllByOwnerId(UUID ownerId, Pageable pageable);

    /** Combines resource lookup and ownership filtering in one query. */
    Optional<Study> findByIdAndOwnerId(UUID id, UUID ownerId);
}
