package com.cs_42_3.surveyplatformbackend.calibration.repository;

import com.cs_42_3.surveyplatformbackend.calibration.domain.CalibrationRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Provides persistence operations for calibration attempts.
 * The service layer is responsible for supplying the authenticated session.
 *
 * @author Shuo Gu
 */
public interface CalibrationRecordRepository extends JpaRepository<CalibrationRecord, UUID> {

    /**
     * Finds an already-ingested attempt, so that a retried submission is not stored twice.
     *
     * @param sessionId the participant session
     * @param attemptNumber the attempt's ordinal within the session
     * @return the stored attempt, if the client already submitted it
     */
    Optional<CalibrationRecord> findBySessionIdAndAttemptNumber(UUID sessionId, int attemptNumber);

    /**
     * Lists a session's attempts in the order they were made. FR-51 uses this to roll up
     * the session's eye-tracking availability and quality.
     *
     * @param sessionId the participant session
     * @return the session's attempts, earliest first
     */
    List<CalibrationRecord> findBySessionIdOrderByAttemptNumberAsc(UUID sessionId);
}
