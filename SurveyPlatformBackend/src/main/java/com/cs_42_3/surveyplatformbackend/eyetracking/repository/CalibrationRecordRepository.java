package com.cs_42_3.surveyplatformbackend.eyetracking.repository;

import com.cs_42_3.surveyplatformbackend.eyetracking.api.dto.CalibrationAttemptRequest;
import com.cs_42_3.surveyplatformbackend.eyetracking.domain.CalibrationQuality;
import com.cs_42_3.surveyplatformbackend.eyetracking.domain.LowQualityReason;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * JDBC access to the FR-49 calibration tables. Callers hold the M5 session lock, so the
 * read-then-insert sequences here cannot race for one session.
 */
@Repository
@RequiredArgsConstructor
public class CalibrationRecordRepository {
    private static final String INSERT_ATTEMPT = """
            INSERT INTO calibration_attempts (
                session_id, attempt_id, schema_version, algorithm_version, attempt_number,
                outcome, tracking_available, unavailable_reason, quality, quality_basis,
                good_fraction, fair_fraction, min_samples_per_target,
                calibration_targets_measured, calibration_targets_required,
                validation_targets_measured, validation_targets_required,
                residual_median_px, validation_median_px,
                viewport_width, viewport_height, device_pixel_ratio,
                started_at, finished_at, request_hash, received_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
    private static final String INSERT_REASON = """
            INSERT INTO calibration_attempt_reasons (session_id, attempt_id, reason)
            VALUES (?, ?, ?)
            """;
    private static final String INSERT_RESIDUAL = """
            INSERT INTO calibration_point_residuals (
                session_id, attempt_id, point_kind, point_index, target_x, target_y,
                predicted_x, predicted_y, residual_px, sample_count
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbc;

    public Optional<StoredAttempt> findAttempt(UUID sessionId, UUID attemptId) {
        return jdbc.query(
                """
                        SELECT attempt_number, request_hash, received_at
                        FROM calibration_attempts
                        WHERE session_id = ? AND attempt_id = ?
                        """,
                (rs, row) -> new StoredAttempt(
                        rs.getInt("attempt_number"),
                        rs.getString("request_hash"),
                        instant(rs, "received_at")
                ),
                sessionId,
                attemptId
        ).stream().findFirst();
    }

    public int countAttempts(UUID sessionId) {
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM calibration_attempts WHERE session_id = ?",
                Integer.class,
                sessionId
        );
        return count == null ? 0 : count;
    }

    public void insertAttempt(
            UUID sessionId,
            CalibrationAttemptRequest attempt,
            Instant startedAt,
            Instant finishedAt,
            String requestHash,
            Instant receivedAt
    ) {
        jdbc.update(INSERT_ATTEMPT, statement -> {
            int index = 1;
            statement.setObject(index++, sessionId);
            statement.setObject(index++, attempt.attemptId());
            statement.setInt(index++, attempt.schemaVersion());
            statement.setString(index++, attempt.algorithmVersion());
            statement.setInt(index++, attempt.attemptNumber());
            statement.setString(index++, attempt.outcome().name());
            statement.setBoolean(index++, attempt.trackingAvailable());
            statement.setString(index++, attempt.unavailableReason() == null
                    ? null
                    : attempt.unavailableReason().name());
            statement.setString(index++, attempt.quality().name());
            statement.setString(index++, attempt.qualityBasis().name());
            statement.setDouble(index++, attempt.qualityThresholds().goodFraction());
            statement.setDouble(index++, attempt.qualityThresholds().fairFraction());
            statement.setInt(index++, attempt.minSamplesPerTarget());
            statement.setInt(index++, attempt.calibrationTargetsMeasured());
            statement.setInt(index++, attempt.calibrationTargetsRequired());
            statement.setInt(index++, attempt.validationTargetsMeasured());
            statement.setInt(index++, attempt.validationTargetsRequired());
            setNullableDouble(statement, index++, attempt.residualMedianPx());
            setNullableDouble(statement, index++, attempt.validationMedianPx());
            statement.setDouble(index++, attempt.viewportWidth());
            statement.setDouble(index++, attempt.viewportHeight());
            statement.setDouble(index++, attempt.devicePixelRatio());
            statement.setObject(index++, utc(startedAt));
            statement.setObject(index++, utc(finishedAt));
            statement.setString(index++, requestHash);
            statement.setObject(index, utc(receivedAt));
        });
        // lowQualityReasons as rows; the browser never repeats a reason (validated before here).
        List<LowQualityReason> reasons = attempt.lowQualityReasons();
        if (!reasons.isEmpty()) {
            jdbc.batchUpdate(INSERT_REASON, reasons, reasons.size(), (statement, reason) -> {
                statement.setObject(1, sessionId);
                statement.setObject(2, attempt.attemptId());
                statement.setString(3, reason.name());
            });
        }
        List<CalibrationAttemptRequest.PointResidual> points = attempt.pointResiduals();
        if (points.isEmpty()) {
            return;
        }
        jdbc.batchUpdate(INSERT_RESIDUAL, points, points.size(), (statement, point) -> {
            statement.setObject(1, sessionId);
            statement.setObject(2, attempt.attemptId());
            statement.setString(3, point.pointKind().name());
            statement.setInt(4, point.pointIndex());
            statement.setDouble(5, point.targetX());
            statement.setDouble(6, point.targetY());
            statement.setDouble(7, point.predictedX());
            statement.setDouble(8, point.predictedY());
            statement.setDouble(9, point.residualPx());
            statement.setInt(10, point.sampleCount());
        });
    }

    public Optional<UUID> findCompletedAttemptId(UUID sessionId) {
        return jdbc.query(
                "SELECT attempt_id FROM calibration_completions WHERE session_id = ?",
                (rs, row) -> rs.getObject("attempt_id", UUID.class),
                sessionId
        ).stream().findFirst();
    }

    public boolean hasCompletion(UUID sessionId) {
        return findCompletedAttemptId(sessionId).isPresent();
    }

    /**
     * The completion time is not stored here: M5's completeCalibration sets
     * participant_sessions.calibration_completed_at in the same transaction.
     */
    public void insertCompletion(UUID sessionId, UUID attemptId) {
        jdbc.update(
                "INSERT INTO calibration_completions (session_id, attempt_id) VALUES (?, ?)",
                sessionId,
                attemptId
        );
    }

    /**
     * Reads, for every attempt recorded for the session, what gaze events recorded under that
     * attempt must repeat. A session holds a handful of attempts, so one query covers them all.
     */
    public Map<UUID, TrackingCalibration> findTrackingCalibrations(UUID sessionId) {
        Map<UUID, Boolean> trackingAvailable = new HashMap<>();
        Map<UUID, CalibrationQuality> quality = new HashMap<>();
        Map<UUID, Set<LowQualityReason>> reasons = new HashMap<>();
        jdbc.query(
                """
                        SELECT a.attempt_id, a.tracking_available, a.quality, r.reason
                        FROM calibration_attempts a
                        LEFT JOIN calibration_attempt_reasons r
                            ON r.session_id = a.session_id AND r.attempt_id = a.attempt_id
                        WHERE a.session_id = ?
                        """,
                (ResultSet rs) -> {
                    UUID attemptId = rs.getObject("attempt_id", UUID.class);
                    trackingAvailable.put(attemptId, rs.getBoolean("tracking_available"));
                    quality.put(attemptId, CalibrationQuality.valueOf(rs.getString("quality")));
                    Set<LowQualityReason> attemptReasons = reasons.computeIfAbsent(
                            attemptId, ignored -> EnumSet.noneOf(LowQualityReason.class));
                    String reason = rs.getString("reason");
                    if (reason != null) {
                        attemptReasons.add(LowQualityReason.valueOf(reason));
                    }
                },
                sessionId
        );
        Map<UUID, TrackingCalibration> found = new HashMap<>();
        trackingAvailable.forEach((attemptId, available) -> found.put(attemptId, new TrackingCalibration(
                available,
                quality.get(attemptId),
                Collections.unmodifiableSet(reasons.get(attemptId))
        )));
        return found;
    }

    private static void setNullableDouble(PreparedStatement statement, int index, Double value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.DOUBLE);
        } else {
            statement.setDouble(index, value);
        }
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, OffsetDateTime.class).toInstant();
    }

    /** What an idempotent replay is compared against. */
    public record StoredAttempt(int attemptNumber, String requestHash, Instant receivedAt) {
    }

    /**
     * The calibration identity every gaze event recorded under an attempt must repeat. The
     * reasons are stored one per row, so they compare as a set rather than in upload order.
     */
    public record TrackingCalibration(
            boolean trackingAvailable,
            CalibrationQuality quality,
            Set<LowQualityReason> lowQualityReasons
    ) {
    }
}
