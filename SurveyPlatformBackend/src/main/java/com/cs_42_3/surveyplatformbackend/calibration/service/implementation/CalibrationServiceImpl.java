package com.cs_42_3.surveyplatformbackend.calibration.service.implementation;

import com.cs_42_3.surveyplatformbackend.calibration.api.dto.PointResidualRequest;
import com.cs_42_3.surveyplatformbackend.calibration.api.dto.SubmitCalibrationRequest;
import com.cs_42_3.surveyplatformbackend.calibration.domain.CalibrationOutcome;
import com.cs_42_3.surveyplatformbackend.calibration.domain.CalibrationPointKind;
import com.cs_42_3.surveyplatformbackend.calibration.domain.CalibrationRecord;
import com.cs_42_3.surveyplatformbackend.calibration.repository.CalibrationRecordRepository;
import com.cs_42_3.surveyplatformbackend.calibration.service.CalibrationService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Implements FR-49 calibration ingest within a transaction.
 *
 * @author Shuo Gu
 */
@Service
@RequiredArgsConstructor
public class CalibrationServiceImpl implements CalibrationService {

    private final CalibrationRecordRepository calibrationRecordRepository;
    private final Validator validator;

    @Override
    @Transactional
    public SubmissionResult submitAttempt(UUID sessionId, SubmitCalibrationRequest request) {
        checkConsistency(request);

        Optional<CalibrationRecord> existing = calibrationRecordRepository
                .findBySessionIdAndAttemptNumber(sessionId, request.attemptNumber());
        if (existing.isPresent()) {
            return new SubmissionResult(existing.get(), false);
        }

        CalibrationRecord record = buildRecord(sessionId, request);

        // Reuse entity constraints before invoking persistence.
        Set<ConstraintViolation<CalibrationRecord>> violations = validator.validate(record);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }

        try {
            return new SubmissionResult(calibrationRecordRepository.saveAndFlush(record), true);
        } catch (DataIntegrityViolationException exception) {
            // A concurrent submission of the same attempt won the unique constraint; return theirs.
            return calibrationRecordRepository
                    .findBySessionIdAndAttemptNumber(sessionId, request.attemptNumber())
                    .map(stored -> new SubmissionResult(stored, false))
                    .orElseThrow(() -> exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CalibrationRecord> findAttempts(UUID sessionId) {
        return calibrationRecordRepository.findBySessionIdOrderByAttemptNumberAsc(sessionId);
    }

    private CalibrationRecord buildRecord(UUID sessionId, SubmitCalibrationRequest request) {
        CalibrationRecord record = new CalibrationRecord(sessionId, request.attemptNumber(),
                request.outcome(), request.unavailableReason(), request.residualMedianPx(),
                request.validationMedianPx(), request.viewportWidth(), request.viewportHeight(),
                request.devicePixelRatio(), request.startedAt(), request.finishedAt());

        for (PointResidualRequest point : request.pointResiduals()) {
            record.addPointResidual(point.pointKind(), point.pointIndex(), point.targetX(),
                    point.targetY(), point.predictedX(), point.predictedY(), point.residualPx(),
                    point.sampleCount());
        }
        return record;
    }

    // Cross-field rules that Bean Validation on a record cannot express on its own.
    // These mirror the CHECK constraints in the calibration migration so that a bad payload
    // fails with a clear message instead of a database error.
    private void checkConsistency(SubmitCalibrationRequest request) {
        boolean unavailable = request.outcome() == CalibrationOutcome.UNAVAILABLE;

        if (unavailable != (request.unavailableReason() != null)) {
            throw new IllegalArgumentException(
                    "unavailableReason is required when outcome is UNAVAILABLE and must be absent otherwise.");
        }
        if (request.finishedAt().isBefore(request.startedAt())) {
            throw new IllegalArgumentException("finishedAt must not precede startedAt.");
        }
        if (unavailable && !request.pointResiduals().isEmpty()) {
            throw new IllegalArgumentException(
                    "An UNAVAILABLE attempt never started, so it must carry no point residuals.");
        }
        if (unavailable && (request.residualMedianPx() != null || request.validationMedianPx() != null)) {
            throw new IllegalArgumentException(
                    "An UNAVAILABLE attempt must carry no residual medians.");
        }
        if (request.outcome() == CalibrationOutcome.COMPLETED) {
            if (request.residualMedianPx() == null) {
                throw new IllegalArgumentException(
                        "A COMPLETED attempt must report residualMedianPx.");
            }
            boolean hasCalibrationTarget = request.pointResiduals().stream()
                    .anyMatch(point -> point.pointKind() == CalibrationPointKind.CALIBRATION);
            if (!hasCalibrationTarget) {
                throw new IllegalArgumentException(
                        "A COMPLETED attempt must report at least one CALIBRATION point residual.");
            }
        }
        if (request.validationMedianPx() != null && request.pointResiduals().stream()
                .noneMatch(point -> point.pointKind() == CalibrationPointKind.VALIDATION)) {
            throw new IllegalArgumentException(
                    "validationMedianPx was reported without any VALIDATION point residual.");
        }
        checkNoDuplicateTargets(request);
    }

    private void checkNoDuplicateTargets(SubmitCalibrationRequest request) {
        Set<String> seen = new HashSet<>();
        for (PointResidualRequest point : request.pointResiduals()) {
            if (!seen.add(point.pointKind() + "#" + point.pointIndex())) {
                throw new IllegalArgumentException(
                        "Duplicate point residual for " + point.pointKind() + " index " + point.pointIndex() + ".");
            }
        }
    }
}
