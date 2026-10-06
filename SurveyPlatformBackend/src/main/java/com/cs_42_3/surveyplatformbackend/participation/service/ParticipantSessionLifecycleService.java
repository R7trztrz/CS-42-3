package com.cs_42_3.surveyplatformbackend.participation.service;

import com.cs_42_3.surveyplatformbackend.participation.repository.ParticipantSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParticipantSessionLifecycleService implements ParticipantSessionLifecyclePort {
    private final ParticipantSessionRepository sessions;

    /** Joins the caller's Study-close transaction after that caller has locked the Study. */
    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void abandonActiveSessionsForStudy(UUID studyId, Instant closedAt) {
        sessions.abandonActiveForStudy(
                Objects.requireNonNull(studyId),
                Objects.requireNonNull(closedAt).truncatedTo(ChronoUnit.MICROS)
        );
    }
}
