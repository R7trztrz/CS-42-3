package com.cs_42_3.surveyplatformbackend.export.service;

import com.cs_42_3.surveyplatformbackend.common.exception.ErrorCode;
import com.cs_42_3.surveyplatformbackend.export.exception.ExportException;
import com.cs_42_3.surveyplatformbackend.export.repository.ExportJobRepository;
import com.cs_42_3.surveyplatformbackend.export.storage.ExportStorage;
import org.slf4j.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.convert.DurationStyle;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.*;

/**
 * Processes tasks under database locks so crashes and multiple application instances are safe.
 *
 * @author Simon Tian
 */
@Service
public class ExportWorker {
    private static final Logger log = LoggerFactory.getLogger(ExportWorker.class);
    private final ExportJobRepository jobs;
    private final ExportSnapshotService snapshots;
    private final ExportStorage storage;
    private final Clock clock;
    private final Duration retention;

    public ExportWorker(ExportJobRepository jobs, ExportSnapshotService snapshots, ExportStorage storage,
            Clock clock, @Value("${app.exports.retention:24h}") String retention) {
        Duration validity = DurationStyle.detectAndParse(retention);
        if (validity.isZero() || validity.isNegative()) throw new IllegalArgumentException("Export retention must be positive.");
        this.jobs = jobs;
        this.snapshots = snapshots;
        this.storage = storage;
        this.clock = clock;
        this.retention = validity;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean startNext() {
        return jobs.startNext(clock.instant());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean processNext() {
        var pending = jobs.lockNextRunning();
        if (pending.isEmpty()) return false;
        var job = pending.get();
        try {
            // The outer job lock prevents deletion and duplicate writers. Participant writes remain unlocked.
            Instant snapshot = job.scope() == com.cs_42_3.surveyplatformbackend.export.domain.ExportScope.STUDY
                    ? snapshots.generateStudy(job.id(), job.studyId(), job.dataTypes())
                    : snapshots.generate(job.id(), job.studyId(), job.sessionId(), job.dataTypes());
            storage.publish(job.id());
            Instant now = clock.instant();
            jobs.complete(job.id(), snapshot, now, now.plus(retention));
        } catch (Exception failure) {
            // Generation runs in its own transaction; its rollback cannot poison this failure update.
            String code = failure instanceof ExportException invalid
                    ? invalid.getCode().code() : ErrorCode.EXPORT_GENERATION_FAILED.code();
            String message = failure instanceof ExportException invalid ? invalid.getMessage()
                    : "Export generation failed. Retry by creating a new export task.";
            if (failure instanceof ExportException invalid && invalid.getFailedSessionId() != null) {
                jobs.fail(job.id(), code, message, clock.instant(), invalid.getFailedSessionId());
            } else {
                jobs.fail(job.id(), code, message, clock.instant());
            }
            // Log identifiers and the exception class, never answer payloads or snapshot content.
            log.warn("Export {} failed ({})", job.id(), failure.getClass().getSimpleName());
        }
        return true;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean expireNext() {
        return jobs.expireNext(clock.instant());
    }
}
