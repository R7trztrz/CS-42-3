package com.cs_42_3.surveyplatformbackend.study.service.implementation;

import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.cs_42_3.surveyplatformbackend.study.service.StudyService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;

/**
 * Implements FR-11 study creation within a transaction.
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
