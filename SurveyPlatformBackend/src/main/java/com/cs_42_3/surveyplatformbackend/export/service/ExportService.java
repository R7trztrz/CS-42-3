package com.cs_42_3.surveyplatformbackend.export.service;

import com.cs_42_3.surveyplatformbackend.export.api.dto.*;
import org.springframework.core.io.InputStreamResource;
import java.util.UUID;

/**
 * Researcher-facing contract for scoped asynchronous export tasks.
 *
 * @author Simon Tian
 */
public interface ExportService {
    ExportJobResponse create(UUID owner, UUID study, UUID session, CreateExportRequest request);
    ExportJobResponse get(UUID owner, UUID study, UUID export);
    InputStreamResource download(UUID owner, UUID study, UUID export);
}
