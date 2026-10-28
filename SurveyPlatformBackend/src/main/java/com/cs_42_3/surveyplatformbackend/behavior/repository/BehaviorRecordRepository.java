package com.cs_42_3.surveyplatformbackend.behavior.repository;

import com.cs_42_3.surveyplatformbackend.behavior.api.dto.BehaviorBatchResponse;
import com.cs_42_3.surveyplatformbackend.behavior.domain.ValidatedBehaviorBatch;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * Stores one immutable envelope and its validated events in the caller's collection transaction.
 *
 * @author Simon Tian
 */
@Repository
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class BehaviorRecordRepository {
    private final JdbcTemplate jdbc;

    public Optional<StoredBatch> findBatch(UUID session, UUID batch) {
        return find("SELECT * FROM behavior_batches WHERE session_id = ? AND batch_id = ?", session, batch);
    }

    public Optional<StoredBatch> findSequence(UUID session, UUID stream, long sequence) {
        return find("SELECT * FROM behavior_batches WHERE session_id = ? AND stream_id = ? AND sequence = ?",
                session, stream, sequence);
    }

    private Optional<StoredBatch> find(String sql, Object... parameters) {
        return jdbc.query(sql, (rs, row) -> new StoredBatch(new BehaviorBatchResponse(
                rs.getObject("session_id", UUID.class), rs.getObject("batch_id", UUID.class),
                rs.getObject("stream_id", UUID.class), rs.getLong("sequence"), rs.getInt("event_count"),
                rs.getTimestamp("received_at").toInstant()), rs.getString("request_hash")), parameters)
                .stream().findFirst();
    }

    public void insertBatch(UUID session, ValidatedBehaviorBatch batch, Instant received) {
        jdbc.update("""
                INSERT INTO behavior_batches(session_id,batch_id,stream_id,sequence,schema_version,event_count,request_hash,received_at)
                VALUES (?,?,?,?,1,?,?,?)
                """, session, batch.batchId(), batch.streamId(), batch.sequence(), batch.events().size(),
                batch.requestHash(), Timestamp.from(received));
    }

    public void insertEvents(UUID session, ValidatedBehaviorBatch batch) {
        jdbc.batchUpdate("""
                INSERT INTO behavior_events(session_id,batch_id,event_index,event_type,occurred_at,session_elapsed_ms,
                    page_id,component_id,component_type,payload)
                VALUES (?,?,?,?,?,?,?,?,?,CAST(? AS jsonb))
                """, batch.events(), batch.events().size(), (statement, event) -> {
            statement.setObject(1, session); statement.setObject(2, batch.batchId()); statement.setInt(3, event.index());
            statement.setString(4, event.type().wireValue()); statement.setTimestamp(5, Timestamp.from(event.occurredAt()));
            statement.setDouble(6, event.sessionElapsedMs()); statement.setString(7, event.pageId());
            statement.setString(8, event.componentId()); statement.setString(9, event.componentType());
            statement.setString(10, event.payload());
        });
    }

    /**
     * Original acknowledgement and digest used for semantic replay checks.
     *
     * @author Simon Tian
     */
    public record StoredBatch(BehaviorBatchResponse response, String requestHash) { }
}
