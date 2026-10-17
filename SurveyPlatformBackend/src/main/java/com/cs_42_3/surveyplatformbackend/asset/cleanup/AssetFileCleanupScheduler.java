package com.cs_42_3.surveyplatformbackend.asset.cleanup;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Polls durable tasks after commit and across restarts without relying on in-memory events.
 *
 * @author Simon Tian
 */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.assets.cleanup", name = "scheduler-enabled", havingValue = "true", matchIfMissing = true)
public class AssetFileCleanupScheduler {
    private static final Logger log = LoggerFactory.getLogger(AssetFileCleanupScheduler.class);
    private final AssetFileCleanupProcessor processor;

    @Scheduled(fixedDelayString = "${app.assets.cleanup.scan-interval:30s}",
            initialDelayString = "${app.assets.cleanup.scan-interval:30s}")
    public void clean() {
        try {
            // Bound each scan so cleanup does not monopolize the shared scheduler.
            for (int count = 0; count < 50 && processor.processNext(); count++) {
                // Each call enters a separate transaction through the processor proxy.
            }
        } catch (RuntimeException failure) {
            log.error("Asset cleanup scan failed; persisted tasks will be retried on the next scan", failure);
        }
    }
}
