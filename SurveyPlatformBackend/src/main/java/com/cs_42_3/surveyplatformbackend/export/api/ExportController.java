package com.cs_42_3.surveyplatformbackend.export.api;

import com.cs_42_3.surveyplatformbackend.export.api.dto.*;
import com.cs_42_3.surveyplatformbackend.export.service.ExportService;
import com.cs_42_3.surveyplatformbackend.security.CurrentResearcher;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.UUID;

/**
 * Exposes owner-only task creation, polling and authenticated ZIP downloads for FR-56.
 *
 * @author Simon Tian
 */
@RestController
@RequestMapping("/api/studies/{studyId}")
@RequiredArgsConstructor
@Tag(name = "Data exports")
@SecurityRequirement(name = "bearerAuth")
public class ExportController {
    private final ExportService service;
    private final CurrentResearcher researcher;

    @PostMapping("/exports")
    @Operation(summary = "Create a study-wide data export",
            description = "FR-52: exports every persisted session in my study from one generation-time snapshot. "
                    + "Select any nonempty combination of QUESTIONNAIRE, GAZE and BEHAVIOR. "
                    + "An empty study produces sessions.csv headers and README. Poll the returned Location URI. "
                    + "State polling is supported; no live percentage is reported. Validation failures identify failedSessionId.")
    @ApiResponses({@ApiResponse(responseCode = "202", description = "Study export queued."),
            @ApiResponse(responseCode = "400", description = "Invalid UUID or category selection."),
            @ApiResponse(responseCode = "401", description = "Researcher authentication required."),
            @ApiResponse(responseCode = "403", description = "RESEARCHER role required."),
            @ApiResponse(responseCode = "404", description = "Missing or foreign-owned study.")})
    public ResponseEntity<ExportJobResponse> createStudy(@PathVariable UUID studyId,
                                                       @Valid @RequestBody CreateExportRequest request) {
        var job = service.createStudy(researcher.getId(), studyId, request);
        return ResponseEntity.accepted().location(URI.create("/api/studies/" + studyId + "/exports/" + job.id()))
                .cacheControl(CacheControl.noStore()).body(job);
    }

    @PostMapping("/participants/{sessionId}/exports")
    @Operation(summary = "Create a data export for one session",
            description = "Returns a durable asynchronous task, not the ZIP. Select any nonempty combination of QUESTIONNAIRE, GAZE and BEHAVIOR. "
                    + "Partial, completed and abandoned sessions are supported in collecting or closed studies.")
    @ApiResponses({@ApiResponse(responseCode = "202", description = "Task queued; poll the Location URI."),
            @ApiResponse(responseCode = "400", description = "Invalid UUID, body or unsupported category."),
            @ApiResponse(responseCode = "401", description = "Researcher authentication required."),
            @ApiResponse(responseCode = "403", description = "RESEARCHER role required."),
            @ApiResponse(responseCode = "404", description = "Missing/foreign study or session outside this study.")})
    public ResponseEntity<ExportJobResponse> create(@PathVariable UUID studyId, @PathVariable UUID sessionId,
                                                   @Valid @RequestBody CreateExportRequest request) {
        var job = service.create(researcher.getId(), studyId, sessionId, request);
        return ResponseEntity.accepted().location(URI.create("/api/studies/" + studyId + "/exports/" + job.id()))
                .cacheControl(CacheControl.noStore()).body(job);
    }

    @GetMapping("/exports/{exportId}")
    @Operation(summary = "Get my export task status", description = "Poll until COMPLETED or FAILED. "
            + "EXPIRED tasks require a new export. Snapshot time records generation read time, not request time.")
    public ResponseEntity<ExportJobResponse> get(@PathVariable UUID studyId, @PathVariable UUID exportId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.get(researcher.getId(), studyId, exportId));
    }

    @GetMapping(value = "/exports/{exportId}/download", produces = "application/zip")
    @Operation(summary = "Download my completed export ZIP",
            description = "Requires the owning researcher's JWT on every download. Contains sessions.csv, README.md "
                    + "and selected category files when records exist: responses.csv, gaze_events.csv, gaze_batches.csv "
                    + "calibration_attempts.csv, behavior_events.csv and behavior_batches.csv. "
                    + "Omitted files and delivery warnings are explained in README. "
                    + "Credentials are never included. "
                    + "Returns 409 before completion, 410 after expiry or file loss, and 404 after study deletion.")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID studyId, @PathVariable UUID exportId) {
        var owner = researcher.getId();
        var metadata = service.get(owner, studyId, exportId);
        var file = service.download(owner, studyId, exportId);
        String prefix = metadata.scope() == com.cs_42_3.surveyplatformbackend.export.domain.ExportScope.STUDY
                ? "study-export-" : "session-export-";
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(prefix + exportId + ".zip").build().toString())
                .contentType(MediaType.parseMediaType("application/zip")).body(file);
    }
}
