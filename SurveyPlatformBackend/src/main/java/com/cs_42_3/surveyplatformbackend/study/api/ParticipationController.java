package com.cs_42_3.surveyplatformbackend.study.api;

import com.cs_42_3.surveyplatformbackend.study.api.dto.ParticipationResponse;
import com.cs_42_3.surveyplatformbackend.study.service.ParticipationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.*;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

/**
 * Provides the public entry configuration without creating a participant session.
 *
 * @author Simon Tian
 */
@RestController
@RequestMapping("/api/participation")
@RequiredArgsConstructor
@Tag(name = "Participation")
public class ParticipationController {
    private final ParticipationService service;

    @GetMapping("/{token}")
    @Operation(operationId = "getParticipation", summary = "Resolve a participation link",
            description = "No researcher JWT required. Reads COLLECTING studies only. "
                    + "Does not create a session or provide questionnaire execution. Responses must not be cached.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Participant-facing configuration."),
            @ApiResponse(responseCode = "404", description = "PARTICIPATION_NOT_FOUND."),
            @ApiResponse(responseCode = "410", description = "STUDY_CLOSED."),
            @ApiResponse(responseCode = "409", description = "FEED_NOT_READY: published content unavailable.")
    })
    public ResponseEntity<ParticipationResponse> get(@PathVariable String token) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.get(token));
    }
}
