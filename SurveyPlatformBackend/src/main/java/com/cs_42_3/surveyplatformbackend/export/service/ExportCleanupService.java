package com.cs_42_3.surveyplatformbackend.export.service;

import com.cs_42_3.surveyplatformbackend.export.storage.ExportStorage;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.UUID;

/**
 * Removes committed cleanup tasks only after both private artifact files have been removed.
 *
 * @author Simon Tian
 */
@Service
@RequiredArgsConstructor
public class ExportCleanupService {
    private static final Logger log = LoggerFactory.getLogger(ExportCleanupService.class);
    private final JdbcTemplate jdbc;
    private final ExportStorage storage;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processNext() {
        var tasks = jdbc.query("""
                SELECT export_id, attempts FROM export_file_cleanup_tasks WHERE next_attempt_at <= ?
                ORDER BY next_attempt_at, export_id LIMIT 1 FOR UPDATE SKIP LOCKED
                """, (rs, n) -> new CleanupTask(rs.getObject(1, UUID.class), rs.getInt(2)), Timestamp.from(clock.instant()));
        if (tasks.isEmpty()) return false;
        var task = tasks.get(0);
        try {
            // Missing files are successful, including retries after a process crash.
            storage.delete(task.id());
        } catch (Exception failure) {
            int attempts = task.attempts() == Integer.MAX_VALUE ? Integer.MAX_VALUE : task.attempts() + 1;
            long delay = Math.min(3600L, 30L << Math.min(attempts - 1, 7));
            jdbc.update("UPDATE export_file_cleanup_tasks SET attempts = ?, next_attempt_at = ? WHERE export_id = ?",
                    attempts, Timestamp.from(clock.instant().plusSeconds(delay)), task.id());
            log.warn("Export cleanup {} failed; retained for retry ({})", task.id(), failure.getClass().getSimpleName());
            return true;
        }
        jdbc.update("DELETE FROM export_file_cleanup_tasks WHERE export_id = ?", task.id());
        return true;
    }

    /**
     * Identifies one locked cleanup attempt without retaining artifact contents.
     *
     * @author Simon Tian
     */
    private record CleanupTask(UUID id, int attempts) { }
}
