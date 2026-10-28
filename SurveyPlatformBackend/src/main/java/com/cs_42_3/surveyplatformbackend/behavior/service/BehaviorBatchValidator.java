package com.cs_42_3.surveyplatformbackend.behavior.service;

import com.cs_42_3.surveyplatformbackend.behavior.api.dto.*;
import com.cs_42_3.surveyplatformbackend.behavior.config.BehaviorProperties;
import com.cs_42_3.surveyplatformbackend.behavior.domain.*;
import com.cs_42_3.surveyplatformbackend.behavior.exception.BehaviorBatchException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;
import java.security.*;
import java.time.Instant;
import java.util.*;

/**
 * Validates typed events and hashes their canonical semantics rather than raw JSON formatting.
 *
 * @author Simon Tian
 */
@Component
@RequiredArgsConstructor
public class BehaviorBatchValidator {
    private final ObjectMapper mapper;
    private final BehaviorProperties properties;

    public ValidatedBehaviorBatch validate(BehaviorBatchRequest request) {
        if (request == null || !Objects.equals(request.schemaVersion(), 1) || request.batchId() == null
                || request.streamId() == null || request.sequence() == null || request.sequence() < 1
                || request.sequence() > 9_007_199_254_740_991L || request.events() == null || request.events().isEmpty()) {
            throw BehaviorBatchException.invalid("schemaVersion 1, batchId, streamId, a positive safe sequence and events are required.");
        }
        if (request.events().size() > properties.maxEventsPerBatch()) throw BehaviorBatchException.tooLarge();
        List<ValidatedBehaviorEvent> events = new ArrayList<>();
        List<Map<String, Object>> canonical = new ArrayList<>();
        for (int i = 0; i < request.events().size(); i++) {
            BehaviorEventRequest input = request.events().get(i);
            var event = validateEvent(input, i);
            events.add(event);
            Map<String, Object> normalized = new TreeMap<>();
            normalized.put("type", event.type().wireValue()); normalized.put("timestamp", input.timestamp());
            normalized.put("sessionElapsedMs", event.sessionElapsedMs()); normalized.put("pageId", event.pageId());
            normalized.put("componentId", event.componentId()); normalized.put("componentType", event.componentType());
            normalized.put("payload", mapper.readTree(event.payload())); canonical.add(normalized);
        }
        Map<String, Object> envelope = new TreeMap<>();
        envelope.put("schemaVersion", 1); envelope.put("batchId", request.batchId().toString());
        envelope.put("streamId", request.streamId().toString()); envelope.put("sequence", request.sequence());
        envelope.put("events", canonical);
        byte[] normalized = mapper.writeValueAsBytes(envelope);
        if (normalized.length > properties.maxPayloadBytes()) throw BehaviorBatchException.tooLarge();
        try {
            String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(normalized));
            return new ValidatedBehaviorBatch(request.batchId(), request.streamId(), request.sequence(), events, hash);
        } catch (NoSuchAlgorithmException unavailable) {
            throw new IllegalStateException("SHA-256 is unavailable.", unavailable);
        }
    }

    /** Validates a single stored or incoming event without collecting an entire batch. */
    public ValidatedBehaviorEvent validateEvent(BehaviorEventRequest input, int index) {
        String path = "events[" + index + "]";
        if (input == null) throw BehaviorBatchException.invalid(path + " must be an event object.");
        BehaviorEventType type = BehaviorEventType.parse(input.type());
        // Keep timestamps in a portable PostgreSQL/ISO range; never compare client time to server time.
        if (input.timestamp() == null || input.timestamp() < 0 || input.timestamp() > 253_402_300_799_999L) {
            throw BehaviorBatchException.invalid(path + ".timestamp must be epoch milliseconds between 1970 and year 9999.");
        }
        double elapsed = finite(input.sessionElapsedMs(), path + ".sessionElapsedMs");
        String page = text(input.pageId(), 128, false, path + ".pageId");
        String component = text(input.componentId(), 255, true, path + ".componentId");
        String componentType = text(input.componentType(), 128, true, path + ".componentType");
        Map<String, Object> payload = new TreeMap<>();
        switch (type) {
            case CLICK, MOUSEMOVE -> {
                absent(input.code(), input.scrollX(), input.scrollY(), input.hidden());
                payload.put("x", finite(input.x(), path + ".x"));
                payload.put("y", finite(input.y(), path + ".y"));
            }
            case KEYDOWN -> {
                absent(input.x(), input.y(), input.scrollX(), input.scrollY(), input.hidden());
                String code = text(input.code(), 64, false, path + ".code");
                if (!code.matches("[A-Za-z][A-Za-z0-9]{0,63}")) {
                    throw BehaviorBatchException.invalid(path + ".code must be a KeyboardEvent.code identifier.");
                }
                payload.put("code", code);
            }
            case SCROLL -> {
                absent(input.x(), input.y(), input.code(), input.hidden());
                payload.put("scrollX", finite(input.scrollX(), path + ".scrollX"));
                payload.put("scrollY", finite(input.scrollY(), path + ".scrollY"));
            }
            case VISIBILITY_CHANGE -> {
                absent(input.x(), input.y(), input.code(), input.scrollX(), input.scrollY());
                if (input.hidden() == null) throw BehaviorBatchException.invalid(path + ".hidden is required.");
                payload.put("hidden", input.hidden());
            }
        }
        String json = mapper.writeValueAsString(payload);
        return new ValidatedBehaviorEvent(index, type, Instant.ofEpochMilli(input.timestamp()), elapsed,
                page, component, componentType, json);
    }

    private double finite(Double value, String field) {
        if (value == null || !Double.isFinite(value)) throw BehaviorBatchException.invalid(field + " must be a finite number.");
        return value;
    }

    private String text(String value, int max, boolean nullable, String field) {
        if (value == null && nullable) return null;
        if (value == null || value.isBlank() || value.length() > max || value.indexOf('\0') >= 0
                || value.codePoints().anyMatch(point -> point >= 0xD800 && point <= 0xDFFF)) {
            throw BehaviorBatchException.invalid(field + " is missing, blank or exceeds its allowed length.");
        }
        return value;
    }

    private void absent(Object... values) {
        if (Arrays.stream(values).anyMatch(Objects::nonNull)) {
            throw BehaviorBatchException.invalid("Event contains payload fields that do not belong to its type.");
        }
    }
}
