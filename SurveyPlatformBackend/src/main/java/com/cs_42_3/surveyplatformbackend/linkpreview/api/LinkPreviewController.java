package com.cs_42_3.surveyplatformbackend.linkpreview.api;

import com.cs_42_3.surveyplatformbackend.linkpreview.api.dto.*;
import com.cs_42_3.surveyplatformbackend.linkpreview.service.LinkPreviewService;
import com.cs_42_3.surveyplatformbackend.security.CurrentResearcher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/**
 * Generates editable link previews without saving or changing the study feed.
 *
 * @author Simon Tian
 */
@RestController
@RequiredArgsConstructor
@Tag(name = "Study link previews")
@SecurityRequirement(name = "bearerAuth")
public class LinkPreviewController {
    private final LinkPreviewService service;
    private final CurrentResearcher researcher;

    @PostMapping(value = "/api/studies/{studyId}/link-previews", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Generate a post draft from a webpage",
            description = "Requires an owned DRAFT study. Imports an optional image; does not save the feed. Text is plain text; warnings describe partial results.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Preview ready, possibly with warnings and a null image."),
        @ApiResponse(responseCode = "400", description = "Invalid input or PREVIEW_URL_INVALID."),
        @ApiResponse(responseCode = "401", description = "Authentication required."),
        @ApiResponse(responseCode = "403", description = "RESEARCHER role required."),
        @ApiResponse(responseCode = "404", description = "STUDY_NOT_FOUND."),
        @ApiResponse(responseCode = "409", description = "STUDY_NOT_EDITABLE."),
        @ApiResponse(responseCode = "422", description = "PREVIEW_CONTENT_UNSUPPORTED."),
        @ApiResponse(responseCode = "502", description = "PREVIEW_FETCH_FAILED.")
    })
    public ResponseEntity<LinkPreviewResponse> preview(@PathVariable UUID studyId,
                                                        @Valid @RequestBody LinkPreviewRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore())
                .body(service.preview(researcher.getId(), studyId, request.url()));
    }
}
