package com.cs_42_3.surveyplatformbackend.feed.api;

import com.cs_42_3.surveyplatformbackend.feed.api.dto.FeedResponse;
import com.cs_42_3.surveyplatformbackend.feed.api.dto.SaveFeedRequest;
import com.cs_42_3.surveyplatformbackend.feed.service.StudyFeedService;
import com.cs_42_3.surveyplatformbackend.security.CurrentResearcher;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.ObjectMapper;
import java.util.UUID;

/**
 * Exposes FR-30 feed retrieval and complete draft replacement.
 *
 * @author Simon Tian
 */
@RestController
@RequestMapping("/api/studies/{studyId}/feed")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Study feeds")
@ApiResponses({
        @ApiResponse(responseCode = "400", description = "Invalid UUID, JSON or request fields; framework error bodies are not yet standardized."),
        @ApiResponse(responseCode = "401", description = "Authentication required."),
        @ApiResponse(responseCode = "403", description = "RESEARCHER role required."),
        @ApiResponse(responseCode = "404", description = "STUDY_NOT_FOUND or FEED_NOT_FOUND.")
})
public class StudyFeedController {
    private final StudyFeedService service;
    private final CurrentResearcher currentResearcher;
    private final ObjectMapper mapper;

    @GetMapping
    @Operation(operationId = "getStudyFeed", summary = "Get my study feed",
            description = "Available in every study state. Placeholder content may be null. Version belongs to the feed, not the study.")
    public FeedResponse getFeed(@PathVariable UUID studyId) {
        return FeedResponse.from(service.getFeed(currentResearcher.getId(), studyId), mapper);
    }

    @PutMapping(consumes = "application/json")
    @Operation(operationId = "saveStudyFeed", summary = "Replace my draft feed",
            description = "DRAFT only. Supply the complete content and numeric feed version. "
                    + "Internal document validation is deferred. Template metadata is retained. "
                    + "Use the returned version for the next save; an unchanged document may retain its version.")
    @ApiResponse(responseCode = "409", description = "STUDY_NOT_EDITABLE or FEED_VERSION_CONFLICT.")
    public FeedResponse saveFeed(@PathVariable UUID studyId, @Valid @RequestBody SaveFeedRequest request) {
        return FeedResponse.from(service.saveFeed(currentResearcher.getId(), studyId,
                request.content().toString(), request.version().longValue()), mapper);
    }
}
