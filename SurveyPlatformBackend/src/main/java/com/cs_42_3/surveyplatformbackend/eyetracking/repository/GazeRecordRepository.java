package com.cs_42_3.surveyplatformbackend.eyetracking.repository;

import com.cs_42_3.surveyplatformbackend.eyetracking.service.ValidatedGazeEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** JDBC access to the FR-50 gaze tables; events are written with one batched statement. */
@Repository
@RequiredArgsConstructor
public class GazeRecordRepository {
    /** pageX/pageY are not stored: they equal x + scroll_x and y + scroll_y (checked on receipt). */
    private static final String INSERT_EVENT = """
            INSERT INTO gaze_events (
                session_id, batch_sequence, event_index, page_id, x, y, scroll_x, scroll_y,
                viewport_width, viewport_height, device_pixel_ratio,
                widget_id, attribution, occurred_at, session_elapsed_ms, calibration_attempt_id
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;

    private final JdbcTemplate jdbc;

    public Optional<StoredBatch> findBatch(UUID sessionId, long sequence) {
        return jdbc.query(
                """
                        SELECT batch_id, event_count, request_hash, received_at
                        FROM gaze_batches
                        WHERE session_id = ? AND sequence = ?
                        """,
                (rs, row) -> new StoredBatch(
                        rs.getString("batch_id"),
                        rs.getInt("event_count"),
                        rs.getString("request_hash"),
                        rs.getObject("received_at", OffsetDateTime.class).toInstant()
                ),
                sessionId,
                sequence
        ).stream().findFirst();
    }

    public void insertBatch(
            UUID sessionId,
            long sequence,
            String batchId,
            int eventCount,
            long droppedEvents,
            long droppedEventsTotal,
            String requestHash,
            Instant receivedAt
    ) {
        jdbc.update(
                """
                        INSERT INTO gaze_batches (
                            session_id, sequence, batch_id, schema_version, event_count,
                            dropped_events, dropped_events_total, request_hash, received_at
                        ) VALUES (?, ?, ?, 1, ?, ?, ?, ?, ?)
                        """,
                sessionId,
                sequence,
                batchId,
                eventCount,
                droppedEvents,
                droppedEventsTotal,
                requestHash,
                utc(receivedAt)
        );
    }

    public void insertEvents(UUID sessionId, long sequence, List<ValidatedGazeEvent> events) {
        jdbc.batchUpdate(INSERT_EVENT, events, events.size(), (statement, event) -> {
            statement.setObject(1, sessionId);
            statement.setLong(2, sequence);
            statement.setInt(3, event.index());
            statement.setString(4, event.pageId());
            statement.setDouble(5, event.x());
            statement.setDouble(6, event.y());
            statement.setDouble(7, event.scrollX());
            statement.setDouble(8, event.scrollY());
            statement.setDouble(9, event.viewportWidth());
            statement.setDouble(10, event.viewportHeight());
            statement.setDouble(11, event.devicePixelRatio());
            setNullableString(statement, 12, event.widgetId());
            statement.setString(13, event.attribution().name());
            statement.setObject(14, utc(event.occurredAt()));
            statement.setDouble(15, event.sessionElapsedMs());
            statement.setObject(16, event.calibrationAttemptId());
        });
    }

    private static void setNullableString(java.sql.PreparedStatement statement, int index, String value)
            throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.VARCHAR);
        } else {
            statement.setString(index, value);
        }
    }

    private static OffsetDateTime utc(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    /** What an idempotent replay is compared against. */
    public record StoredBatch(String batchId, int eventCount, String requestHash, Instant receivedAt) {
    }
}
