package com.cs_42_3.surveyplatformbackend.participation.repository;

import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ParticipantAnswerRepository extends JpaRepository<ParticipantAnswer, UUID> {
    Optional<ParticipantAnswer> findByStepId(UUID stepId);

    List<ParticipantAnswer> findAllByStepIdIn(Collection<UUID> stepIds);
}
