package com.cs_42_3.surveyplatformbackend.participation.api;

import com.cs_42_3.surveyplatformbackend.participation.api.dto.ConsentDecisionRequest;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.CreateParticipantSessionRequest;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.CreateParticipantSessionResponse;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.ParticipantSessionResponse;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionPrincipal;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Participant sessions")
public class ParticipantSessionController {
    private final ParticipantSessionService sessions;

    @PostMapping("/participation/{studyToken}/sessions")
    @Operation(operationId = "createParticipantSession", summary = "Create an anonymous participant session")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Session created; token is returned once."),
            @ApiResponse(responseCode = "404", description = "PARTICIPATION_NOT_FOUND."),
            @ApiResponse(responseCode = "409", description = "Feed or questionnaire is not ready."),
            @ApiResponse(responseCode = "410", description = "STUDY_CLOSED.")
    })
    public ResponseEntity<CreateParticipantSessionResponse> create(
            @PathVariable String studyToken,
            @Valid @RequestBody CreateParticipantSessionRequest request
    ) {
        return ResponseEntity.status(201)
                .cacheControl(CacheControl.noStore())
                .body(sessions.create(studyToken, request));
    }

    @GetMapping("/participant-session")
    @Operation(operationId = "getCurrentParticipantSession", summary = "Restore the authenticated session")
    public ResponseEntity<ParticipantSessionResponse> current(
            @AuthenticationPrincipal ParticipantSessionPrincipal principal
    ) {
        return noStore(sessions.getCurrent(principal));
    }

    @PutMapping("/participant-session/consent")
    @Operation(operationId = "decideParticipantConsent", summary = "Accept or decline informed consent")
    public ResponseEntity<ParticipantSessionResponse> consent(
            @AuthenticationPrincipal ParticipantSessionPrincipal principal,
            @Valid @RequestBody ConsentDecisionRequest request
    ) {
        return noStore(sessions.decideConsent(principal, request.accepted()));
    }

    @PostMapping("/participant-session/browsing-completion")
    @Operation(operationId = "completeParticipantBrowsing", summary = "Manually complete the browsing phase")
    public ResponseEntity<ParticipantSessionResponse> completeBrowsing(
            @AuthenticationPrincipal ParticipantSessionPrincipal principal
    ) {
        return noStore(sessions.completeBrowsing(principal));
    }

    private ResponseEntity<ParticipantSessionResponse> noStore(ParticipantSessionResponse body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
