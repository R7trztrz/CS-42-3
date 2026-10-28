package com.cs_42_3.surveyplatformbackend.study.api;

import com.cs_42_3.surveyplatformbackend.common.exception.ErrorResponse;
import com.cs_42_3.surveyplatformbackend.security.CurrentResearcher;
import com.cs_42_3.surveyplatformbackend.study.api.dto.StudyParticipationResponse;
import com.cs_42_3.surveyplatformbackend.study.api.dto.StudyParticipantDetailResponse;
import com.cs_42_3.surveyplatformbackend.study.service.StudyParticipationService;
import io.swagger.v3.oas.annotations.*;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/**
 * Exposes the researcher-only FR-19 overview and FR-20 details without raw data or session credentials.
 *
 * @author Simon Tian
 */
@RestController
@RequestMapping("/api/studies")
@RequiredArgsConstructor
@Tag(name = "Studies")
@SecurityRequirement(name = "bearerAuth")
public class StudyParticipationController {
    private final StudyParticipationService service;
    private final CurrentResearcher currentResearcher;

    @GetMapping("/{studyId}/participants/{sessionId}")
    @Operation(operationId = "getStudyParticipant", summary = "Get a participant session in my study",
            description = "FR-20: returns anonymous identity, persisted state/timestamps, allow-listed device metadata "
                    + "and final calibration metadata in one read-only snapshot. Closed studies remain readable. "
                    + "No answers, behavior/gaze events, calibration points or authentication data are returned. "
                    + "RECORDED means a final calibration attempt was selected, including unavailable or abandoned outcomes; "
                    + "trackingAvailable is not proof that gaze events were saved. Missing final metadata remains null. "
                    + "This endpoint provides no export operation or download URL.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Owned session details.",
                    content = @Content(schema = @Schema(implementation = StudyParticipantDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid study or session UUID.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required."),
            @ApiResponse(responseCode = "403", description = "RESEARCHER role required."),
            @ApiResponse(responseCode = "404", description = "STUDY_NOT_FOUND for a missing/foreign study; "
                    + "PARTICIPANT_SESSION_NOT_FOUND for a missing session or a session outside this study.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<StudyParticipantDetailResponse> getParticipant(
            @PathVariable UUID studyId, @PathVariable UUID sessionId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.getParticipant(currentResearcher.getId(), studyId, sessionId));
    }

    @GetMapping("/{studyId}/participation")
    @Operation(operationId = "getStudyParticipation", summary = "Get my study participation overview and session page",
            description = "FR-19: available for owned studies in all lifecycle states. Counts created sessions, "
                    + "including declined-consent sessions, rather than link clicks or distinct people. "
                    + "Summary counts cover all sessions, independent of pagination. Reads persisted M5 states without "
                    + "changing them. Status ascending order is IN_PROGRESS, COMPLETED, ABANDONED; descending reverses it. "
                    + "Status ties use enteredAt descending, then sessionId ascending; time ties use sessionId ascending. "
                    + "The summary and page share a database snapshot. No answers, events, device payloads or tokens are returned.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Overview and page, possibly empty.",
                    content = @Content(schema = @Schema(implementation = StudyParticipationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid UUID, pagination or sorting parameters.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required."),
            @ApiResponse(responseCode = "403", description = "RESEARCHER role required."),
            @ApiResponse(responseCode = "404", description = "STUDY_NOT_FOUND: missing or foreign-owned study.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<StudyParticipationResponse> getParticipation(
            @PathVariable UUID studyId,
            @Parameter(description = "Zero-based page index.", schema = @Schema(minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(schema = @Schema(minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "20") int size,
            @Parameter(schema = @Schema(allowableValues = {"enteredAt", "status"}))
            @RequestParam(defaultValue = "enteredAt") String sortBy,
            @Parameter(schema = @Schema(allowableValues = {"asc", "desc"}))
            @RequestParam(defaultValue = "desc") String direction) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.getParticipation(
                currentResearcher.getId(), studyId, page, size, sortBy, direction));
    }
}
