package com.cs_42_3.surveyplatformbackend.asset.cleanup;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.*;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Claims committed cleanup tasks with row locks shared safely across application instances.
 *
 * @author Simon Tian
 */
@Repository
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY)
public class AssetFileCleanupRepository {
    private final JdbcTemplate jdbc;

    public Optional<Task> claimDue(Instant now) {
        return jdbc.query("""
                SELECT id, storage_key, attempt_count FROM asset_file_cleanup_tasks
                WHERE next_attempt_at <= ? ORDER BY next_attempt_at, id
                LIMIT 1 FOR UPDATE SKIP LOCKED
                """, (rs, row) -> new Task(rs.getObject("id", UUID.class),
                rs.getString("storage_key"), rs.getInt("attempt_count")), Timestamp.from(now))
                .stream().findFirst();
    }

    public void complete(UUID id) {
        jdbc.update("DELETE FROM asset_file_cleanup_tasks WHERE id = ?", id);
    }

    public void retry(UUID id, int attempts, Instant nextAttempt, String error) {
        jdbc.update("""
                UPDATE asset_file_cleanup_tasks SET attempt_count = ?, next_attempt_at = ?, last_error = ?
                WHERE id = ?
                """, attempts, Timestamp.from(nextAttempt), error, id);
    }

    /**
     * Contains only the server-generated storage key needed for idempotent removal.
     *
     * @author Simon Tian
     */
    public record Task(UUID id, String storageKey, int attempts) {}
}
