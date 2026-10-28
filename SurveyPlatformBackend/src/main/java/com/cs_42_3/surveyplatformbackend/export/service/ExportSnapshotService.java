package com.cs_42_3.surveyplatformbackend.export.service;

import com.cs_42_3.surveyplatformbackend.export.storage.ExportStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.io.IOException;
import java.time.*;
import java.util.UUID;

/**
 * Uses a separate read-only snapshot while the worker retains its task lock.
 *
 * @author Simon Tian
 */
@Service
@RequiredArgsConstructor
public class ExportSnapshotService {
    private final QuestionnaireExportWriter questionnaire;
    private final ExportStorage storage;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true,
            isolation = Isolation.REPEATABLE_READ, rollbackFor = IOException.class)
    public Instant generate(UUID id, UUID study, UUID session) throws IOException {
        Instant snapshot = clock.instant();
        try (var output = storage.createTemporary(id)) {
            questionnaire.write(study, session, snapshot, output);
        }
        return snapshot;
    }
}
