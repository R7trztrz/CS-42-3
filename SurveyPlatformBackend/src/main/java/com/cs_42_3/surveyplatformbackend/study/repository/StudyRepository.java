package com.cs_42_3.surveyplatformbackend.study.repository;

import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

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

    /** Holds a shared lifecycle lock while a participant session is created. */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select s from Study s where s.participationToken = :token")
    Optional<Study> findByParticipationTokenForParticipation(
            @Param("token") String token
    );

    /** Uses the same Study-before-session lock order for participant writes. */
    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("select s from Study s where s.id = :studyId")
    Optional<Study> findByIdForParticipation(
            @Param("studyId") UUID studyId
    );

    /** Serializes feed saves with study lifecycle changes in the same transaction. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Study s where s.id = :id and s.ownerId = :ownerId")
    Optional<Study> findOwnedByIdForUpdate(
            @Param("id") UUID id,
            @Param("ownerId") UUID ownerId
    );

    /** Retrieves only studies owned by the requested researcher. */
    Page<Study> findAllByOwnerId(UUID ownerId, Pageable pageable);

    /** Combines resource lookup and ownership filtering in one query. */
    Optional<Study> findByIdAndOwnerId(UUID id, UUID ownerId);

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select s
            from Study s
            where s.id in :studyIds
            order by s.id
            """)
    List<Study> lockAllByIdsForQuestionReferenceChange(
            @Param("studyIds") Collection<UUID> studyIds
    );
}
