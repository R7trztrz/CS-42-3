package com.cs_42_3.surveyplatformbackend.export.api.dto;

import com.cs_42_3.surveyplatformbackend.export.domain.*;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.*;

/**
 * Safe task metadata without filesystem paths, answers or participant credentials.
 *
 * @author Simon Tian
 */
public record ExportJobResponse(UUID id, UUID studyId,
        @Schema(description = "Target session; null for STUDY scope.", nullable = true) UUID sessionId,
        Set<ExportDataType> dataTypes,
        ExportStatus status, Instant createdAt, Instant startedAt, Instant snapshotAt,
        Instant completedAt, Instant expiresAt, String errorCode, String errorMessage,
        ExportScope scope,
        @Schema(description = "Session that failed validation/generation; null without session-specific failure.", nullable = true)
        UUID failedSessionId) {
    public ExportJobResponse {
        dataTypes = Set.copyOf(dataTypes);
    }

    /** Keeps existing session-oriented callers source-compatible with the additive response fields. */
    public ExportJobResponse(UUID id, UUID studyId, UUID sessionId, Set<ExportDataType> dataTypes,
            ExportStatus status, Instant createdAt, Instant startedAt, Instant snapshotAt,
            Instant completedAt, Instant expiresAt, String errorCode, String errorMessage) {
        this(id, studyId, sessionId, dataTypes, status, createdAt, startedAt, snapshotAt, completedAt,
                expiresAt, errorCode, errorMessage, sessionId == null ? ExportScope.STUDY : ExportScope.SESSION, null);
    }
}
