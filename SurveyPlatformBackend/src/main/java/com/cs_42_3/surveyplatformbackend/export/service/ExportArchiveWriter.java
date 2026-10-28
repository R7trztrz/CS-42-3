package com.cs_42_3.surveyplatformbackend.export.service;

import com.cs_42_3.surveyplatformbackend.export.domain.ExportDataType;
import com.cs_42_3.surveyplatformbackend.export.domain.ExportSessionRecord;
import com.cs_42_3.surveyplatformbackend.export.storage.ExportArchive;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import java.io.*;
import java.time.Instant;
import java.util.*;

/**
 * Coordinates common files and selected categories without opening separate data snapshots.
 *
 * @author Simon Tian
 */
@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY, readOnly = true)
public class ExportArchiveWriter {
    private final SessionExportWriter sessions;
    private final QuestionnaireExportWriter questionnaire;
    private final GazeExportWriter gaze;
    private final BehaviorExportWriter behavior;

    public void write(UUID study, UUID sessionId, Set<ExportDataType> types, Instant snapshotAt,
                      OutputStream output) throws IOException {
        var session = sessions.read(study, sessionId);
        try (var archive = new ExportArchive(output)) {
            String notes = writeRows(archive, session, types, snapshotAt);
            archive.text("README.md", schema(study, sessionId, types, snapshotAt) + notes);
        }
    }

    /** Appends one session using exactly the same validation and row writers for either scope. */
    public String writeRows(ExportArchive archive, ExportSessionRecord session, Set<ExportDataType> types,
                            Instant snapshotAt) throws IOException {
        sessions.write(archive, session, snapshotAt);
        StringBuilder readme = new StringBuilder();
        if (types.contains(ExportDataType.QUESTIONNAIRE)) {
            readme.append("\n## Questionnaire data\n").append(questionnaire.write(archive, session));
        } else {
            readme.append("\nresponses.csv was omitted: QUESTIONNAIRE was not selected.\n");
        }
        if (types.contains(ExportDataType.GAZE)) {
            readme.append("\n## Eye-tracking data\n").append(gaze.write(archive, session));
        } else {
            readme.append("\nEye-tracking CSV files were omitted: GAZE was not selected.\n");
        }
        if (types.contains(ExportDataType.BEHAVIOR)) {
            readme.append("\n## Behavior data\n").append(behavior.write(archive, session));
        } else {
            readme.append("\nBehavior CSV files were omitted: BEHAVIOR was not selected.\n");
        }
        return readme.toString();
    }

    public String schema(UUID study, UUID session, Set<ExportDataType> types, Instant snapshotAt) {
        return commonSchema(study, session, types, snapshotAt)
                + (types.contains(ExportDataType.QUESTIONNAIRE) ? questionnaireSchema() : "")
                + (types.contains(ExportDataType.GAZE) ? gazeSchema() : "")
                + (types.contains(ExportDataType.BEHAVIOR) ? behaviorSchema() : "");
    }

    private String commonSchema(UUID study, UUID session, Set<ExportDataType> types, Instant snapshot) {
        return """
                # Research data export — schema v4

                Study: %s
                Scope: %s
                Session: %s
                Snapshot transaction started at: %s
                Selected categories: %s

                CSV uses UTF-8 without BOM and RFC 4180 escaping. All timestamp columns use UTC ISO-8601.
                Empty cells represent absent values; an empty TEXT answer remains distinguishable by ANSWERED.
                Research text is preserved, including formula-like prefixes: import columns as text in spreadsheet tools.
                For Excel, use Data > From Text/CSV and select 65001: Unicode (UTF-8).
                IDs are anonymous identifiers, not counts of distinct people. No credentials or camera video are exported.
                This is a consistent snapshot of persisted data, not proof that every browser event arrived.

                ## sessions.csv
                study_id/session_id identify the source; anonymous_participant_id is the per-study alias.
                status is IN_PROGRESS/COMPLETED/ABANDONED; phase is the stored participation stage.
                abandonment_reason and lifecycle *_at columns are the stored state and timestamps.
                questionnaire_enabled/eye_tracking_enabled are study collection switches.
                calibration_state, tracking_available, calibration_quality, quality_basis,
                unavailable_reason and low_quality_reasons describe the final selected calibration.
                Missing final metadata stays blank; low_quality_reasons is a JSON string array.
                tracking_available is not proof of saved gaze events. snapshot_at is export read time.
                """.formatted(study, session == null ? "STUDY" : "SESSION", session == null ? "ALL" : session,
                snapshot, ExportDataType.encode(types));
    }

    private String questionnaireSchema() {
        return """
                ### responses.csv
                One row per saved questionnaire step in sequence_number order.
                study_id/session_id identify the source; questionnaire_item_id identifies the published item.
                question_type/question_text and option labels come from the immutable publication, not the current bank.
                response_status is ANSWERED or SKIPPED (an intentionally unanswered optional item).
                answer_payload preserves stored JSON: optionId, optionIds, value or text.
                answer_text resolves published labels; MULTI_CHOICE uses a JSON string array.
                submitted_at is step submission time; answered_at exists only for saved answers.
                Branch-unvisited and not-yet-submitted items have no rows; partial sessions are legitimate.
                """;
    }

    private String behaviorSchema() {
        return """
                ### behavior_events.csv
                One row per stored event, sorted by session_elapsed_ms, batch_id, event_index.
                study_id/session_id identify its source; stream_id identifies the page collection instance.
                batch_id/sequence/event_index retain original batch identity, stream sequence and array order.
                event_type is click/keydown/mousemove/scroll/visibilitychange. page_id/component_id/component_type
                preserve reported browser attribution; missing attribution is blank.
                occurred_at is client event time in UTC; session_elapsed_ms is the reported shared-clock value.
                Negative/equal elapsed values are valid. Sorting does not repair clock-origin changes after reload.
                x/y are viewport CSS pixels, key_code is KeyboardEvent.code, scroll_x/scroll_y are scroll offsets,
                and hidden is the visibility flag. Inapplicable columns are blank, not invented zeros or false values.
                payload preserves the stored JSON object; expanded columns extract its fields without new metrics.
                No characters typed into inputs, DOM content, aggregate metrics or inferred events are added.

                ### behavior_batches.csv
                One row per stored batch ordered by stream_id, sequence, batch_id.
                study_id/session_id/batch_id/stream_id identify its source and collection instance.
                sequence is local to that stream, schema_version is the stored upload version,
                event_count is the declared count checked against saved events, received_at is server receipt time.
                Sequence gaps are counted separately per stream, not across stream restarts.
                No final expected sequence or behavior drop count is persisted, so a gap-free result cannot prove
                complete delivery. Absent types can mean uncollected, absent activity or lost uploads.
                """;
    }

    private String gazeSchema() {
        return """
                ### gaze_events.csv
                One row per saved prediction, ordered by session_elapsed_ms, batch_sequence, event_index.
                study_id/session_id identify the source; batch_sequence/event_index identify the original event.
                page_id is the browser page identifier. x/y are raw viewport CSS pixels, not physical screen pixels.
                scroll_x/scroll_y are scroll offsets; viewport_width/viewport_height are CSS pixels.
                device_pixel_ratio is the reported device ratio. Coordinates are not rounded, clamped or filtered.
                widget_id and attribution (WIDGET/NO_CONTENT/UNRESOLVED) preserve browser attribution.
                occurred_at is the browser-reported wall-clock time; session_elapsed_ms is the reported relative time.
                Negative elapsed values and equal timestamps are valid. Client and server clocks may differ.
                calibration_attempt_id identifies this point's calibration, not necessarily the session's final attempt.
                calibration_quality, quality_basis and low_quality_reasons are joined from that exact attempt.
                No fixation metrics, heatmaps, inferred page coordinates or new grades are computed.

                ### gaze_batches.csv
                One row per received batch in batch_sequence order. study_id/session_id identify its source.
                batch_id/schema_version/event_count/dropped_events/dropped_events_total/received_at are stored values.
                dropped_events_total is a reported cumulative count: do not sum cumulative counts across batches.
                Missing batch sequences and browser-reported drops are warnings, not fabricated replacement events.
                There is no persisted final expected sequence: lost tail batches or a wholly lost stream cannot be detected.

                ### calibration_attempts.csv
                One row per stored completed/abandoned/unavailable attempt, including attempts with no gaze events.
                study_id/session_id and attempt_id identify the source. is_final marks calibration_completions selection.
                schema_version/algorithm_version identify the stored wire contract and grading algorithm.
                attempt_number is the browser counter and may repeat after a reload; attempt_id is the stable identity.
                outcome/tracking_available/unavailable_reason and quality/quality_basis preserve the reported result.
                low_quality_reasons is a JSON string array. good_fraction/fair_fraction/min_samples_per_target,
                *_targets_measured/*_targets_required are the saved thresholds and counts.
                residual_median_px/validation_median_px are stored browser measurements, not export-time calculations.
                viewport_width/viewport_height/device_pixel_ratio describe the calibration viewport.
                started_at/finished_at are browser times; received_at is server receipt time.
                Raw target residual rows are outside this increment; camera images/video are never included.
                """;
    }
}
