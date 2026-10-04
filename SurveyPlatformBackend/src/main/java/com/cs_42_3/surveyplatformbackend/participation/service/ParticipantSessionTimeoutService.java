package com.cs_42_3.surveyplatformbackend.participation.service;

import com.cs_42_3.surveyplatformbackend.participation.config.ParticipationProperties;
import com.cs_42_3.surveyplatformbackend.participation.repository.ParticipantSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ParticipantSessionTimeoutService {
    private static final int BATCH_SIZE = 100;

    private final ParticipantSessionRepository sessions;
    private final ParticipantSessionTimeoutProcessor processor;
    private final ParticipationProperties properties;
    private final Clock clock;

    /** Returns the number of sessions ended by this scan. */
    @Transactional(readOnly = true)
    public List<ParticipantSessionRepository.TimeoutCandidate> findCandidates(Instant cutoff) {
        return sessions.findTimeoutCandidates(cutoff, PageRequest.of(0, BATCH_SIZE));
    }

    public int scanOnce() {
        Instant now = clock.instant().truncatedTo(ChronoUnit.MICROS);
        Instant cutoff = now.minus(properties.inactivityTimeout());
        int changed = 0;
        for (int batch = 0; batch < 100; batch++) {
            List<ParticipantSessionRepository.TimeoutCandidate> candidates = findCandidates(cutoff);
            if (candidates.isEmpty()) {
                break;
            }
            int batchChanged = 0;
            for (ParticipantSessionRepository.TimeoutCandidate candidate : candidates) {
                if (processor.process(
                        candidate.getStudyId(), candidate.getSessionId(), cutoff, now
                )) {
                    changed++;
                    batchChanged++;
                }
            }
            if (candidates.size() < BATCH_SIZE || batchChanged == 0) {
                break;
            }
        }
        return changed;
    }
}
