package com.cs_42_3.surveyplatformbackend.participation.repository;

import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantQuestionnaireStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParticipantQuestionnaireStepRepository
        extends JpaRepository<ParticipantQuestionnaireStep, UUID> {
    Optional<ParticipantQuestionnaireStep> findBySessionIdAndIdempotencyKey(
            UUID sessionId,
            UUID idempotencyKey
    );

    List<ParticipantQuestionnaireStep> findAllBySessionIdOrderBySequenceNumber(UUID sessionId);

    long countBySessionId(UUID sessionId);
}
