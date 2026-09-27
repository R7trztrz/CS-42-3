package com.cs_42_3.surveyplatformbackend.study.api;

import com.cs_42_3.surveyplatformbackend.security.CurrentResearcher;
import com.cs_42_3.surveyplatformbackend.study.api.dto.*;
import com.cs_42_3.surveyplatformbackend.study.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

/**
 * Exposes the owner-only FR-14 publication command.
 *
 * @author Simon Tian
 */
@RestController
@RequestMapping("/api/studies")
@RequiredArgsConstructor
@Tag(name = "Studies")
public class StudyPublicationController {
    private final StudyPublicationService service;
    private final CurrentResearcher currentResearcher;
    private final ParticipationLinks links;

    @PostMapping(value = "/{studyId}/publish", consumes = "application/json")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(operationId = "publishStudy", summary = "Publish my draft study",
            description = "Publishes DRAFT exactly once using the numeric study version. Requires a nonempty feed JSON object. "
                    + "Detailed document validation is deferred. Questionnaire readiness and question snapshots are temporarily bypassed, "
                    + "even when questionnaireEnabled is true. Returns the participant page link and refreshed study version.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Published in COLLECTING state."),
            @ApiResponse(responseCode = "400", description = "Invalid request; framework error bodies are not yet standardized."),
            @ApiResponse(responseCode = "401", description = "Authentication required."),
            @ApiResponse(responseCode = "403", description = "RESEARCHER role required."),
            @ApiResponse(responseCode = "404", description = "STUDY_NOT_FOUND."),
            @ApiResponse(responseCode = "409", description = "STUDY_NOT_PUBLISHABLE, STUDY_VERSION_CONFLICT or FEED_NOT_READY.")
    })
    public StudyResponse publish(@PathVariable UUID studyId, @Valid @RequestBody PublishStudyRequest request) {
        var study = service.publish(currentResearcher.getId(), studyId, request.version().longValue());
        return StudyResponse.from(study, links.forToken(study.getParticipationToken()));
    }
}
