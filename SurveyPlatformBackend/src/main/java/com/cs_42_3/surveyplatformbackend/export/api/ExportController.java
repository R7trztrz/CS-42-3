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

    @PostMapping("/participants/{sessionId}/exports")
    @Operation(summary = "Create a questionnaire export for one session",
            description = "Returns a durable asynchronous task, not the ZIP. Currently supports only QUESTIONNAIRE. "
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
                    + "and responses.csv when saved steps exist. Credentials are never included. "
                    + "Returns 409 before completion, 410 after expiry or file loss, and 404 after study deletion.")
    public ResponseEntity<InputStreamResource> download(@PathVariable UUID studyId, @PathVariable UUID exportId) {
        var file = service.download(researcher.getId(), studyId, exportId);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename("session-export-" + exportId + ".zip").build().toString())
                .contentType(MediaType.parseMediaType("application/zip")).body(file);
    }
}
