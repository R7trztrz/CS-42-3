package com.cs_42_3.surveyplatformbackend.export.service;

import com.cs_42_3.surveyplatformbackend.export.exception.ExportException;
import com.cs_42_3.surveyplatformbackend.eyetracking.service.EyeTrackingSummaryReadService;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.SubmitQuestionnaireAnswerRequest;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantAnswerNormalizer;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.domain.QuestionnaireSnapshotPayload;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.service.*;
import lombok.RequiredArgsConstructor;
import org.apache.commons.csv.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.zip.*;

/**
 * Streams saved questionnaire steps against immutable publication content in the caller's snapshot.
 *
 * @author Simon Tian
 */
@Component
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY, readOnly = true)
public class QuestionnaireExportWriter {
    private final JdbcTemplate jdbc;
    private final ObjectMapper mapper;
    private final QuestionnaireSnapshotReadService snapshots;
    private final ParticipantAnswerNormalizer normalizer;
    private final QuestionnaireBranchResolver branches;
    private final EyeTrackingSummaryReadService eyeTracking;

    public void write(UUID studyId, UUID sessionId, Instant snapshotAt, OutputStream output) throws IOException {
        var sessions = jdbc.query("""
                SELECT p.id, p.anonymous_participant_id, p.status, p.phase, p.abandonment_reason,
                    p.entered_at, p.consented_at, p.calibration_completed_at, p.browsing_completed_at,
                    p.completed_at, p.abandoned_at, p.last_activity_at, s.questionnaire_enabled
                FROM participant_sessions p JOIN studies s ON s.id = p.study_id
                WHERE p.study_id = ? AND p.id = ?
                """, (rs, n) -> new SessionRow(rs.getObject("anonymous_participant_id", UUID.class),
                rs.getString("status"), rs.getString("phase"), rs.getString("abandonment_reason"),
                instant(rs, "entered_at"), instant(rs, "consented_at"), instant(rs, "calibration_completed_at"),
                instant(rs, "browsing_completed_at"), instant(rs, "completed_at"), instant(rs, "abandoned_at"),
                instant(rs, "last_activity_at"), rs.getBoolean("questionnaire_enabled")), studyId, sessionId);
        if (sessions.size() != 1) throw ExportException.invalidData();
        SessionRow session = sessions.get(0);
        var quality = eyeTracking.getSummary(studyId, sessionId);
        try (var zip = new ZipOutputStream(output, StandardCharsets.UTF_8)) {
            var csv = csv(zip, "sessions.csv", "study_id", "session_id", "anonymous_participant_id", "status",
                    "phase", "abandonment_reason", "entered_at", "consented_at", "calibration_completed_at",
                    "browsing_completed_at", "completed_at", "abandoned_at", "last_activity_at",
                    "questionnaire_enabled", "eye_tracking_enabled", "calibration_state", "tracking_available",
                    "calibration_quality", "quality_basis", "unavailable_reason", "low_quality_reasons", "snapshot_at");
            csv.printRecord(studyId, sessionId, session.anonymousId(), session.status(), session.phase(),
                    session.reason(), session.entered(), session.consented(), session.calibrated(), session.browsed(),
                    session.completed(), session.abandoned(), session.activity(), session.questionnaireEnabled(),
                    quality.enabled(), quality.calibrationState(), quality.trackingAvailable(), quality.quality(),
                    quality.qualityBasis(), quality.unavailableReason(), mapper.writeValueAsString(quality.lowQualityReasons()),
                    snapshotAt);
            csv.flush();
            zip.closeEntry();

            // A skipped optional item has a step but no answer; an unvisited item has neither.
            long count = jdbc.queryForObject("SELECT count(*) FROM participant_questionnaire_steps WHERE session_id = ?",
                    Long.class, sessionId);
            if (!session.questionnaireEnabled() && count > 0) throw ExportException.invalidData();
            QuestionnaireSnapshotPayload snapshot = session.questionnaireEnabled() ? snapshots.getPayload(studyId) : null;
            if (snapshot != null && !studyId.equals(snapshot.studyId())) throw ExportException.invalidData();
            if (count > 0) {
                if (session.consented() == null) throw ExportException.invalidData();
                writeResponses(zip, studyId, sessionId, session, snapshot);
            } else if (session.questionnaireEnabled() && "COMPLETED".equals(session.status())) {
                throw ExportException.invalidData();
            }

            zip.putNextEntry(new ZipEntry("README.md"));
            String reason = !session.questionnaireEnabled() ? "The study has questionnaire collection disabled."
                    : "No questionnaire submissions were saved for this session at the export snapshot.";
            zip.write(readme(studyId, sessionId, snapshotAt, count, reason).getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
    }

    private void writeResponses(ZipOutputStream zip, UUID study, UUID sessionId, SessionRow session,
                                QuestionnaireSnapshotPayload snapshot) throws IOException {
        Map<UUID, QuestionnaireSnapshotPayload.Item> items = new HashMap<>();
        for (var item : snapshot.items()) {
            if (item.itemId() == null || items.put(item.itemId(), item) != null) throw ExportException.invalidData();
        }
        UUID first = snapshot.items().stream().min(Comparator.comparingInt(QuestionnaireSnapshotPayload.Item::position))
                .orElseThrow(ExportException::invalidData).itemId();
        var csv = csv(zip, "responses.csv", "study_id", "session_id", "questionnaire_item_id", "sequence_number",
                "question_type", "question_text", "response_status", "answer_payload", "answer_text",
                "submitted_at", "answered_at");
        // A server-side cursor avoids buffering all answer payloads in memory.
        jdbc.query(connection -> {
            var statement = connection.prepareStatement("""
                    SELECT q.item_id, q.sequence_number, q.next_item_id, q.submitted_at,
                        a.id AS answer_id, a.question_type, a.answer_payload, a.answered_at
                    FROM participant_questionnaire_steps q LEFT JOIN participant_answers a ON a.step_id = q.id
                    WHERE q.session_id = ? ORDER BY q.sequence_number
                    """);
            statement.setObject(1, sessionId);
            statement.setFetchSize(256);
            return statement;
        }, (org.springframework.jdbc.core.ResultSetExtractor<Void>) rs -> {
            UUID expected = first;
            Instant previous = session.consented();
            int sequence = 0;
            Set<UUID> visited = new HashSet<>();
            while (rs.next()) {
                UUID itemId = rs.getObject("item_id", UUID.class);
                var item = items.get(itemId);
                Instant submitted = instant(rs, "submitted_at");
                if (item == null || !Objects.equals(expected, itemId) || !visited.add(itemId)
                        || rs.getInt("sequence_number") != ++sequence || submitted == null
                        || submitted.isBefore(previous)) throw ExportException.invalidData();
                String raw = rs.getString("answer_payload");
                boolean answered = rs.getObject("answer_id") != null;
                Instant answeredAt = instant(rs, "answered_at");
                if (answered && (raw == null || !item.type().name().equals(rs.getString("question_type"))
                        || answeredAt == null || answeredAt.isBefore(session.consented())
                        || answeredAt.isAfter(submitted))) throw ExportException.invalidData();
                try {
                    var request = answered ? request(item, raw)
                            : new SubmitQuestionnaireAnswerRequest(null, null, null, null, true);
                    var normalized = normalizer.normalize(item, request);
                    expected = branches.resolve(snapshot, itemId, normalized.standardized()).nextItemId();
                    if (!Objects.equals(expected, rs.getObject("next_item_id", UUID.class))) {
                        throw ExportException.invalidData();
                    }
                    csv.printRecord(study, sessionId, itemId, sequence, item.type(), item.questionText(),
                            answered ? "ANSWERED" : "SKIPPED", raw,
                            answered ? answerText(item, request) : null, submitted, answeredAt);
                } catch (IOException failure) {
                    throw new UncheckedIOException(failure);
                } catch (RuntimeException invalid) {
                    if (invalid instanceof UncheckedIOException) throw invalid;
                    throw ExportException.invalidData();
                }
                previous = submitted;
            }
            if ("COMPLETED".equals(session.status()) && expected != null) throw ExportException.invalidData();
            return null;
        });
        csv.flush();
        zip.closeEntry();
    }

    private SubmitQuestionnaireAnswerRequest request(QuestionnaireSnapshotPayload.Item item, String raw) {
        JsonNode node = mapper.readTree(raw);
        if (!node.isObject() || node.size() != 1) throw ExportException.invalidData();
        return switch (item.type()) {
            case SINGLE_CHOICE -> new SubmitQuestionnaireAnswerRequest(
                    UUID.fromString(string(node.get("optionId"))), null, null, null, false);
            case MULTI_CHOICE -> {
                JsonNode values = node.get("optionIds");
                if (values == null || !values.isArray()) throw ExportException.invalidData();
                List<UUID> ids = new ArrayList<>();
                for (JsonNode value : values) ids.add(UUID.fromString(string(value)));
                if (new HashSet<>(ids).size() != ids.size()) throw ExportException.invalidData();
                yield new SubmitQuestionnaireAnswerRequest(null, ids, null, null, false);
            }
            case SCALE -> {
                JsonNode value = node.get("value");
                if (value == null || !value.isIntegralNumber() || !value.canConvertToInt()) {
                    throw ExportException.invalidData();
                }
                yield new SubmitQuestionnaireAnswerRequest(null, null, value.intValue(), null, false);
            }
            case TEXT -> new SubmitQuestionnaireAnswerRequest(null, null, null, string(node.get("text")), false);
        };
    }

    private String answerText(QuestionnaireSnapshotPayload.Item item, SubmitQuestionnaireAnswerRequest request) {
        return switch (item.type()) {
            case SINGLE_CHOICE -> optionText(item, request.optionId());
            case MULTI_CHOICE -> mapper.writeValueAsString(request.optionIds().stream()
                    .map(id -> optionText(item, id)).toList());
            case SCALE -> request.scaleValue().toString();
            case TEXT -> request.textValue();
        };
    }

    private String optionText(QuestionnaireSnapshotPayload.Item item, UUID id) {
        return item.options().stream().filter(o -> o.optionId().equals(id)).findFirst()
                .orElseThrow(ExportException::invalidData).optionText();
    }

    private String string(JsonNode value) {
        if (value == null || !value.isString()) throw ExportException.invalidData();
        return value.asString();
    }

    private CSVPrinter csv(ZipOutputStream zip, String name, String... headers) throws IOException {
        zip.putNextEntry(new ZipEntry(name));
        // Flush each entry without closing the shared ZIP stream.
        return new CSVPrinter(new OutputStreamWriter(zip, StandardCharsets.UTF_8),
                CSVFormat.RFC4180.builder().setHeader(headers).get());
    }

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

    private String readme(UUID study, UUID session, Instant snapshot, long count, String reason) {
        return """
                # Questionnaire export — schema v1

                Study: %s
                Session: %s
                Snapshot transaction started at: %s
                Selected categories: QUESTIONNAIRE
                Saved questionnaire steps: %d

                CSV uses UTF-8 without BOM and RFC 4180 escaping. All timestamps use UTC ISO-8601.
                Empty cells represent absent values; an empty TEXT answer remains distinguishable by ANSWERED.
                Text is preserved as raw research data, including formula-like prefixes; import CSV columns
                as text in spreadsheet tools rather than enabling formula evaluation.
                Session and participant IDs are anonymous identifiers, not counts of distinct people.
                This is a consistent export of persisted records, not proof that every browser event arrived.

                ## sessions.csv
                study_id/session_id identify the source; anonymous_participant_id is the per-study alias.
                status is IN_PROGRESS/COMPLETED/ABANDONED; phase is the persisted participation stage.
                abandonment_reason and lifecycle *_at columns are the stored state and timestamps.
                questionnaire_enabled/eye_tracking_enabled are study collection switches.
                calibration_state, tracking_available, calibration_quality, quality_basis,
                unavailable_reason and low_quality_reasons describe final calibration metadata.
                Missing final calibration values are blank; low_quality_reasons is a JSON array.
                tracking_available is not proof that gaze events were saved. snapshot_at is export read time.

                ## responses.csv
                One row per saved questionnaire step, in sequence_number order.
                study_id/session_id identify the source; questionnaire_item_id identifies the published item.
                question_type/question_text come from the immutable publication, never the current question bank.
                response_status: ANSWERED has an answer row; SKIPPED is an intentionally unanswered optional item.
                answer_payload preserves the stored JSON (optionId, optionIds, value or text).
                answer_text resolves published option labels; MULTI_CHOICE uses a JSON string array.
                submitted_at is the step submission time; answered_at exists only for saved answers.
                Branch-unvisited and not-yet-submitted items have no rows, and are not inferred as missing answers.
                In-progress and abandoned sessions may legitimately contain a partial questionnaire path.
                %s

                Gaze and behavior files are not included because those categories were not selected.
                Authentication credentials and session tokens are never exported.
                """.formatted(study, session, snapshot, count,
                count == 0 ? "responses.csv was omitted: " + reason : "responses.csv is included.");
    }

    /**
     * Allow-listed session columns needed by the archive writer.
     *
     * @author Simon Tian
     */
    private record SessionRow(UUID anonymousId, String status, String phase, String reason, Instant entered,
            Instant consented, Instant calibrated, Instant browsed, Instant completed, Instant abandoned,
            Instant activity, boolean questionnaireEnabled) { }
}
