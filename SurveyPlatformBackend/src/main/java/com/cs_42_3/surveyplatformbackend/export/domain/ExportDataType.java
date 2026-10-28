package com.cs_42_3.surveyplatformbackend.export.domain;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Data categories currently supported by the shared export pipeline.
 *
 * @author Simon Tian
 */
public enum ExportDataType {
    QUESTIONNAIRE, GAZE, BEHAVIOR;

    /** Stores selections in canonical order so equivalent requests have one representation. */
    public static String encode(Set<ExportDataType> types) {
        if (types == null || types.isEmpty() || types.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException("At least one supported export category is required.");
        }
        return types.stream().map(Enum::name).sorted().collect(Collectors.joining(","));
    }

    /** Reads both historical V21 tasks and newer combined tasks without inventing defaults. */
    public static Set<ExportDataType> decode(String value) {
        Set<ExportDataType> types = Arrays.stream(value.split(","))
                .map(ExportDataType::valueOf).collect(Collectors.toCollection(() -> EnumSet.noneOf(ExportDataType.class)));
        if (!encode(types).equals(value)) throw new IllegalStateException("Noncanonical export categories.");
        return Set.copyOf(types);
    }
}
