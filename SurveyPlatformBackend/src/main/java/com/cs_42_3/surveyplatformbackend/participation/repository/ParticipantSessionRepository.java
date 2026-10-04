package com.cs_42_3.surveyplatformbackend.participation.repository;

import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParticipantSessionRepository extends JpaRepository<ParticipantSession, UUID> {
    Optional<ParticipantSession> findBySessionTokenHash(String sessionTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ParticipantSession s where s.id = :sessionId")
    Optional<ParticipantSession> findByIdForUpdate(@Param("sessionId") UUID sessionId);

    @Query("""
            select s.id as sessionId, s.studyId as studyId
            from ParticipantSession s
            where s.status = com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSessionStatus.IN_PROGRESS
              and s.lastActivityAt < :cutoff
            order by s.lastActivityAt, s.id
            """)
    List<TimeoutCandidate> findTimeoutCandidates(
            @Param("cutoff") Instant cutoff,
            Pageable pageable
    );

    @Modifying(flushAutomatically = true)
    @Query(value = """
            UPDATE participant_sessions
            SET status = 'ABANDONED',
                phase = 'FINISHED',
                abandonment_reason = 'STUDY_CLOSED',
                current_question_item_id = NULL,
                questionnaire_ready_at = NULL,
                abandoned_at = :closedAt,
                last_activity_at = :closedAt,
                updated_at = :closedAt,
                lock_version = lock_version + 1
            WHERE study_id = :studyId
              AND status = 'IN_PROGRESS'
            """, nativeQuery = true)
    int abandonActiveForStudy(
            @Param("studyId") UUID studyId,
            @Param("closedAt") Instant closedAt
    );

    interface TimeoutCandidate {
        UUID getSessionId();

        UUID getStudyId();
    }
}
