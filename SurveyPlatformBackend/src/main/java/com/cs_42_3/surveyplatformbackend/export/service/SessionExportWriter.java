package com.cs_42_3.surveyplatformbackend.export.service;

import com.cs_42_3.surveyplatformbackend.export.domain.ExportSessionRecord;
import com.cs_42_3.surveyplatformbackend.export.exception.ExportException;
import com.cs_42_3.surveyplatformbackend.export.storage.ExportArchive;
import com.cs_42_3.surveyplatformbackend.eyetracking.service.EyeTrackingSummaryReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.sql.*;
import java.time.Instant;
import java.util.UUID;

/**
 * Reads scoped session metadata independently of the selected questionnaire or gaze category.
 *
 * @author Simon Tian
 */
@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY, readOnly = true)
public class SessionExportWriter {
    private final JdbcTemplate jdbc;
    private final EyeTrackingSummaryReadService eyeTracking;
    private final ObjectMapper mapper;

    public ExportSessionRecord read(UUID studyId, UUID sessionId) {
        return jdbc.query("""
                SELECT p.anonymous_participant_id, p.status, p.phase, p.abandonment_reason,
                    p.entered_at, p.consented_at, p.calibration_completed_at, p.browsing_completed_at,
                    p.completed_at, p.abandoned_at, p.last_activity_at,
                    s.questionnaire_enabled, s.eye_tracking_enabled
                FROM participant_sessions p JOIN studies s ON s.id = p.study_id
                WHERE p.study_id = ? AND p.id = ?
                """, (rs, n) -> new ExportSessionRecord(studyId, sessionId, rs.getObject("anonymous_participant_id", UUID.class),
                rs.getString("status"), rs.getString("phase"), rs.getString("abandonment_reason"),
                instant(rs, "entered_at"), instant(rs, "consented_at"), instant(rs, "calibration_completed_at"),
                instant(rs, "browsing_completed_at"), instant(rs, "completed_at"), instant(rs, "abandoned_at"),
                instant(rs, "last_activity_at"), rs.getBoolean("questionnaire_enabled"), rs.getBoolean("eye_tracking_enabled")),
                studyId, sessionId).stream().findFirst().orElseThrow(ExportException::invalidData);
    }

    public void write(ExportArchive archive, ExportSessionRecord session, Instant snapshotAt) throws IOException {
        var quality = eyeTracking.getSummary(session.studyId(), session.sessionId());
        var csv = archive.csv("sessions.csv", "study_id", "session_id", "anonymous_participant_id", "status",
                "phase", "abandonment_reason", "entered_at", "consented_at", "calibration_completed_at",
                "browsing_completed_at", "completed_at", "abandoned_at", "last_activity_at",
                "questionnaire_enabled", "eye_tracking_enabled", "calibration_state", "tracking_available",
                "calibration_quality", "quality_basis", "unavailable_reason", "low_quality_reasons", "snapshot_at");
        csv.printRecord(session.studyId(), session.sessionId(), session.anonymousId(), session.status(), session.phase(),
                session.reason(), session.entered(), session.consented(), session.calibrated(), session.browsed(),
                session.completed(), session.abandoned(), session.activity(), session.questionnaireEnabled(),
                quality.enabled(), quality.calibrationState(), quality.trackingAvailable(), quality.quality(),
                quality.qualityBasis(), quality.unavailableReason(), mapper.writeValueAsString(quality.lowQualityReasons()), snapshotAt);
        archive.finish(csv);
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
