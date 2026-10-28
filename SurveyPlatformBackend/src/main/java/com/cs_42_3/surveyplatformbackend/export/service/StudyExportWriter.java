package com.cs_42_3.surveyplatformbackend.export.service;

import com.cs_42_3.surveyplatformbackend.export.domain.*;
import com.cs_42_3.surveyplatformbackend.export.exception.ExportException;
import com.cs_42_3.surveyplatformbackend.export.storage.ExportArchive;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import java.io.*;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;

/**
 * Traverses all study sessions in one snapshot and appends validated records to disk-backed CSV sinks.
 *
 * @author Simon Tian
 */
@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY, readOnly = true)
public class StudyExportWriter {
    private static final int PAGE_SIZE = 200;
    private static final List<DataFile> FILES = List.of(
            new DataFile("responses.csv", ExportDataType.QUESTIONNAIRE),
            new DataFile("gaze_events.csv", ExportDataType.GAZE),
            new DataFile("gaze_batches.csv", ExportDataType.GAZE),
            new DataFile("calibration_attempts.csv", ExportDataType.GAZE),
            new DataFile("behavior_events.csv", ExportDataType.BEHAVIOR),
            new DataFile("behavior_batches.csv", ExportDataType.BEHAVIOR));
    private final JdbcTemplate jdbc;
    private final SessionExportWriter sessions;
    private final ExportArchiveWriter rows;

    public void write(UUID study, Set<ExportDataType> types, Instant snapshot, OutputStream output,
                      Path workspace) throws IOException {
        if (jdbc.queryForObject("SELECT count(*) FROM studies WHERE id = ?", Long.class, study) != 1) {
            throw new IllegalStateException("Export study is no longer available.");
        }
        long sessionCount = 0;
        UUID after = null;
        try (var archive = new ExportArchive(output, workspace)) {
            // An empty study still exports the normal session schema with no fabricated rows.
            sessions.writeEmpty(archive);
            while (true) {
                List<UUID> page = page(study, after);
                if (page.isEmpty()) break;
                for (UUID id : page) {
                    try {
                        var session = sessions.read(study, id);
                        long[] before = FILES.stream().mapToLong(file -> archive.records(file.name())).toArray();
                        String notes = rows.writeRows(archive, session, types, snapshot);
                        var manifest = archive.csv("data_availability.csv", "session_id", "file_name", "status",
                                "reason", "record_count", "session_notes");
                        for (int i = 0; i < FILES.size(); i++) {
                            var file = FILES.get(i);
                            boolean selected = types.contains(file.type());
                            long count = archive.records(file.name()) - before[i];
                            String status = !selected ? "NOT_SELECTED" : count > 0 ? "PRESENT" : "NO_SAVED_RECORDS";
                            String reason = !selected ? "NOT_SELECTED" : count > 0 ? "RECORDS_PRESENT" : missingReason(file, session);
                            manifest.printRecord(id, file.name(), status, reason, count, i == 0 ? notes : null);
                        }
                        archive.finish(manifest);
                        sessionCount++;
                    } catch (Exception failure) {
                        throw ExportException.forSession(id, failure);
                    }
                }
                after = page.get(page.size() - 1);
            }
            archive.text("README.md", rows.schema(study, null, types, snapshot) + """

                    ## Study-wide output
                    Scope: every session in this study, including in-progress, completed and abandoned sessions.
                    Included session count: %d
                    All source reads share one read-only REPEATABLE READ snapshot established during generation.
                    CSV rows are grouped by session_id ascending; within a session each file retains single-session ordering.
                    Unselected categories are not queried. Data files with no rows across the study are omitted.
                    sessions.csv always exists; an empty study has only its header and this README.
                    No image files, feed templates or other configuration assets are included.

                    ### data_availability.csv
                    One row per session and potential category file; session_id/file_name identify its scope.
                    status is PRESENT, NO_SAVED_RECORDS or NOT_SELECTED. reason explains known disabled switches
                    or simply NO_SAVED_RECORDS when the absence has no more precise stored explanation.
                    record_count is a file receipt/count check, not a scientific metric.
                    session_notes contains that session's selected-category notes and delivery warnings, written
                    only on its first manifest row. These notes apply to the session, not only that row's file.
                    For final eye-tracking availability and unavailable reasons, consult sessions.csv.
                    No missing event types, tail batches or browser delivery guarantees are inferred.
                    This manifest is omitted for a study without sessions.
                    """.formatted(sessionCount));
            // Packing starts only after all sessions validate. The worker publishes the ZIP after this returns.
            archive.complete();
        }
    }

    private List<UUID> page(UUID study, UUID after) {
        String sql = "SELECT id FROM participant_sessions WHERE study_id = ?"
                + (after == null ? "" : " AND id > ?") + " ORDER BY id LIMIT " + PAGE_SIZE;
        Object[] parameters = after == null ? new Object[]{study} : new Object[]{study, after};
        return jdbc.query(sql, (rs, index) -> rs.getObject(1, UUID.class), parameters);
    }

    private String missingReason(DataFile file, ExportSessionRecord session) {
        if (file.type() == ExportDataType.QUESTIONNAIRE && !session.questionnaireEnabled()) return "QUESTIONNAIRE_DISABLED";
        if (file.type() == ExportDataType.GAZE && !session.eyeTrackingEnabled()) return "EYE_TRACKING_DISABLED";
        return "NO_SAVED_RECORDS";
    }

    /**
     * Stable potential file list used for per-session availability, including unselected categories.
     *
     * @author Simon Tian
     */
    private record DataFile(String name, ExportDataType type) { }
}
