package com.cs_42_3.surveyplatformbackend.export.service;

import com.cs_42_3.surveyplatformbackend.export.exception.ExportException;
import com.cs_42_3.surveyplatformbackend.export.domain.ExportSessionRecord;
import com.cs_42_3.surveyplatformbackend.export.storage.ExportArchive;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.SubmitQuestionnaireAnswerRequest;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantAnswerNormalizer;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.domain.QuestionnaireSnapshotPayload;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.*;
import tools.jackson.databind.*;
import java.io.*;
import java.sql.*;
import java.time.Instant;
import java.util.*;

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

    /** Returns archive notes while writing only the selected questionnaire category. */
    public String write(ExportArchive archive, ExportSessionRecord session) throws IOException {
        UUID studyId = session.studyId(), sessionId = session.sessionId();
        long count = jdbc.queryForObject("SELECT count(*) FROM participant_questionnaire_steps WHERE session_id = ?",
                Long.class, sessionId);
        if (!session.questionnaireEnabled() && count > 0) throw ExportException.invalidData();
        QuestionnaireSnapshotPayload snapshot = session.questionnaireEnabled() ? snapshots.getPayload(studyId) : null;
        if (snapshot != null && !studyId.equals(snapshot.studyId())) throw ExportException.invalidData();
        if (count > 0) {
            if (session.consented() == null) throw ExportException.invalidData();
            writeResponses(archive, studyId, sessionId, session, snapshot);
        } else if (session.questionnaireEnabled() && "COMPLETED".equals(session.status())) {
            throw ExportException.invalidData();
        }
        String reason = !session.questionnaireEnabled() ? "The study has questionnaire collection disabled."
                : "No questionnaire submissions were saved for this session at the export snapshot.";
        return "Saved questionnaire steps: " + count + "\n"
                + (count == 0 ? "responses.csv was omitted: " + reason : "responses.csv is included.");
    }

    private void writeResponses(ExportArchive archive, UUID study, UUID sessionId, ExportSessionRecord session,
                                QuestionnaireSnapshotPayload snapshot) throws IOException {
        Map<UUID, QuestionnaireSnapshotPayload.Item> items = new HashMap<>();
        for (var item : snapshot.items()) {
            if (item.itemId() == null || items.put(item.itemId(), item) != null) throw ExportException.invalidData();
        }
        UUID first = snapshot.items().stream().min(Comparator.comparingInt(QuestionnaireSnapshotPayload.Item::position))
                .orElseThrow(ExportException::invalidData).itemId();
        var csv = archive.csv("responses.csv", "study_id", "session_id", "questionnaire_item_id", "sequence_number",
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
        archive.finish(csv);
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

    private Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp value = rs.getTimestamp(column);
        return value == null ? null : value.toInstant();
    }

}
