package com.cs_42_3.surveyplatformbackend.behavior.api;

import com.cs_42_3.surveyplatformbackend.behavior.api.dto.BehaviorBatchRequest;
import com.cs_42_3.surveyplatformbackend.behavior.config.BehaviorProperties;
import com.cs_42_3.surveyplatformbackend.behavior.exception.BehaviorBatchException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.*;
import java.io.*;
import java.util.*;

/**
 * Bounds bytes before JSON parsing and rejects unknown fields and primitive type coercion.
 *
 * @author Simon Tian
 */
@Component
@RequiredArgsConstructor
public class BehaviorBatchRequestReader {
    private static final Set<String> ROOT_FIELDS = Set.of("schemaVersion", "batchId", "streamId", "sequence", "events");
    private static final Set<String> EVENT_FIELDS = Set.of("type", "timestamp", "sessionElapsedMs", "pageId", "x", "y",
            "code", "scrollX", "scrollY", "hidden", "componentId", "componentType");
    private final ObjectMapper mapper;
    private final BehaviorProperties properties;

    public BehaviorBatchRequest read(InputStream input) throws IOException {
        byte[] body = input.readNBytes(properties.maxPayloadBytes() + 1);
        if (body.length > properties.maxPayloadBytes()) throw BehaviorBatchException.tooLarge();
        try {
            // A second JSON value must not be silently ignored by readTree's default settings.
            JsonNode root = mapper.reader().with(tools.jackson.databind.DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
                    .readTree(body);
            fields(root, ROOT_FIELDS, "Request");
            kind(root.get("schemaVersion"), "integer", "schemaVersion");
            kind(root.get("sequence"), "integer", "sequence");
            uuid(root.get("batchId"), "batchId");
            uuid(root.get("streamId"), "streamId");
            JsonNode events = root.get("events");
            if (events == null || !events.isArray() || events.isEmpty()) {
                throw BehaviorBatchException.invalid("events must be a nonempty array.");
            }
            if (events.size() > properties.maxEventsPerBatch()) throw BehaviorBatchException.tooLarge();
            for (int i = 0; i < events.size(); i++) {
                JsonNode event = events.get(i);
                String path = "events[" + i + "]";
                fields(event, EVENT_FIELDS, path);
                for (String field : List.of("type", "pageId", "code", "componentId", "componentType")) {
                    kind(event.get(field), "string", path + "." + field);
                }
                kind(event.get("timestamp"), "integer", path + ".timestamp");
                for (String field : List.of("sessionElapsedMs", "x", "y", "scrollX", "scrollY")) {
                    kind(event.get(field), "number", path + "." + field);
                }
                kind(event.get("hidden"), "boolean", path + ".hidden");
            }
            return mapper.treeToValue(root, BehaviorBatchRequest.class);
        } catch (JacksonException | IllegalArgumentException invalid) {
            throw BehaviorBatchException.invalid("Request must contain one valid behavior batch JSON object.");
        }
    }

    private void fields(JsonNode node, Set<String> allowed, String path) {
        if (node == null || !node.isObject()) throw BehaviorBatchException.invalid(path + " must be an object.");
        long known = allowed.stream().filter(node::has).count();
        if (known != node.size()) throw BehaviorBatchException.invalid(path + " contains unsupported fields.");
    }

    private void kind(JsonNode node, String expected, String path) {
        // Missing values are reported by the service validator; nullable attribution fields are permitted.
        if (node == null || node.isNull()) return;
        boolean valid = switch (expected) {
            case "integer" -> node.isIntegralNumber() && node.canConvertToLong();
            case "number" -> node.isNumber();
            case "string" -> node.isString();
            case "boolean" -> node.isBoolean();
            default -> false;
        };
        if (!valid) throw BehaviorBatchException.invalid(path + " must be a JSON " + expected + ".");
    }

    private void uuid(JsonNode node, String path) {
        if (node == null || node.isNull()) return;
        kind(node, "string", path);
        try {
            String value = node.asString();
            if (!UUID.fromString(value).toString().equalsIgnoreCase(value)) throw new IllegalArgumentException();
        } catch (IllegalArgumentException invalid) {
            throw BehaviorBatchException.invalid(path + " must be a canonical UUID.");
        }
    }
}
