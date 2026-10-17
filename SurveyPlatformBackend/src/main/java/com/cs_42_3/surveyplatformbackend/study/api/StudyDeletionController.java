package com.cs_42_3.surveyplatformbackend.study.api;

import com.cs_42_3.surveyplatformbackend.common.exception.ErrorResponse;
import com.cs_42_3.surveyplatformbackend.security.CurrentResearcher;
import com.cs_42_3.surveyplatformbackend.study.service.StudyDeletionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;

/**
 * Exposes the irreversible owner-only FR-18 deletion command.
 *
 * @author Simon Tian
 */
@RestController
@RequestMapping("/api/studies")
@RequiredArgsConstructor
@Tag(name = "Studies")
@SecurityRequirement(name = "bearerAuth")
public class StudyDeletionController {
    private final StudyDeletionService service;
    private final CurrentResearcher currentResearcher;

    @DeleteMapping("/{studyId}")
    @Operation(operationId = "deleteStudy", summary = "Permanently delete my study",
            description = "FR-18: confirm this irreversible operation before calling. Allowed in every study state. "
                    + "Send the current numeric study version as a query parameter. Removes the study, feed, questionnaire, "
                    + "sessions and answers. Shared templates and the question bank remain. Images become inaccessible "
                    + "immediately; physical file cleanup runs asynchronously with durable retries.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Study deleted.", content = @Content),
            @ApiResponse(responseCode = "400", description = "Invalid UUID or missing/invalid nonnegative version.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required."),
            @ApiResponse(responseCode = "403", description = "RESEARCHER role required."),
            @ApiResponse(responseCode = "404", description = "STUDY_NOT_FOUND: absent, already deleted or foreign-owned study.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "STUDY_VERSION_CONFLICT: reload before confirming deletion.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<Void> delete(@PathVariable UUID studyId, @RequestParam long version) {
        if (version < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Version must be a nonnegative 64-bit integer.");
        }
        service.delete(currentResearcher.getId(), studyId, version);
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build();
    }
}
