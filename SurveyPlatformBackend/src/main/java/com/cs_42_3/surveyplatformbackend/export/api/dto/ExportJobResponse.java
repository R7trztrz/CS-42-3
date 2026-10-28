package com.cs_42_3.surveyplatformbackend.export.api.dto;

import com.cs_42_3.surveyplatformbackend.export.domain.*;
import java.time.Instant;
import java.util.*;

/**
 * Safe task metadata without filesystem paths, answers or participant credentials.
 *
 * @author Simon Tian
 */
public record ExportJobResponse(UUID id, UUID studyId, UUID sessionId, Set<ExportDataType> dataTypes,
        ExportStatus status, Instant createdAt, Instant startedAt, Instant snapshotAt,
        Instant completedAt, Instant expiresAt, String errorCode, String errorMessage) {
    public ExportJobResponse {
        dataTypes = Set.copyOf(dataTypes);
    }
}
