package com.cs_42_3.surveyplatformbackend.export.service;

import com.cs_42_3.surveyplatformbackend.behavior.api.dto.BehaviorEventRequest;
import com.cs_42_3.surveyplatformbackend.behavior.domain.BehaviorEventType;
import com.cs_42_3.surveyplatformbackend.behavior.exception.BehaviorBatchException;
import com.cs_42_3.surveyplatformbackend.behavior.service.BehaviorBatchValidator;
import com.cs_42_3.surveyplatformbackend.export.domain.ExportSessionRecord;
import com.cs_42_3.surveyplatformbackend.export.exception.ExportException;
import com.cs_42_3.surveyplatformbackend.export.storage.ExportArchive;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.*;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.*;
import java.io.*;
import java.sql.*;
import java.util.*;

/**
 * Streams behavior records with raw payloads, readable columns and per-stream receipt warnings.
 *
 * @author Simon Tian
 */
@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY, readOnly = true)
public class BehaviorExportWriter {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final BehaviorBatchValidator validator;

    public String write(ExportArchive archive, ExportSessionRecord session) throws IOException {
        UUID id = session.sessionId();
        long events = jdbc.queryForObject("SELECT count(*) FROM behavior_events WHERE session_id = ?", Long.class, id);
        long batches = jdbc.queryForObject("SELECT count(*) FROM behavior_batches WHERE session_id = ?", Long.class, id);
        if ((events > 0 || batches > 0) && session.consented() == null) throw ExportException.invalidBehaviorData();
        var totals = batches > 0 ? writeBatches(archive, session) : new BatchTotals(0, 0, 0);
        if (totals.events() != events) throw ExportException.invalidBehaviorData();
        if (events > 0) writeEvents(archive, session);
        StringBuilder notes = new StringBuilder("Saved behavior events: " + events + "; batches: " + batches
                + "; streams: " + totals.streams() + ".\n");
        if (batches == 0) notes.append("behavior_batches.csv was omitted: no saved behavior batches.\n");
        if (events == 0) notes.append("behavior_events.csv was omitted: no saved behavior events at this snapshot.\n");
        if (totals.gaps() > 0) notes.append("WARNING: ").append(totals.gaps())
                .append(" batch sequence numbers are absent before the last received batch, counted separately per stream.\n");
        notes.append("No records for an event type do not establish whether it was uncollected, absent or lost.\n")
                .append("Final expected sequences and behavior drop counts are not recorded; lost tails and wholly lost streams are unknown.\n");
        return notes.toString();
    }

    private BatchTotals writeBatches(ExportArchive archive, ExportSessionRecord session) throws IOException {
        var csv = archive.csv("behavior_batches.csv", "study_id", "session_id", "batch_id", "stream_id",
                "sequence", "schema_version", "event_count", "received_at");
        UUID[] current = {null};
        long[] stream = {0, 0}; // batch count and last sequence for the current stream
        long[] total = {0, 0, 0}; // event count, stream count and missing sequences
        stream("""
                SELECT b.*, coalesce(e.actual_count,0) AS actual_count, e.min_index, e.max_index
                FROM behavior_batches b LEFT JOIN (
                    SELECT batch_id,count(*) AS actual_count,min(event_index) AS min_index,max(event_index) AS max_index
                    FROM behavior_events WHERE session_id = ? GROUP BY batch_id
                ) e ON e.batch_id = b.batch_id
                WHERE b.session_id = ? ORDER BY b.stream_id,b.sequence,b.batch_id
                """, List.of(session.sessionId(), session.sessionId()), rs -> {
            UUID batch = rs.getObject("batch_id", UUID.class), streamId = rs.getObject("stream_id", UUID.class);
            long sequence = rs.getLong("sequence"), count = rs.getLong("event_count");
            if (batch == null || streamId == null || sequence < 1 || sequence > 9_007_199_254_740_991L
                    || rs.getInt("schema_version") != 1 || count < 1 || count > 10000
                    || rs.getLong("actual_count") != count || rs.getInt("min_index") != 0
                    || rs.getLong("max_index") != count - 1 || rs.getTimestamp("received_at") == null) {
                throw ExportException.invalidBehaviorData();
            }
            if (!streamId.equals(current[0])) {
                total[2] += stream[1] - stream[0]; total[1]++;
                current[0] = streamId; stream[0] = 0; stream[1] = 0;
            }
            if (sequence <= stream[1]) throw ExportException.invalidBehaviorData();
            stream[0]++; stream[1] = sequence; total[0] += count;
            csv.printRecord(session.studyId(), session.sessionId(), batch, streamId, sequence, 1, count,
                    rs.getTimestamp("received_at").toInstant());
        });
        total[2] += stream[1] - stream[0];
        archive.finish(csv);
        return new BatchTotals(total[0], total[1], total[2]);
    }

    private void writeEvents(ExportArchive archive, ExportSessionRecord session) throws IOException {
        var csv = archive.csv("behavior_events.csv", "study_id", "session_id", "stream_id", "batch_id", "sequence",
                "event_index", "event_type", "page_id", "component_id", "component_type", "occurred_at", "session_elapsed_ms",
                "x", "y", "key_code", "scroll_x", "scroll_y", "hidden", "payload");
        stream("""
                SELECT e.*,b.batch_id AS parent_batch_id,b.stream_id,b.sequence FROM behavior_events e
                LEFT JOIN behavior_batches b ON b.session_id = e.session_id AND b.batch_id = e.batch_id
                WHERE e.session_id = ? ORDER BY e.session_elapsed_ms,e.batch_id,e.event_index
                """, List.of(session.sessionId()), rs -> {
            if (rs.getObject("parent_batch_id") == null || rs.getTimestamp("occurred_at") == null
                    || rs.getObject("session_elapsed_ms") == null || rs.getInt("event_index") < 0) {
                throw ExportException.invalidBehaviorData();
            }
            String raw = rs.getString("payload");
            JsonNode payload = validatePayload(rs, raw);
            csv.printRecord(session.studyId(), session.sessionId(), rs.getObject("stream_id", UUID.class),
                    rs.getObject("batch_id", UUID.class), rs.getLong("sequence"), rs.getInt("event_index"),
                    rs.getString("event_type"), rs.getString("page_id"), rs.getString("component_id"), rs.getString("component_type"),
                    rs.getTimestamp("occurred_at").toInstant(), rs.getDouble("session_elapsed_ms"),
                    number(payload, "x"), number(payload, "y"), text(payload, "code"), number(payload, "scrollX"),
                    number(payload, "scrollY"), payload.has("hidden") ? payload.get("hidden").booleanValue() : null, raw);
        });
        archive.finish(csv);
    }

    private JsonNode validatePayload(ResultSet rs, String raw) throws SQLException {
        if (raw == null) throw ExportException.invalidBehaviorData();
        try {
            JsonNode payload = mapper.readTree(raw);
            BehaviorEventType type = BehaviorEventType.parse(rs.getString("event_type"));
            Set<String> fields = switch (type) {
                case CLICK, MOUSEMOVE -> Set.of("x", "y");
                case KEYDOWN -> Set.of("code");
                case SCROLL -> Set.of("scrollX", "scrollY");
                case VISIBILITY_CHANGE -> Set.of("hidden");
            };
            if (payload == null || !payload.isObject() || payload.size() != fields.size()
                    || fields.stream().anyMatch(field -> !payload.hasNonNull(field))) throw ExportException.invalidBehaviorData();
            if (payload.has("hidden") && !payload.get("hidden").isBoolean()) throw ExportException.invalidBehaviorData();
            var input = new BehaviorEventRequest(type.wireValue(), rs.getTimestamp("occurred_at").toInstant().toEpochMilli(),
                    rs.getDouble("session_elapsed_ms"), rs.getString("page_id"),
                    doubleValue(payload, "x"), doubleValue(payload, "y"), text(payload, "code"),
                    doubleValue(payload, "scrollX"), doubleValue(payload, "scrollY"),
                    payload.has("hidden") ? payload.get("hidden").booleanValue() : null,
                    rs.getString("component_id"), rs.getString("component_type"));
            // Reuse the upload rules per row without imposing today's batch-size configuration on historical data.
            validator.validateEvent(input, rs.getInt("event_index"));
            return payload;
        } catch (BehaviorBatchException | JacksonException | ArithmeticException invalid) {
            throw ExportException.invalidBehaviorData();
        }
    }

    private Double doubleValue(JsonNode node, String field) {
        Number value = number(node, field);
        return value == null ? null : value.doubleValue();
    }

    private Number number(JsonNode node, String field) {
        if (!node.has(field)) return null;
        if (!node.get(field).isNumber()) throw ExportException.invalidBehaviorData();
        return node.get(field).numberValue();
    }

    private String text(JsonNode node, String field) {
        if (!node.has(field)) return null;
        if (!node.get(field).isString()) throw ExportException.invalidBehaviorData();
        return node.get(field).asString();
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
     * Cursor callback that propagates file failures without buffering event rows.
     *
     * @author Simon Tian
     */
    @FunctionalInterface
    private interface CheckedRow { void write(ResultSet row) throws SQLException, IOException; }

    /**
     * Receipt metadata for README warnings; no scientific behavior metrics are computed.
     *
     * @author Simon Tian
     */
    private record BatchTotals(long events, long streams, long gaps) { }
}
