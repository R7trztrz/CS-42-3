package com.cs_42_3.surveyplatformbackend.eyetracking.service;

import com.cs_42_3.surveyplatformbackend.eyetracking.api.dto.GazeBatchRequest;
import com.cs_42_3.surveyplatformbackend.eyetracking.api.dto.GazeBatchResponse;
import com.cs_42_3.surveyplatformbackend.eyetracking.config.EyeTrackingProperties;
import com.cs_42_3.surveyplatformbackend.eyetracking.domain.CalibrationQuality;
import com.cs_42_3.surveyplatformbackend.eyetracking.domain.GazeAttribution;
import com.cs_42_3.surveyplatformbackend.eyetracking.domain.LowQualityReason;
import com.cs_42_3.surveyplatformbackend.eyetracking.exception.EyeTrackingRequestException;
import com.cs_42_3.surveyplatformbackend.eyetracking.repository.CalibrationRecordRepository;
import com.cs_42_3.surveyplatformbackend.eyetracking.repository.GazeRecordRepository;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionPrincipal;
import com.cs_42_3.surveyplatformbackend.participation.exception.ParticipationException;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantCollectionActivityPort;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantCollectionPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Accepts FR-50 gaze batches for the browsing phase.
 *
 * <p>The payload is parsed and validated before any lock is taken. The M5 collection policy is
 * then asserted, the batch and its events are stored, and accepted activity is recorded, all in
 * one transaction as the M5 ports require. {@code (sessionId, sequence)} is the idempotency key:
 * an identical replay returns the stored batch and a different batch under the same key is a
 * conflict. A batch is accepted or rejected whole; nothing is stored for a rejected one.
 */
@Service
@RequiredArgsConstructor
public class GazeBatchService {
    private static final int MAX_PAGE_ID_LENGTH = 128;
    private static final int MAX_WIDGET_ID_LENGTH = 255;

    private final GazeRecordRepository gaze;
    private final CalibrationRecordRepository calibrations;
    private final ParticipantCollectionPolicy collectionPolicy;
    private final ParticipantCollectionActivityPort collectionActivity;
    private final EyeTrackingProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional
    public BatchResult accept(ParticipantSessionPrincipal principal, GazeBatchRequest batch) {
        UUID sessionId = principal.sessionId();
        if (!batch.sessionId().equals(sessionId)) {
            throw EyeTrackingRequestException.invalid(
                    "sessionId does not match the authenticated participant session");
        }
        if (batch.eventCount() > properties.maxGazeEventsPerBatch()) {
            throw EyeTrackingRequestException.invalid(
                    "eventCount exceeds " + properties.maxGazeEventsPerBatch());
        }
        if (batch.droppedEventsTotal() < batch.droppedEvents()) {
            throw EyeTrackingRequestException.invalid(
                    "droppedEventsTotal must not be smaller than droppedEvents");
        }
        if (batch.payload().length() > properties.maxGazePayloadChars()) {
            throw EyeTrackingRequestException.invalid("payload is too large");
        }
        String requestHash = RequestDigest.sha256(String.join("\n",
                batch.batchId(),
                Integer.toString(batch.eventCount()),
                Long.toString(batch.droppedEvents()),
                Long.toString(batch.droppedEventsTotal()),
                batch.payload()
        ));
        List<ValidatedGazeEvent> events = parseEvents(sessionId, batch);

        collectionPolicy.assertCollectionAllowed(sessionId, ParticipantCollectionPolicy.CollectionKind.GAZE);

        var existing = gaze.findBatch(sessionId, batch.sequence());
        if (existing.isPresent()) {
            var stored = existing.get();
            if (!stored.batchId().equals(batch.batchId()) || !stored.requestHash().equals(requestHash)) {
                throw ParticipationException.idempotencyConflict();
            }
            return new BatchResult(false, new GazeBatchResponse(
                    batch.sequence(),
                    stored.batchId(),
                    stored.eventCount(),
                    stored.receivedAt()
            ));
        }
        verifyCalibration(sessionId, events);

        Instant receivedAt = clock.instant().truncatedTo(ChronoUnit.MICROS);
        gaze.insertBatch(
                sessionId,
                batch.sequence(),
                batch.batchId(),
                batch.eventCount(),
                batch.droppedEvents(),
                batch.droppedEventsTotal(),
                requestHash,
                receivedAt
        );
        gaze.insertEvents(sessionId, batch.sequence(), events);
        collectionActivity.recordAcceptedBatch(sessionId, ParticipantCollectionPolicy.CollectionKind.GAZE);
        return new BatchResult(true, new GazeBatchResponse(
                batch.sequence(),
                batch.batchId(),
                batch.eventCount(),
                receivedAt
        ));
    }

    private List<ValidatedGazeEvent> parseEvents(UUID sessionId, GazeBatchRequest batch) {
        GazeEventPayload[] payload;
        try {
            payload = objectMapper.readValue(batch.payload(), GazeEventPayload[].class);
        } catch (JacksonException exception) {
            throw EyeTrackingRequestException.invalid("payload must be a JSON array of gaze events");
        }
        if (payload == null) {
            throw EyeTrackingRequestException.invalid("payload must be a JSON array of gaze events");
        }
        if (payload.length != batch.eventCount()) {
            throw EyeTrackingRequestException.invalid("eventCount does not match the payload");
        }
        List<ValidatedGazeEvent> events = new ArrayList<>(payload.length);
        for (int index = 0; index < payload.length; index++) {
            events.add(validateEvent(sessionId, index, payload[index]));
        }
        return events;
    }

    private ValidatedGazeEvent validateEvent(UUID sessionId, int index, GazeEventPayload event) {
        String field = "payload[" + index + "]";
        if (event == null) {
            throw EyeTrackingRequestException.invalid(field + " must be a gaze event");
        }
        if (event.schemaVersion() == null || event.schemaVersion() != 1) {
            throw EyeTrackingRequestException.invalid(field + ".schemaVersion must be 1");
        }
        if (!sessionId.equals(uuid(event.sessionId(), field + ".sessionId"))) {
            throw EyeTrackingRequestException.invalid(
                    field + ".sessionId does not match the authenticated participant session");
        }
        String pageId = event.pageId();
        if (pageId == null || pageId.isBlank() || pageId.length() > MAX_PAGE_ID_LENGTH) {
            throw EyeTrackingRequestException.invalid(field + ".pageId is invalid");
        }
        double viewportWidth = finite(event.viewportWidth(), field + ".viewportWidth");
        double viewportHeight = finite(event.viewportHeight(), field + ".viewportHeight");
        double devicePixelRatio = finite(event.devicePixelRatio(), field + ".devicePixelRatio");
        if (viewportWidth < 0 || viewportHeight < 0 || devicePixelRatio <= 0) {
            throw EyeTrackingRequestException.invalid(field + " has an invalid viewport");
        }
        GazeAttribution attribution = enumValue(
                GazeAttribution.class, event.attribution(), field + ".attribution");
        String widgetId = event.widgetId();
        if (attribution == GazeAttribution.WIDGET) {
            if (widgetId == null || widgetId.isBlank() || widgetId.length() > MAX_WIDGET_ID_LENGTH) {
                throw EyeTrackingRequestException.invalid(field + ".widgetId is invalid");
            }
        } else if (widgetId != null) {
            throw EyeTrackingRequestException.invalid(
                    field + ".widgetId must be null unless attribution is WIDGET");
        }
        List<String> reasonNames = event.calibrationLowQualityReasons();
        if (reasonNames == null || reasonNames.size() > LowQualityReason.values().length) {
            throw EyeTrackingRequestException.invalid(
                    field + ".calibrationLowQualityReasons is invalid");
        }
        List<LowQualityReason> reasons = new ArrayList<>(reasonNames.size());
        for (String name : reasonNames) {
            reasons.add(enumValue(LowQualityReason.class, name,
                    field + ".calibrationLowQualityReasons"));
        }
        if (new HashSet<>(reasons).size() != reasons.size()) {
            throw EyeTrackingRequestException.invalid(
                    field + ".calibrationLowQualityReasons must not repeat a reason");
        }
        double x = finite(event.x(), field + ".x");
        double y = finite(event.y(), field + ".y");
        double scrollX = finite(event.scrollX(), field + ".scrollX");
        double scrollY = finite(event.scrollY(), field + ".scrollY");
        // pageX/pageY are not stored, so they must be exactly what the stored columns give back.
        // The browser computes them as x + scrollX in IEEE doubles, which Java reproduces exactly.
        if (finite(event.pageX(), field + ".pageX") != x + scrollX) {
            throw EyeTrackingRequestException.invalid(field + ".pageX must equal x + scrollX");
        }
        if (finite(event.pageY(), field + ".pageY") != y + scrollY) {
            throw EyeTrackingRequestException.invalid(field + ".pageY must equal y + scrollY");
        }
        return new ValidatedGazeEvent(
                index,
                pageId,
                x,
                y,
                scrollX,
                scrollY,
                viewportWidth,
                viewportHeight,
                devicePixelRatio,
                widgetId,
                attribution,
                EyeTrackingTimestamps.parse(event.timestamp(), field + ".timestamp"),
                // May be negative: readings that predate the clock origin keep their order.
                finite(event.sessionElapsedMs(), field + ".sessionElapsedMs"),
                uuid(event.calibrationAttemptId(), field + ".calibrationAttemptId"),
                enumValue(CalibrationQuality.class, event.calibrationQuality(),
                        field + ".calibrationQuality"),
                List.copyOf(reasons)
        );
    }

    /**
     * Every event must name an attempt recorded for this session that handed off a usable
     * engine, and repeat that attempt's quality markers, so stored gaze can always be joined to
     * the calibration it was predicted under.
     */
    private void verifyCalibration(UUID sessionId, List<ValidatedGazeEvent> events) {
        Map<UUID, CalibrationRecordRepository.TrackingCalibration> recorded =
                calibrations.findTrackingCalibrations(sessionId);
        for (ValidatedGazeEvent event : events) {
            String field = "payload[" + event.index() + "]";
            var calibration = recorded.get(event.calibrationAttemptId());
            if (calibration == null) {
                throw EyeTrackingRequestException.invalid(field
                        + ".calibrationAttemptId is not a calibration attempt of this session");
            }
            if (!calibration.trackingAvailable()) {
                throw EyeTrackingRequestException.invalid(field
                        + ".calibrationAttemptId names an attempt without a usable eye tracker");
            }
            // The attempt's reasons are stored one per row, so they compare as a set.
            if (calibration.quality() != event.calibrationQuality()
                    || !calibration.lowQualityReasons().equals(Set.copyOf(event.calibrationLowQualityReasons()))) {
                throw EyeTrackingRequestException.invalid(field
                        + " calibration quality does not match the recorded attempt");
            }
        }
    }

    private static double finite(Double value, String field) {
        if (value == null || !Double.isFinite(value)) {
            throw EyeTrackingRequestException.invalid(field + " must be a finite number");
        }
        return value;
    }

    private static UUID uuid(String value, String field) {
        if (value == null) {
            throw EyeTrackingRequestException.invalid(field + " is required");
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            throw EyeTrackingRequestException.invalid(field + " must be a UUID");
        }
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String value, String field) {
        if (value == null) {
            throw EyeTrackingRequestException.invalid(field + " is required");
        }
        try {
            return Enum.valueOf(type, value);
        } catch (IllegalArgumentException exception) {
            throw EyeTrackingRequestException.invalid(field + " has an unknown value");
        }
    }

    /** Whether this call created the batch, and what to acknowledge. */
    public record BatchResult(boolean created, GazeBatchResponse response) {
    }
}
