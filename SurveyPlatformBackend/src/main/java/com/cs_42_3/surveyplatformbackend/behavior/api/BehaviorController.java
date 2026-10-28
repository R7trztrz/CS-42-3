package com.cs_42_3.surveyplatformbackend.behavior.api;

import com.cs_42_3.surveyplatformbackend.behavior.api.dto.*;
import com.cs_42_3.surveyplatformbackend.behavior.service.BehaviorBatchService;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.*;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.io.IOException;

/**
 * Accepts bounded JSON batches authenticated by the existing participant-session token filter.
 *
 * @author Simon Tian
 */
@RestController
@RequestMapping("/api/participant-session")
@RequiredArgsConstructor
@Tag(name = "Participant behavior")
@SecurityRequirement(name = "participantSessionToken")
public class BehaviorController {
    private final BehaviorBatchService service;
    private final BehaviorBatchRequestReader reader;

    @PostMapping(value = "/behavior-batches", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(operationId = "submitBehaviorBatch", summary = "Store one batch of raw participant behavior",
            description = "Use X-Participant-Session-Token, not a researcher JWT. Identity comes from the token. "
                    + "Retries must reuse batchId, streamId, sequence and event content. A new page collection instance "
                    + "uses a new streamId; sequence starts at 1 per instance and may arrive out of order. "
                    + "Only consented IN_PROGRESS sessions in BROWSING/QUESTIONNAIRE while the study is COLLECTING "
                    + "accept uploads. No event values or typed input text are inferred. All events commit atomically.")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = BehaviorBatchRequest.class),
            examples = @ExampleObject(value = """
                    {"schemaVersion":1,"batchId":"36db590d-2945-4bd3-a16a-640289316fcb",
                    "streamId":"05520822-f473-4ad2-bf0c-a04d15df4880","sequence":1,"events":[
                    {"type":"click","timestamp":1793145600000,"sessionElapsedMs":1250.5,"pageId":"feed-page",
                    "x":320,"y":180,"componentId":"post-1","componentType":"PostWidget"},
                    {"type":"keydown","timestamp":1793145600100,"sessionElapsedMs":1350.5,"pageId":"feed-page",
                    "code":"ArrowDown","componentId":null,"componentType":null}]}
                    """)))
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "New batch and all events stored."),
            @ApiResponse(responseCode = "200", description = "Identical batch already stored; original acknowledgement returned."),
            @ApiResponse(responseCode = "400", description = "BEHAVIOR_BATCH_INVALID: invalid JSON, fields, version or event payload."),
            @ApiResponse(responseCode = "401", description = "PARTICIPANT_SESSION_UNAUTHORIZED: missing, invalid or deleted session token."),
            @ApiResponse(responseCode = "403", description = "Participant authority required."),
            @ApiResponse(responseCode = "404", description = "PARTICIPANT_SESSION_NOT_FOUND."),
            @ApiResponse(responseCode = "409", description = "Conflicting batch identity/sequence, ended session or invalid collection phase."),
            @ApiResponse(responseCode = "410", description = "STUDY_CLOSED: stop retrying; no late writes are accepted."),
            @ApiResponse(responseCode = "413", description = "BEHAVIOR_BATCH_TOO_LARGE: byte or event-count limit exceeded."),
            @ApiResponse(responseCode = "415", description = "Content-Type must be application/json.")})
    public ResponseEntity<BehaviorBatchResponse> upload(@AuthenticationPrincipal ParticipantSessionPrincipal principal,
                                                       HttpServletRequest request) throws IOException {
        // Read a bounded stream rather than allowing MVC to allocate an unbounded JSON body first.
        var result = service.accept(principal, reader.read(request.getInputStream()));
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .cacheControl(CacheControl.noStore()).body(result.response());
    }
}
