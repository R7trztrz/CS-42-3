package com.cs_42_3.surveyplatformbackend.eyetracking.service;

import com.cs_42_3.surveyplatformbackend.eyetracking.api.dto.CalibrationAttemptRequest;
import com.cs_42_3.surveyplatformbackend.eyetracking.api.dto.CalibrationAttemptResponse;
import com.cs_42_3.surveyplatformbackend.eyetracking.config.EyeTrackingProperties;
import com.cs_42_3.surveyplatformbackend.eyetracking.domain.CalibrationOutcome;
import com.cs_42_3.surveyplatformbackend.eyetracking.domain.CalibrationPointKind;
import com.cs_42_3.surveyplatformbackend.eyetracking.exception.EyeTrackingRequestException;
import com.cs_42_3.surveyplatformbackend.eyetracking.repository.CalibrationRecordRepository;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionPrincipal;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSession;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSessionPhase;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSessionStatus;
import com.cs_42_3.surveyplatformbackend.participation.exception.ParticipationException;
import com.cs_42_3.surveyplatformbackend.participation.repository.ParticipantSessionRepository;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantCalibrationLifecyclePort;
import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyClosedException;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Persists FR-49 calibration attempts and completes the M5 calibration phase.
 *
 * <p>Two writes exist. {@link #record} stores one finished attempt as the browser's
 * {@code CalibrationRecordSink} delivers it; it never changes the session phase. {@link #complete}
 * stores the attempt the participant continued with and, in the same transaction, calls
 * {@link ParticipantCalibrationLifecyclePort#completeCalibration}, so the M5 session reaches
 * BROWSING only together with its persisted calibration outcome.
 *
 * <p>Every outcome is accepted, including abandoned and camera-unavailable attempts: neither
 * quality nor camera availability may gate the participant flow (FR-49).
 */
@Service
@RequiredArgsConstructor
public class CalibrationRecordService {
    /**
     * Canonical serialization for replay detection. A dedicated default mapper keeps the digest
     * independent of the application's HTTP mapper configuration.
     */
    private static final JsonMapper CANONICAL = JsonMapper.builder().build();

    private final CalibrationRecordRepository records;
    private final ParticipantSessionRepository sessions;
    private final StudyRepository studies;
    private final ParticipantCalibrationLifecyclePort calibrationLifecycle;
    private final EyeTrackingProperties properties;
    private final Clock clock;

    /** Stores one finished attempt. Identical replays are acknowledged with the original row. */
    @Transactional
    public RecordResult record(
            ParticipantSessionPrincipal principal,
            UUID pathAttemptId,
            CalibrationAttemptRequest attempt
    ) {
        ValidatedAttempt validated = validate(principal, attempt);
        if (!attempt.attemptId().equals(pathAttemptId)) {
            throw EyeTrackingRequestException.invalid("attemptId does not match the request path");
        }
        lockRecordableSession(principal);
        return store(principal.sessionId(), validated, true);
    }

    /**
     * Stores the attempt the participant continued with and completes M5 calibration atomically.
     * Replaying the same completion is a no-op; completing with a different attempt is a conflict.
     */
    @Transactional
    public void complete(ParticipantSessionPrincipal principal, CalibrationAttemptRequest attempt) {
        ValidatedAttempt validated = validate(principal, attempt);
        UUID sessionId = principal.sessionId();
        // Takes the Study-then-session locks in M5's order before any M6 row is touched, and
        // rejects sessions that are not (or no longer) allowed to finish calibration.
        calibrationLifecycle.completeCalibration(sessionId);

        Optional<UUID> completedWith = records.findCompletedAttemptId(sessionId);
        if (completedWith.isPresent()) {
            boolean sameAttempt = completedWith.get().equals(attempt.attemptId())
                    && records.findAttempt(sessionId, attempt.attemptId())
                    .map(stored -> stored.requestHash().equals(validated.requestHash()))
                    .orElse(false);
            if (!sameAttempt) {
                throw ParticipationException.idempotencyConflict();
            }
            return;
        }
        // The final attempt is never refused for exceeding the per-session attempt cap: the
        // participant must always be able to leave calibration.
        store(sessionId, validated, false);
        records.insertCompletion(sessionId, attempt.attemptId());
    }

    private RecordResult store(UUID sessionId, ValidatedAttempt validated, boolean enforceCap) {
        CalibrationAttemptRequest attempt = validated.attempt();
        var existing = records.findAttempt(sessionId, attempt.attemptId());
        if (existing.isPresent()) {
            if (!existing.get().requestHash().equals(validated.requestHash())) {
                throw ParticipationException.idempotencyConflict();
            }
            return new RecordResult(false, new CalibrationAttemptResponse(
                    attempt.attemptId(),
                    existing.get().attemptNumber(),
                    existing.get().receivedAt()
            ));
        }
        if (enforceCap
                && records.countAttempts(sessionId) >= properties.maxCalibrationAttemptsPerSession()) {
            throw ParticipationException.invalidState();
        }
        Instant receivedAt = now();
        records.insertAttempt(
                sessionId,
                attempt,
                validated.startedAt(),
                validated.finishedAt(),
                validated.requestHash(),
                receivedAt
        );
        return new RecordResult(true, new CalibrationAttemptResponse(
                attempt.attemptId(),
                attempt.attemptNumber(),
                receivedAt
        ));
    }

    /**
     * Locks in M5's Study-then-session order and accepts records only for consented, active
     * eye-tracking sessions. Late deliveries after calibration finished are still accepted while
     * the session is in progress, because the browser retries failed deliveries.
     */
    private void lockRecordableSession(ParticipantSessionPrincipal principal) {
        Study study = studies.findByIdForParticipation(principal.studyId())
                .orElseThrow(ParticipationException::sessionNotFound);
        if (study.getStatus() == StudyStatus.CLOSED) {
            throw new StudyClosedException();
        }
        if (study.getStatus() != StudyStatus.COLLECTING) {
            throw ParticipationException.invalidState();
        }
        ParticipantSession session = sessions.findByIdForUpdate(principal.sessionId())
                .orElseThrow(ParticipationException::sessionNotFound);
        if (!session.getStudyId().equals(study.getId())) {
            throw ParticipationException.sessionNotFound();
        }
        if (session.getStatus() != ParticipantSessionStatus.IN_PROGRESS) {
            throw ParticipationException.terminated();
        }
        if (!study.isEyeTrackingEnabled()
                || session.getConsentedAt() == null
                || session.getPhase() == ParticipantSessionPhase.CONSENT) {
            throw ParticipationException.invalidState();
        }
    }

    private ValidatedAttempt validate(
            ParticipantSessionPrincipal principal,
            CalibrationAttemptRequest attempt
    ) {
        if (!attempt.sessionId().equals(principal.sessionId())) {
            throw EyeTrackingRequestException.invalid(
                    "sessionId does not match the authenticated participant session");
        }
        requireFinite(attempt.qualityThresholds().goodFraction(), "qualityThresholds.goodFraction");
        requireFinite(attempt.qualityThresholds().fairFraction(), "qualityThresholds.fairFraction");
        double good = attempt.qualityThresholds().goodFraction();
        double fair = attempt.qualityThresholds().fairFraction();
        if (good < 0 || fair <= good || fair > 1) {
            throw EyeTrackingRequestException.invalid(
                    "qualityThresholds must satisfy 0 <= goodFraction < fairFraction <= 1");
        }
        if (attempt.residualMedianPx() != null) {
            requireFinite(attempt.residualMedianPx(), "residualMedianPx");
        }
        if (attempt.validationMedianPx() != null) {
            requireFinite(attempt.validationMedianPx(), "validationMedianPx");
        }
        requireFinite(attempt.viewportWidth(), "viewportWidth");
        requireFinite(attempt.viewportHeight(), "viewportHeight");
        requireFinite(attempt.devicePixelRatio(), "devicePixelRatio");

        if (attempt.trackingAvailable() && attempt.unavailableReason() != null) {
            throw EyeTrackingRequestException.invalid(
                    "unavailableReason must be null when tracking is available");
        }
        if (attempt.outcome() == CalibrationOutcome.UNAVAILABLE
                && (attempt.trackingAvailable() || attempt.unavailableReason() == null)) {
            throw EyeTrackingRequestException.invalid(
                    "an UNAVAILABLE attempt must carry an unavailableReason and no tracking");
        }
        if (new HashSet<>(attempt.lowQualityReasons()).size() != attempt.lowQualityReasons().size()) {
            throw EyeTrackingRequestException.invalid("lowQualityReasons must not repeat a reason");
        }
        if (attempt.calibrationTargetsMeasured() > attempt.calibrationTargetsRequired()
                || attempt.validationTargetsMeasured() > attempt.validationTargetsRequired()) {
            throw EyeTrackingRequestException.invalid(
                    "measured targets must not exceed required targets");
        }

        Instant startedAt = EyeTrackingTimestamps.parse(attempt.startedAt(), "startedAt");
        Instant finishedAt = EyeTrackingTimestamps.parse(attempt.finishedAt(), "finishedAt");
        if (finishedAt.isBefore(startedAt)) {
            throw EyeTrackingRequestException.invalid("finishedAt must not be before startedAt");
        }
        validatePoints(attempt);
        return new ValidatedAttempt(
                attempt,
                startedAt,
                finishedAt,
                RequestDigest.sha256(CANONICAL.writeValueAsString(attempt))
        );
    }

    private void validatePoints(CalibrationAttemptRequest attempt) {
        Set<String> seen = new HashSet<>();
        for (int index = 0; index < attempt.pointResiduals().size(); index++) {
            CalibrationAttemptRequest.PointResidual point = attempt.pointResiduals().get(index);
            String field = "pointResiduals[" + index + "]";
            requireFinite(point.targetX(), field + ".targetX");
            requireFinite(point.targetY(), field + ".targetY");
            requireFinite(point.predictedX(), field + ".predictedX");
            requireFinite(point.predictedY(), field + ".predictedY");
            requireFinite(point.residualPx(), field + ".residualPx");
            int required = point.pointKind() == CalibrationPointKind.CALIBRATION
                    ? attempt.calibrationTargetsRequired()
                    : attempt.validationTargetsRequired();
            if (point.pointIndex() >= required) {
                throw EyeTrackingRequestException.invalid(
                        field + ".pointIndex is outside the attempt's target layout");
            }
            if (!seen.add(point.pointKind() + ":" + point.pointIndex())) {
                throw EyeTrackingRequestException.invalid(field + " repeats a measured target");
            }
            // Target centres are viewport CSS pixels inside the calibration viewport.
            if (point.targetX() < 0 || point.targetX() > attempt.viewportWidth()
                    || point.targetY() < 0 || point.targetY() > attempt.viewportHeight()) {
                throw EyeTrackingRequestException.invalid(
                        field + " target centre is outside the viewport");
            }
        }
    }

    private static void requireFinite(Double value, String field) {
        if (value == null || !Double.isFinite(value)) {
            throw EyeTrackingRequestException.invalid(field + " must be a finite number");
        }
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    /** Whether this call created the row, and what to acknowledge. */
    public record RecordResult(boolean created, CalibrationAttemptResponse response) {
    }

    private record ValidatedAttempt(
            CalibrationAttemptRequest attempt,
            Instant startedAt,
            Instant finishedAt,
            String requestHash
    ) {
    }
}
