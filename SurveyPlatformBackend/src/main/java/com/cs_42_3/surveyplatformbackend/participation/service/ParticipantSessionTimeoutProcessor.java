package com.cs_42_3.surveyplatformbackend.participation.service;

import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSession;
import com.cs_42_3.surveyplatformbackend.participation.repository.ParticipantSessionRepository;
import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParticipantSessionTimeoutProcessor {
    private final StudyRepository studies;
    private final ParticipantSessionRepository sessions;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean process(UUID studyId, UUID sessionId, Instant cutoff, Instant now) {
        Study study = studies.findByIdForParticipation(studyId).orElse(null);
        if (study == null) {
            return false;
        }
        ParticipantSession session = sessions.findByIdForUpdate(sessionId).orElse(null);
        if (session == null || !session.getStudyId().equals(studyId)) {
            return false;
        }
        if (study.getStatus() == StudyStatus.CLOSED) {
            return session.abandonForStudyClosure(now);
        }
        if (study.getStatus() != StudyStatus.COLLECTING) {
            return false;
        }
        return session.abandonIfInactive(cutoff, now);
    }
}
