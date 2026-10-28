package com.cs_42_3.surveyplatformbackend.export.service.implementation;

import com.cs_42_3.surveyplatformbackend.common.exception.ErrorCode;
import com.cs_42_3.surveyplatformbackend.export.api.dto.*;
import com.cs_42_3.surveyplatformbackend.export.domain.*;
import com.cs_42_3.surveyplatformbackend.export.exception.ExportException;
import com.cs_42_3.surveyplatformbackend.export.repository.ExportJobRepository;
import com.cs_42_3.surveyplatformbackend.export.service.ExportService;
import com.cs_42_3.surveyplatformbackend.export.storage.ExportStorage;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.io.IOException;
import java.time.Clock;
import java.util.*;

/**
 * Authorizes every operation and opens files before releasing task locks to avoid cleanup races.
 *
 * @author Simon Tian
 */
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESEARCHER')")
public class ExportServiceImpl implements ExportService {
    private final ExportJobRepository jobs;
    private final ExportStorage storage;
    private final Clock clock;

    @Override
    @Transactional
    public ExportJobResponse create(UUID owner, UUID study, UUID session, CreateExportRequest request) {
        // Study-first shared locking serializes creation with permanent aggregate deletion.
        jobs.requireOwner(owner, study, true);
        jobs.requireSession(study, session);
        if (request == null || request.dataTypes() == null
                || !request.dataTypes().equals(Set.of(ExportDataType.QUESTIONNAIRE))) {
            throw new ExportException(ErrorCode.EXPORT_CATEGORY_UNSUPPORTED, HttpStatus.BAD_REQUEST,
                    "Select QUESTIONNAIRE; other export categories are not supported yet.");
        }
        return jobs.insert(study, session, clock.instant());
    }

    @Override
    @Transactional(readOnly = true)
    public ExportJobResponse get(UUID owner, UUID study, UUID export) {
        jobs.requireOwner(owner, study, false);
        var job = jobs.get(study, export, false);
        if (job.status() == ExportStatus.COMPLETED && !clock.instant().isBefore(job.expiresAt())) {
            return new ExportJobResponse(job.id(), job.studyId(), job.sessionId(), job.dataTypes(),
                    ExportStatus.EXPIRED, job.createdAt(), job.startedAt(), job.snapshotAt(), job.completedAt(),
                    job.expiresAt(), job.errorCode(), job.errorMessage());
        }
        return job;
    }

    @Override
    @Transactional
    public InputStreamResource download(UUID owner, UUID study, UUID export) {
        jobs.requireOwner(owner, study, false);
        var job = jobs.get(study, export, true);
        if (job.status() == ExportStatus.EXPIRED || job.expiresAt() != null
                && !clock.instant().isBefore(job.expiresAt())) {
            throw new ExportException(ErrorCode.EXPORT_EXPIRED, HttpStatus.GONE, "Export expired. Create a new task.");
        }
        if (job.status() != ExportStatus.COMPLETED) {
            throw new ExportException(ErrorCode.EXPORT_NOT_READY, HttpStatus.CONFLICT, "Export is not ready for download.");
        }
        try {
            // An open handle permits an in-flight download to finish if deletion or expiry follows.
            return new InputStreamResource(storage.open(export));
        } catch (IOException failure) {
            throw new ExportException(ErrorCode.EXPORT_FILE_UNAVAILABLE, HttpStatus.GONE,
                    "Export file is unavailable. Create a new task.");
        }
    }
}
