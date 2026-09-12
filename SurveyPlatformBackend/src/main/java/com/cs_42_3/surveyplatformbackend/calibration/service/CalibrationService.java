package com.cs_42_3.surveyplatformbackend.calibration.service;

import com.cs_42_3.surveyplatformbackend.calibration.api.dto.SubmitCalibrationRequest;
import com.cs_42_3.surveyplatformbackend.calibration.domain.CalibrationRecord;

import java.util.List;
import java.util.UUID;

/**
 * Defines calibration record operations for FR-49.
 *
 * @author Shuo Gu
 */
public interface CalibrationService {

    /**
     * Stores a finished calibration attempt for an authenticated participant session.
     *
     * <p>The operation is idempotent on (session, attempt number): a client that retries after a
     * network failure receives the attempt already stored rather than creating a duplicate, as
     * NFR-21 requires.
     *
     * @param sessionId the authenticated participant session
     * @param request the finished attempt
     * @return the stored attempt and whether this call created it
     * @throws IllegalArgumentException if the request is internally inconsistent, for example an
     *     UNAVAILABLE outcome carrying residuals
     */
    SubmissionResult submitAttempt(UUID sessionId, SubmitCalibrationRequest request);

    /**
     * Lists a session's attempts in the order they were made, for the FR-51 quality rollup.
     *
     * @param sessionId the participant session
     * @return the session's attempts, earliest first
     */
    List<CalibrationRecord> findAttempts(UUID sessionId);

    /**
     * The stored attempt together with whether this submission created it.
     *
     * @param record the stored attempt
     * @param created true when this call inserted the attempt, false when it already existed
     */
    record SubmissionResult(CalibrationRecord record, boolean created) {
    }
}
