package com.cs_42_3.surveyplatformbackend.study.api;

import com.cs_42_3.surveyplatformbackend.common.exception.ErrorResponse;
import com.cs_42_3.surveyplatformbackend.security.CurrentResearcher;
import com.cs_42_3.surveyplatformbackend.study.api.dto.CloseStudyRequest;
import com.cs_42_3.surveyplatformbackend.study.api.dto.StudyResponse;
import com.cs_42_3.surveyplatformbackend.study.service.ParticipationLinks;
import com.cs_42_3.surveyplatformbackend.study.service.StudyClosureService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/**
 * Exposes the owner-only FR-16 closure command without deleting collected data.
 *
 * @author Simon Tian
 */
@RestController
@RequestMapping("/api/studies")
@RequiredArgsConstructor
@Tag(name = "Studies")
@SecurityRequirement(name = "bearerAuth")
public class StudyClosureController {
    private final StudyClosureService service;
    private final CurrentResearcher currentResearcher;
    private final ParticipationLinks links;

    @PostMapping(value = "/{studyId}/close", consumes = "application/json")
    @Operation(operationId = "closeStudy", summary = "Permanently close my collecting study",
            description = "FR-16: confirm this irreversible operation before calling. Send the numeric study version. "
                    + "Atomically changes COLLECTING to CLOSED and abandons active participant sessions. "
                    + "Completed sessions, answers and published content are retained. Old participant links return STUDY_CLOSED. "
                    + "Repeating closure of an owned CLOSED study succeeds without changing data, even with a stale version.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Closed or already closed; returns current version and closedAt.",
                    content = @Content(schema = @Schema(implementation = StudyResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid UUID, JSON or version.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required."),
            @ApiResponse(responseCode = "403", description = "RESEARCHER role required."),
            @ApiResponse(responseCode = "404", description = "STUDY_NOT_FOUND: missing or foreign-owned study.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "STUDY_NOT_CLOSABLE or STUDY_VERSION_CONFLICT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<StudyResponse> close(@PathVariable UUID studyId,
                                              @Valid @RequestBody CloseStudyRequest request) {
        var study = service.close(currentResearcher.getId(), studyId, request.version().longValue());
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(StudyResponse.from(study, links.forToken(study.getParticipationToken())));
    }
}
