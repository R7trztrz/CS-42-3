package com.cs_42_3.surveyplatformbackend.eyetracking.repository;

import com.cs_42_3.surveyplatformbackend.eyetracking.api.dto.EyeTrackingSessionSummaryResponse;
import com.cs_42_3.surveyplatformbackend.eyetracking.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Projects the existing M6 quality view without loading gaze events or calibration point residuals.
 * The view uses the completion-selected attempt rather than the latest delivery.
 *
 * @author Simon Tian
 */
@Repository
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY, readOnly = true)
public class EyeTrackingSummaryQueryRepository {
    private final JdbcTemplate jdbc;

    public Optional<EyeTrackingSessionSummaryResponse> find(UUID studyId, UUID sessionId) {
        return jdbc.query("""
                SELECT eye_tracking_enabled, calibration_attempt_count, completed_attempt_id,
                       tracking_available, unavailable_reason, calibration_quality,
                       calibration_quality_basis, calibration_low_quality_reasons
                FROM eye_tracking_session_quality WHERE study_id = ? AND session_id = ?
                """, (rs, row) -> {
            boolean enabled = rs.getBoolean("eye_tracking_enabled");
            long attempts = rs.getLong("calibration_attempt_count");
            UUID completedAttempt = rs.getObject("completed_attempt_id", UUID.class);
            EyeTrackingCalibrationState state = !enabled ? EyeTrackingCalibrationState.DISABLED
                    : completedAttempt != null ? EyeTrackingCalibrationState.RECORDED
                    : attempts > 0 ? EyeTrackingCalibrationState.IN_PROGRESS : EyeTrackingCalibrationState.NOT_STARTED;
            // Never substitute a provisional attempt, a good grade or false availability for missing final data.
            if (!enabled || completedAttempt == null) {
                return new EyeTrackingSessionSummaryResponse(enabled, state, attempts, null, null,
                        null, null, null, List.of());
            }
            String reasons = rs.getString("calibration_low_quality_reasons");
            return new EyeTrackingSessionSummaryResponse(true, state, attempts, completedAttempt,
                    rs.getObject("tracking_available", Boolean.class),
                    nullableEnum(EyeTrackingUnavailableReason.class, rs.getString("unavailable_reason")),
                    nullableEnum(CalibrationQuality.class, rs.getString("calibration_quality")),
                    nullableEnum(CalibrationQualityBasis.class, rs.getString("calibration_quality_basis")),
                    reasons == null || reasons.isBlank() ? List.of()
                            : Arrays.stream(reasons.split(";")).map(LowQualityReason::valueOf).toList());
        }, studyId, sessionId).stream().findFirst();
    }

    private static <E extends Enum<E>> E nullableEnum(Class<E> type, String value) {
        return value == null ? null : Enum.valueOf(type, value);
    }
}
