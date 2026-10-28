package com.cs_42_3.surveyplatformbackend.export.service;

import com.cs_42_3.surveyplatformbackend.export.domain.ExportSessionRecord;
import com.cs_42_3.surveyplatformbackend.export.exception.ExportException;
import com.cs_42_3.surveyplatformbackend.export.storage.ExportArchive;
import com.cs_42_3.surveyplatformbackend.eyetracking.domain.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.ObjectMapper;
import java.io.*;
import java.sql.*;
import java.util.*;

/**
 * Streams raw gaze records and their exact calibration references without filtering predictions.
 *
 * @author Simon Tian
 */
@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY, readOnly = true)
public class GazeExportWriter {
    private static final String[] ATTEMPT_COLUMNS = {
            "attempt_id", "schema_version", "algorithm_version", "attempt_number", "outcome", "tracking_available",
            "unavailable_reason", "quality", "quality_basis", "good_fraction", "fair_fraction", "min_samples_per_target",
            "calibration_targets_measured", "calibration_targets_required", "validation_targets_measured",
            "validation_targets_required", "residual_median_px", "validation_median_px", "viewport_width",
            "viewport_height", "device_pixel_ratio", "started_at", "finished_at", "received_at"
    };
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;

    public String write(ExportArchive archive, ExportSessionRecord session) throws IOException {
        UUID id = session.sessionId();
        long events = count("gaze_events", id), batches = count("gaze_batches", id), attempts = count("calibration_attempts", id);
        if ((!session.eyeTrackingEnabled() && (events > 0 || batches > 0 || attempts > 0))
                || (events > 0 && session.consented() == null)) throw ExportException.invalidGazeData();

        // Only small calibration metadata is retained; gaze events and batches always use cursors.
        Map<UUID, List<String>> reasons = reasons(id);
        if (attempts > 0) writeAttempts(archive, session, reasons);
        else if (!reasons.isEmpty()) throw ExportException.invalidGazeData();
        BatchSummary summary = batches > 0 ? writeBatches(archive, session) : new BatchSummary(0, 0, 0);
        if (summary.events() != events) throw ExportException.invalidGazeData();
        if (events > 0) writeEvents(archive, session, reasons);

        StringBuilder notes = new StringBuilder("Saved gaze events: " + events + "; batches: " + batches
                + "; calibration attempts: " + attempts + ".\n");
        if (attempts == 0) notes.append("calibration_attempts.csv was omitted: no saved calibration attempts.\n");
        if (batches == 0) notes.append("gaze_batches.csv was omitted: no saved gaze batches.\n");
        if (events == 0) notes.append("gaze_events.csv was omitted: ")
                .append(!session.eyeTrackingEnabled() ? "eye tracking is disabled for this study.\n"
                        : "no gaze predictions were saved at this snapshot; see sessions.csv for final availability and reason.\n");
        if (summary.gaps() > 0) notes.append("WARNING: ").append(summary.gaps())
                .append(" batch sequence numbers are absent before the last received batch.\n");
        if (summary.dropped() > 0) notes.append("WARNING: maximum browser-reported dropped_events_total is ")
                .append(summary.dropped()).append("; discarded predictions cannot be reconstructed.\n");
        notes.append("A zero gap count does not establish complete delivery: lost tail batches and a wholly lost stream are unknown.\n");
        return notes.toString();
    }

    private Map<UUID, List<String>> reasons(UUID session) {
        Map<UUID, List<String>> result = new HashMap<>();
        jdbc.query("""
                SELECT attempt_id, reason FROM calibration_attempt_reasons
                WHERE session_id = ? ORDER BY attempt_id, reason
                """, (RowCallbackHandler) rs -> {
            String reason = rs.getString("reason");
            enumValue(LowQualityReason.class, reason);
            result.computeIfAbsent(rs.getObject("attempt_id", UUID.class), ignored -> new ArrayList<>()).add(reason);
        }, session);
        return result;
    }

    private void writeAttempts(ExportArchive archive, ExportSessionRecord session,
                               Map<UUID, List<String>> reasons) throws IOException {
        List<String> headers = new ArrayList<>(List.of("study_id", "session_id"));
        headers.addAll(List.of(ATTEMPT_COLUMNS));
        headers.addAll(List.of("is_final", "low_quality_reasons"));
        CSVPrinter csv = archive.csv("calibration_attempts.csv", headers.toArray(String[]::new));
        Set<UUID> known = new HashSet<>();
        stream("""
                SELECT a.*, c.attempt_id AS selected_attempt_id FROM calibration_attempts a
                LEFT JOIN calibration_completions c ON c.session_id = a.session_id AND c.attempt_id = a.attempt_id
                WHERE a.session_id = ? ORDER BY a.started_at, a.attempt_id
                """, List.of(session.sessionId()), rs -> {
            UUID attempt = rs.getObject("attempt_id", UUID.class);
            if (attempt == null || !known.add(attempt) || rs.getInt("schema_version") != 1
                    || rs.getTimestamp("started_at") == null || rs.getTimestamp("finished_at") == null
                    || rs.getTimestamp("received_at") == null
                    || rs.getTimestamp("finished_at").before(rs.getTimestamp("started_at"))) {
                throw ExportException.invalidGazeData();
            }
            enumValue(CalibrationOutcome.class, rs.getString("outcome"));
            enumValue(CalibrationQuality.class, rs.getString("quality"));
            enumValue(CalibrationQualityBasis.class, rs.getString("quality_basis"));
            if (rs.getString("unavailable_reason") != null) {
                enumValue(EyeTrackingUnavailableReason.class, rs.getString("unavailable_reason"));
            }
            for (String column : List.of("good_fraction", "fair_fraction", "viewport_width", "viewport_height", "device_pixel_ratio")) {
                finite(rs, column);
            }
            for (String column : List.of("residual_median_px", "validation_median_px")) {
                if (rs.getObject(column) != null && finite(rs, column) < 0) throw ExportException.invalidGazeData();
            }
            List<Object> row = new ArrayList<>();
            row.add(session.studyId()); row.add(session.sessionId());
            for (String column : ATTEMPT_COLUMNS) {
                Object value = rs.getObject(column);
                // Timestamp.toString uses the server's local zone; exports always use UTC Instant.
                row.add(column.endsWith("_at") && value != null ? rs.getTimestamp(column).toInstant() : value);
            }
            row.add(rs.getObject("selected_attempt_id") != null);
            row.add(mapper.writeValueAsString(reasons.getOrDefault(attempt, List.of())));
            csv.printRecord(row);
        });
        if (!known.containsAll(reasons.keySet())) throw ExportException.invalidGazeData();
        archive.finish(csv);
    }

    private BatchSummary writeBatches(ExportArchive archive, ExportSessionRecord session) throws IOException {
        CSVPrinter csv = archive.csv("gaze_batches.csv", "study_id", "session_id", "batch_sequence", "batch_id",
                "schema_version", "event_count", "dropped_events", "dropped_events_total", "received_at");
        // Aggregate each batch inside PostgreSQL, not by retaining event records in Java.
        long[] totals = {0, 0, 0, 0}; // received batches, last sequence, events, maximum reported drops
        stream("""
                SELECT b.*, coalesce(e.actual_count, 0) AS actual_count, e.min_index, e.max_index
                FROM gaze_batches b LEFT JOIN (
                    SELECT batch_sequence, count(*) AS actual_count, min(event_index) AS min_index, max(event_index) AS max_index
                    FROM gaze_events WHERE session_id = ? GROUP BY batch_sequence
                ) e ON e.batch_sequence = b.sequence
                WHERE b.session_id = ? ORDER BY b.sequence
                """, List.of(session.sessionId(), session.sessionId()), rs -> {
            long sequence = rs.getLong("sequence"), expected = rs.getLong("event_count");
            if (sequence <= totals[1] || expected < 1 || rs.getInt("schema_version") != 1
                    || rs.getString("batch_id") == null || rs.getString("batch_id").isBlank()
                    || rs.getLong("actual_count") != expected || rs.getInt("min_index") != 0
                    || rs.getLong("max_index") != expected - 1 || rs.getTimestamp("received_at") == null
                    || rs.getLong("dropped_events") < 0
                    || rs.getLong("dropped_events_total") < rs.getLong("dropped_events")) {
                throw ExportException.invalidGazeData();
            }
            totals[0]++; totals[1] = sequence; totals[2] += expected;
            totals[3] = Math.max(totals[3], rs.getLong("dropped_events_total"));
            csv.printRecord(session.studyId(), session.sessionId(), sequence, rs.getString("batch_id"),
                    rs.getInt("schema_version"), expected, rs.getLong("dropped_events"),
                    rs.getLong("dropped_events_total"), rs.getTimestamp("received_at").toInstant());
        });
        archive.finish(csv);
        return new BatchSummary(totals[2], totals[1] - totals[0], totals[3]);
    }

    private void writeEvents(ExportArchive archive, ExportSessionRecord session,
                             Map<UUID, List<String>> reasons) throws IOException {
        CSVPrinter csv = archive.csv("gaze_events.csv", "study_id", "session_id", "batch_sequence", "event_index",
                "page_id", "x", "y", "scroll_x", "scroll_y", "viewport_width", "viewport_height", "device_pixel_ratio",
                "widget_id", "attribution", "occurred_at", "session_elapsed_ms", "calibration_attempt_id",
                "calibration_quality", "quality_basis", "low_quality_reasons");
        // LEFT JOIN preserves invalid references for rejection instead of silently dropping those events.
        stream("""
                SELECT e.*, b.session_id AS matched_batch_session, a.session_id AS matched_attempt_session,
                    a.tracking_available, a.quality AS calibration_quality, a.quality_basis
                FROM gaze_events e
                LEFT JOIN gaze_batches b ON b.session_id = e.session_id AND b.sequence = e.batch_sequence
                LEFT JOIN calibration_attempts a ON a.session_id = e.session_id AND a.attempt_id = e.calibration_attempt_id
                WHERE e.session_id = ? ORDER BY e.session_elapsed_ms, e.batch_sequence, e.event_index
                """, List.of(session.sessionId()), rs -> {
            UUID attempt = rs.getObject("calibration_attempt_id", UUID.class);
            String attribution = rs.getString("attribution"), widget = rs.getString("widget_id");
            if (rs.getObject("matched_batch_session") == null || rs.getObject("matched_attempt_session") == null
                    || !rs.getBoolean("tracking_available") || rs.getTimestamp("occurred_at") == null
                    || rs.getString("page_id") == null || rs.getString("page_id").isBlank()
                    || rs.getInt("event_index") < 0 || ("WIDGET".equals(attribution) != (widget != null))
                    || (widget != null && widget.isBlank())) throw ExportException.invalidGazeData();
            enumValue(GazeAttribution.class, attribution);
            enumValue(CalibrationQuality.class, rs.getString("calibration_quality"));
            enumValue(CalibrationQualityBasis.class, rs.getString("quality_basis"));
            double x = finite(rs, "x"), y = finite(rs, "y"), scrollX = finite(rs, "scroll_x"), scrollY = finite(rs, "scroll_y");
            double width = finite(rs, "viewport_width"), height = finite(rs, "viewport_height"), ratio = finite(rs, "device_pixel_ratio");
            double elapsed = finite(rs, "session_elapsed_ms");
            if (width < 0 || height < 0 || ratio <= 0) throw ExportException.invalidGazeData();
            csv.printRecord(session.studyId(), session.sessionId(), rs.getLong("batch_sequence"), rs.getInt("event_index"),
                    rs.getString("page_id"), x, y, scrollX, scrollY, width, height, ratio, widget, attribution,
                    rs.getTimestamp("occurred_at").toInstant(), elapsed, attempt, rs.getString("calibration_quality"),
                    rs.getString("quality_basis"), mapper.writeValueAsString(reasons.getOrDefault(attempt, List.of())));
        });
        archive.finish(csv);
    }

    private long count(String table, UUID session) {
        // Table names are fixed internal constants supplied by write(), never request input.
        return jdbc.queryForObject("SELECT count(*) FROM " + table + " WHERE session_id = ?", Long.class, session);
    }

    private double finite(ResultSet rs, String column) throws SQLException {
        double value = rs.getDouble(column);
        if (rs.wasNull() || !Double.isFinite(value)) throw ExportException.invalidGazeData();
        return value;
    }

    private <E extends Enum<E>> void enumValue(Class<E> type, String value) {
        try { Enum.valueOf(type, value); }
        catch (IllegalArgumentException | NullPointerException invalid) { throw ExportException.invalidGazeData(); }
    }

    private void stream(String sql, List<UUID> parameters, CheckedRow row) {
        jdbc.query(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql);
            for (int i = 0; i < parameters.size(); i++) statement.setObject(i + 1, parameters.get(i));
            statement.setFetchSize(512);
            return statement;
        }, (RowCallbackHandler) rs -> {
            try { row.write(rs); }
            catch (IOException failure) { throw new UncheckedIOException(failure); }
        });
    }

    /**
     * Allows cursor callbacks to propagate file failures without retaining rows.
     *
     * @author Simon Tian
     */
    @FunctionalInterface
    private interface CheckedRow { void write(ResultSet rs) throws SQLException, IOException; }

    /**
     * Operational receipt metadata, not scientific metrics derived from gaze coordinates.
     *
     * @author Simon Tian
     */
    private record BatchSummary(long events, long gaps, long dropped) { }
}
