package com.cs_42_3.surveyplatformbackend.study.service.implementation;

import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.cs_42_3.surveyplatformbackend.study.service.StudyService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyNotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyUpdate;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyNotEditableException;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyVersionConflictException;
import org.springframework.dao.OptimisticLockingFailureException;
import jakarta.persistence.OptimisticLockException;
import java.util.Objects;

/**
 * Implements study creation and owner-scoped queries within transactions.
 *
 * @author Simon Tian
 */
@Service
@RequiredArgsConstructor
public class StudyServiceImpl implements StudyService {

    private final StudyRepository studyRepository;
    private final Validator validator;

    @Override
    @Transactional
    @PreAuthorize("hasRole('RESEARCHER')")
    public Study updateStudy(UUID ownerId, UUID studyId, StudyUpdate update) {
        Study study = studyRepository.findByIdAndOwnerId(studyId, ownerId)
                .orElseThrow(StudyNotFoundException::new);

        if (study.getStatus() != StudyStatus.DRAFT) {
            throw new StudyNotEditableException();
        }

        if (!Objects.equals(study.getLockVersion(), update.version())) {
            throw new StudyVersionConflictException();
        }

        study.applyUpdate(update);

        Set<ConstraintViolation<Study>> violations = validator.validate(study);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        try {
            // Flush inside this transaction to obtain timestamps/version and translate races.
            // JPA dirty checking updates the managed entity; never assign the client version.
            studyRepository.flush();
        } catch (OptimisticLockingFailureException | OptimisticLockException exception) {
            throw new StudyVersionConflictException(exception);
        }

        return study;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('RESEARCHER')")
    public Page<Study> listStudies(UUID ownerId, int page, int size) {
        // Reject invalid bounds before constructing a pageable or querying storage.
        if (page < 0 || size < 1 || size > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page must be nonnegative and size must be between 1 and 100.");
        }
        var pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id")));
        return studyRepository.findAllByOwnerId(ownerId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('RESEARCHER')")
    public Study getStudy(UUID ownerId, UUID studyId) {
        // Missing and foreign-owned studies deliberately have the same response.
        return studyRepository.findByIdAndOwnerId(studyId, ownerId)
                .orElseThrow(StudyNotFoundException::new);
    }

    @Override
    @Transactional
    @PreAuthorize("hasRole('RESEARCHER')")
    public Study createStudy(UUID ownerId, String title, String description) {
        Study study = new Study(ownerId, title, description);

        // Reuse entity constraints before invoking persistence.
        Set<ConstraintViolation<Study>> violations = validator.validate(study);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        return studyRepository.save(study);
    }
}
