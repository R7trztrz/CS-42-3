package com.cs_42_3.surveyplatformbackend.participation.service;

import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSession;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSessionPhase;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSessionStatus;
import com.cs_42_3.surveyplatformbackend.participation.exception.ParticipationException;
import com.cs_42_3.surveyplatformbackend.participation.repository.ParticipantSessionRepository;
import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyClosedException;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/** M5 implementation of the transaction boundary consumed by future M6 batches. */
@Service
@RequiredArgsConstructor
public class ParticipantCollectionService implements
        ParticipantCollectionPolicy,
        ParticipantCollectionActivityPort {

    private final ParticipantSessionRepository sessions;
    private final StudyRepository studies;
    private final Clock clock;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void assertCollectionAllowed(UUID sessionId, CollectionKind kind) {
        LockedCollectionContext context = lockContext(sessionId);
        assertAllowed(context.study(), context.session(), kind);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void recordAcceptedBatch(UUID sessionId, CollectionKind kind) {
        LockedCollectionContext context = lockContext(sessionId);
        assertAllowed(context.study(), context.session(), kind);
        context.session().recordCollectionActivity(now());
    }

    private LockedCollectionContext lockContext(UUID sessionId) {
        Objects.requireNonNull(sessionId, "sessionId");
        UUID studyId = sessions.findStudyIdById(sessionId)
                .orElseThrow(ParticipationException::sessionNotFound);
        Study study = studies.findByIdForParticipation(studyId)
                .orElseThrow(ParticipationException::sessionNotFound);
        if (study.getStatus() == StudyStatus.CLOSED) {
            throw new StudyClosedException();
        }
        if (study.getStatus() != StudyStatus.COLLECTING) {
            throw ParticipationException.invalidState();
        }
        ParticipantSession session = sessions.findByIdForUpdate(sessionId)
                .orElseThrow(ParticipationException::sessionNotFound);
        if (!session.getStudyId().equals(study.getId())) {
            throw ParticipationException.sessionNotFound();
        }
        return new LockedCollectionContext(study, session);
    }

    private void assertAllowed(
            Study study,
            ParticipantSession session,
            CollectionKind kind
    ) {
        Objects.requireNonNull(kind, "kind");
        if (session.getStatus() != ParticipantSessionStatus.IN_PROGRESS) {
            throw ParticipationException.terminated();
        }
        if (session.getConsentedAt() == null
                || (session.getPhase() != ParticipantSessionPhase.BROWSING
                && session.getPhase() != ParticipantSessionPhase.QUESTIONNAIRE)) {
            throw ParticipationException.invalidState();
        }
        if (kind == CollectionKind.GAZE && !study.isEyeTrackingEnabled()) {
            throw ParticipationException.invalidState();
        }
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    private record LockedCollectionContext(Study study, ParticipantSession session) {
    }
}
