package com.cs_42_3.surveyplatformbackend.study.service.implementation;

import com.cs_42_3.surveyplatformbackend.study.exception.*;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyDeletionRepository;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.cs_42_3.surveyplatformbackend.study.service.StudyDeletionService;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.Objects;
import java.util.UUID;

/**
 * Serializes permanent deletion with editors and collectors using the Study-first lock order.
 *
 * @author Simon Tian
 */
@Service
@RequiredArgsConstructor
public class StudyDeletionServiceImpl implements StudyDeletionService {
    private final StudyRepository studies;
    private final StudyDeletionRepository dependencies;
    private final Clock clock;

    @Override
    @Transactional
    @PreAuthorize("hasRole('RESEARCHER')")
    public void delete(UUID ownerId, UUID studyId, long expectedVersion) {
        var study = studies.findOwnedByIdForUpdate(studyId, ownerId)
                .orElseThrow(StudyNotFoundException::new);
        if (!Objects.equals(study.getLockVersion(), expectedVersion)) {
            throw new StudyVersionConflictException();
        }
        try {
            dependencies.removeDependencies(studyId, clock.instant());
            studies.delete(study);
            // Detect constraints and optimistic conflicts before returning a successful response.
            studies.flush();
        } catch (OptimisticLockingFailureException | OptimisticLockException exception) {
            throw new StudyVersionConflictException(exception);
        }
    }
}
