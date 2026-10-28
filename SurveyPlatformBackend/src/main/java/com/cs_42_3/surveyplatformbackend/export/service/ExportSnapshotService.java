package com.cs_42_3.surveyplatformbackend.export.service;

import com.cs_42_3.surveyplatformbackend.export.storage.ExportStorage;
import com.cs_42_3.surveyplatformbackend.export.domain.ExportDataType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.io.IOException;
import java.time.*;
import java.util.*;

/**
 * Uses a separate read-only snapshot while the worker retains its task lock.
 *
 * @author Simon Tian
 */
@Service
@RequiredArgsConstructor
public class ExportSnapshotService {
    private final ExportArchiveWriter archive;
    private final ExportStorage storage;
    private final Clock clock;
    private final StudyExportWriter studies;

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true,
            isolation = Isolation.REPEATABLE_READ, rollbackFor = IOException.class)
    public Instant generateStudy(UUID id, UUID study, Set<ExportDataType> types) throws IOException {
        Instant snapshot = clock.instant();
        var workspace = storage.createWorkspace(id);
        try (var output = storage.createTemporary(id)) {
            studies.write(study, types, snapshot, output, workspace);
        }
        return snapshot;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true,
            isolation = Isolation.REPEATABLE_READ, rollbackFor = IOException.class)
    public Instant generate(UUID id, UUID study, UUID session, Set<ExportDataType> types) throws IOException {
        Instant snapshot = clock.instant();
        try (var output = storage.createTemporary(id)) {
            archive.write(study, session, types, snapshot, output);
        }
        return snapshot;
    }
}
