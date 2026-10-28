package com.cs_42_3.surveyplatformbackend.export.repository;

import com.cs_42_3.surveyplatformbackend.export.api.dto.ExportJobResponse;
import com.cs_42_3.surveyplatformbackend.export.domain.*;
import com.cs_42_3.surveyplatformbackend.export.exception.ExportException;
import com.cs_42_3.surveyplatformbackend.common.exception.ErrorCode;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;

/**
 * JDBC task persistence; callers own transaction boundaries and lock ordering.
 *
 * @author Simon Tian
 */
@Repository
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class ExportJobRepository {
    private final JdbcTemplate jdbc;

    public void requireOwner(UUID owner, UUID study, boolean lock) {
        var ids = jdbc.query("SELECT id FROM studies WHERE id = ? AND owner_id = ?"
                + (lock ? " FOR SHARE" : ""), (rs, n) -> rs.getObject(1, UUID.class), study, owner);
        if (ids.isEmpty()) throw new StudyNotFoundException();
    }

    public void requireSession(UUID study, UUID session) {
        if (jdbc.queryForObject("SELECT count(*) FROM participant_sessions WHERE id = ? AND study_id = ?",
                Long.class, session, study) == 0) {
            throw new ExportException(ErrorCode.PARTICIPANT_SESSION_NOT_FOUND, HttpStatus.NOT_FOUND,
                    "Participant session not found.");
        }
    }

    public ExportJobResponse insert(UUID study, UUID session, Set<ExportDataType> types, Instant now) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO export_jobs(id, study_id, session_id, data_types, status, created_at)
                VALUES (?, ?, ?, ?, 'PENDING', ?)
                """, id, study, session, ExportDataType.encode(types), Timestamp.from(now));
        return get(study, id, false);
    }

    public ExportJobResponse get(UUID study, UUID id, boolean lock) {
        return jdbc.query("SELECT * FROM export_jobs WHERE study_id = ? AND id = ?"
                + (lock ? " FOR UPDATE" : ""), this::map, study, id).stream().findFirst()
                .orElseThrow(() -> new ExportException(ErrorCode.EXPORT_NOT_FOUND,
                        HttpStatus.NOT_FOUND, "Export task not found."));
    }

    /** Makes RUNNING visible before generation; the later processing lock is crash-recoverable. */
    public boolean startNext(Instant now) {
        var ids = jdbc.query("""
                SELECT id FROM export_jobs WHERE status = 'PENDING'
                ORDER BY created_at, id LIMIT 1 FOR UPDATE SKIP LOCKED
                """, (rs, n) -> rs.getObject(1, UUID.class));
        if (ids.isEmpty()) return false;
        jdbc.update("UPDATE export_jobs SET status = 'RUNNING', started_at = ? WHERE id = ?",
                Timestamp.from(now), ids.get(0));
        return true;
    }

    /** Only one worker may write a task's deterministic temporary and ZIP paths. */
    public Optional<ExportJobResponse> lockNextRunning() {
        return jdbc.query("""
                SELECT * FROM export_jobs WHERE status = 'RUNNING'
                ORDER BY created_at, id LIMIT 1 FOR UPDATE SKIP LOCKED
                """, this::map).stream().findFirst();
    }

    public void complete(UUID id, Instant snapshot, Instant now, Instant expires) {
        jdbc.update("""
                UPDATE export_jobs SET status = 'COMPLETED', snapshot_at = ?, completed_at = ?, expires_at = ?
                WHERE id = ?
                """, Timestamp.from(snapshot), Timestamp.from(now), Timestamp.from(expires), id);
    }

    public void fail(UUID id, String code, String message, Instant now) {
        jdbc.update("""
                UPDATE export_jobs SET status = 'FAILED', completed_at = ?, error_code = ?, error_message = ?
                WHERE id = ?
                """, Timestamp.from(now), code, message, id);
        enqueueCleanup(id, now);
    }

    public void enqueueCleanup(UUID id, Instant now) {
        jdbc.update("""
                INSERT INTO export_file_cleanup_tasks(export_id, created_at, next_attempt_at)
                VALUES (?, ?, ?) ON CONFLICT (export_id) DO NOTHING
                """, id, Timestamp.from(now), Timestamp.from(now));
    }

    public boolean expireNext(Instant now) {
        var ids = jdbc.query("""
                SELECT id FROM export_jobs WHERE status = 'COMPLETED' AND expires_at <= ?
                ORDER BY expires_at, id LIMIT 1 FOR UPDATE SKIP LOCKED
                """, (rs, n) -> rs.getObject(1, UUID.class), Timestamp.from(now));
        if (ids.isEmpty()) return false;
        UUID id = ids.get(0);
        jdbc.update("UPDATE export_jobs SET status = 'EXPIRED' WHERE id = ?", id);
        enqueueCleanup(id, now);
        return true;
    }

    private ExportJobResponse map(ResultSet rs, int n) throws SQLException {
        return new ExportJobResponse(rs.getObject("id", UUID.class), rs.getObject("study_id", UUID.class),
                rs.getObject("session_id", UUID.class), ExportDataType.decode(rs.getString("data_types")),
                ExportStatus.valueOf(rs.getString("status")), instant(rs, "created_at"),
                instant(rs, "started_at"), instant(rs, "snapshot_at"), instant(rs, "completed_at"),
                instant(rs, "expires_at"), rs.getString("error_code"), rs.getString("error_message"));
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }
}
