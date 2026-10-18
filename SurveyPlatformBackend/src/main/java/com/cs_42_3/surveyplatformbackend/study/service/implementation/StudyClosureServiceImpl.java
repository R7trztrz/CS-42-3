package com.cs_42_3.surveyplatformbackend.study.service.implementation;

import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantSessionLifecyclePort;
import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.exception.*;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.cs_42_3.surveyplatformbackend.study.service.StudyClosureService;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.Objects;
import java.util.UUID;

/**
 * Serializes closure with participant writes using the shared Study-before-session lock order.
 *
 * @author Simon Tian
 */
@Service
@RequiredArgsConstructor
public class StudyClosureServiceImpl implements StudyClosureService {
    private final StudyRepository studies;
    private final ParticipantSessionLifecyclePort sessionLifecycle;
    private final Clock clock;

    @Override
    @Transactional
    @PreAuthorize("hasRole('RESEARCHER')")
    public Study close(UUID ownerId, UUID studyId, long expectedVersion) {
        Study study = studies.findOwnedByIdForUpdate(studyId, ownerId)
                .orElseThrow(StudyNotFoundException::new);
        // Repeated commands do not re-abandon sessions, rewrite timestamps or advance the version.
        if (study.getStatus() == StudyStatus.CLOSED) {
            return study;
        }
        if (study.getStatus() != StudyStatus.COLLECTING) {
            throw new StudyNotClosableException();
        }
        if (!Objects.equals(study.getLockVersion(), expectedVersion)) {
            throw new StudyVersionConflictException();
        }
        study.close(clock.instant().truncatedTo(ChronoUnit.MICROS));
        try {
            // M5 joins this transaction; any session failure also rolls back the Study transition.
            sessionLifecycle.abandonActiveSessionsForStudy(studyId, study.getClosedAt());
            studies.flush();
        } catch (OptimisticLockingFailureException | OptimisticLockException exception) {
            throw new StudyVersionConflictException(exception);
        }
        return study;
    }
}
