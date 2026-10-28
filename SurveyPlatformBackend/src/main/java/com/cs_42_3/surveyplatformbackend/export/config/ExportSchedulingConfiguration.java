package com.cs_42_3.surveyplatformbackend.export.config;

import com.cs_42_3.surveyplatformbackend.export.service.*;
import lombok.RequiredArgsConstructor;
import org.slf4j.*;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.*;
import org.springframework.scheduling.annotation.*;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * Runs exports independently of session timeouts and image cleanup scheduling.
 *
 * @author Simon Tian
 */
@Configuration
@EnableScheduling
@ConditionalOnProperty(name = "app.exports.scheduler-enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class ExportSchedulingConfiguration {
    private static final Logger log = LoggerFactory.getLogger(ExportSchedulingConfiguration.class);
    private final ExportWorker worker;
    private final ExportCleanupService cleanup;

    /** Keeps unqualified lifecycle/asset schedules off the long-running export worker. */
    @Bean(name = "taskScheduler")
    @ConditionalOnMissingBean(name = "taskScheduler")
    public ThreadPoolTaskScheduler lifecycleTaskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("lifecycle-scheduler-");
        return scheduler;
    }

    @Bean
    public ThreadPoolTaskScheduler exportTaskScheduler() {
        var scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("export-worker-");
        return scheduler;
    }

    @Scheduled(fixedDelayString = "${app.exports.scan-interval:2s}",
            initialDelayString = "${app.exports.scan-interval:2s}", scheduler = "exportTaskScheduler")
    public void tick() {
        try {
            // Bounded maintenance prevents a large cleanup backlog from starving generation.
            for (int i = 0; i < 20 && worker.expireNext(); i++) { /* One committed task per iteration. */ }
            for (int i = 0; i < 20 && cleanup.processNext(); i++) { /* Retry failures on their next due date. */ }
            worker.startNext();
            // RUNNING tasks without a held row lock are recoverable after a restart.
            worker.processNext();
        } catch (RuntimeException failure) {
            log.warn("Export scan interrupted; tasks remain recoverable ({})", failure.getClass().getSimpleName());
        }
    }
}
