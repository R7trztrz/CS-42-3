package com.cs_42_3.surveyplatformbackend.participation.repository;

import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ParticipantSessionRepository extends JpaRepository<ParticipantSession, UUID> {
    Optional<ParticipantSession> findBySessionTokenHash(String sessionTokenHash);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from ParticipantSession s where s.id = :sessionId")
    Optional<ParticipantSession> findByIdForUpdate(@Param("sessionId") UUID sessionId);
}
