package com.cs_42_3.surveyplatformbackend.asset.cleanup;

import com.cs_42_3.surveyplatformbackend.asset.storage.AssetStorage;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Clock;

/**
 * Processes one locked task per transaction, retaining failures for bounded-backoff retries.
 *
 * @author Simon Tian
 */
@Service
@RequiredArgsConstructor
public class AssetFileCleanupProcessor {
    private static final Logger log = LoggerFactory.getLogger(AssetFileCleanupProcessor.class);
    private final AssetFileCleanupRepository tasks;
    private final AssetStorage storage;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processNext() {
        var pending = tasks.claimDue(clock.instant());
        if (pending.isEmpty()) return false;
        var task = pending.get();
        try {
            // Missing files are successful; a crash after deletion can safely retry this task.
            storage.delete(task.storageKey());
        } catch (RuntimeException failure) {
            int attempts = task.attempts() == Integer.MAX_VALUE ? Integer.MAX_VALUE : task.attempts() + 1;
            long delaySeconds = Math.min(3600L, 30L << Math.min(attempts - 1, 7));
            tasks.retry(task.id(), attempts, clock.instant().plusSeconds(delaySeconds),
                    "File deletion failed; see application logs.");
            log.warn("Asset cleanup task {} failed on attempt {}; retained for retry", task.id(), attempts, failure);
            return true;
        }
        // A database failure rolls this transaction back. The missing file is safe on the next retry.
        tasks.complete(task.id());
        return true;
    }
}
