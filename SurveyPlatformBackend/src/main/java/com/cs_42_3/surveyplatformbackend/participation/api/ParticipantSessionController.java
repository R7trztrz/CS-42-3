package com.cs_42_3.surveyplatformbackend.participation.api;

import com.cs_42_3.surveyplatformbackend.participation.api.dto.ConsentDecisionRequest;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.CreateParticipantSessionRequest;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.CreateParticipantSessionResponse;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.ParticipantSessionResponse;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.ParticipantQuestionnaireStateResponse;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.SubmitQuestionnaireAnswerRequest;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionPrincipal;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantSessionService;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantQuestionnaireService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Participant sessions")
public class ParticipantSessionController {
    private final ParticipantSessionService sessions;
    private final ParticipantQuestionnaireService questionnaire;

    @PostMapping("/participation/{studyToken}/sessions")
    @Operation(operationId = "createParticipantSession", summary = "Create an anonymous participant session")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Session created; token is returned once."),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED."),
            @ApiResponse(responseCode = "404", description = "PARTICIPATION_NOT_FOUND."),
            @ApiResponse(responseCode = "409", description = "FEED_NOT_READY or PARTICIPANT_QUESTIONNAIRE_NOT_READY."),
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
    @SecurityRequirement(name = "participantSessionToken")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current session restored."),
            @ApiResponse(responseCode = "401", description = "PARTICIPANT_SESSION_UNAUTHORIZED."),
            @ApiResponse(responseCode = "404", description = "PARTICIPANT_SESSION_NOT_FOUND."),
            @ApiResponse(responseCode = "409", description = "PARTICIPANT_SESSION_STATE_INVALID."),
            @ApiResponse(responseCode = "410", description = "STUDY_CLOSED.")
    })
    public ResponseEntity<ParticipantSessionResponse> current(
            @AuthenticationPrincipal ParticipantSessionPrincipal principal
    ) {
        return noStore(sessions.getCurrent(principal));
    }

    @PutMapping("/participant-session/consent")
    @Operation(operationId = "decideParticipantConsent", summary = "Accept or decline informed consent")
    @SecurityRequirement(name = "participantSessionToken")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consent decision recorded or replayed."),
            @ApiResponse(responseCode = "400", description = "VALIDATION_FAILED."),
            @ApiResponse(responseCode = "401", description = "PARTICIPANT_SESSION_UNAUTHORIZED."),
            @ApiResponse(responseCode = "404", description = "PARTICIPANT_SESSION_NOT_FOUND."),
            @ApiResponse(responseCode = "409", description = "PARTICIPANT_SESSION_STATE_INVALID or PARTICIPANT_SESSION_TERMINATED."),
            @ApiResponse(responseCode = "410", description = "STUDY_CLOSED.")
    })
    public ResponseEntity<ParticipantSessionResponse> consent(
            @AuthenticationPrincipal ParticipantSessionPrincipal principal,
            @Valid @RequestBody ConsentDecisionRequest request
    ) {
        return noStore(sessions.decideConsent(principal, request.accepted()));
    }

    @PostMapping("/participant-session/browsing-completion")
    @Operation(operationId = "completeParticipantBrowsing", summary = "Manually complete the browsing phase")
    @SecurityRequirement(name = "participantSessionToken")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Browsing completed or an identical completion replayed."),
            @ApiResponse(responseCode = "401", description = "PARTICIPANT_SESSION_UNAUTHORIZED."),
            @ApiResponse(responseCode = "404", description = "PARTICIPANT_SESSION_NOT_FOUND."),
            @ApiResponse(responseCode = "409", description = "PARTICIPANT_SESSION_STATE_INVALID, PARTICIPANT_SESSION_TERMINATED, or collection completion rejected."),
            @ApiResponse(responseCode = "410", description = "STUDY_CLOSED.")
    })
    public ResponseEntity<ParticipantSessionResponse> completeBrowsing(
            @AuthenticationPrincipal ParticipantSessionPrincipal principal
    ) {
        return noStore(sessions.completeBrowsing(principal));
    }

    @GetMapping("/participant-session/questionnaire/current")
    @Operation(operationId = "getCurrentParticipantQuestion", summary = "Get the current published question")
    @SecurityRequirement(name = "participantSessionToken")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Current questionnaire state returned."),
            @ApiResponse(responseCode = "401", description = "PARTICIPANT_SESSION_UNAUTHORIZED."),
            @ApiResponse(responseCode = "404", description = "PARTICIPANT_SESSION_NOT_FOUND."),
            @ApiResponse(responseCode = "409", description = "PARTICIPANT_QUESTIONNAIRE_DISABLED, PARTICIPANT_QUESTIONNAIRE_NOT_READY, PARTICIPANT_SESSION_STATE_INVALID, or PARTICIPANT_SESSION_TERMINATED."),
            @ApiResponse(responseCode = "410", description = "STUDY_CLOSED.")
    })
    public ResponseEntity<ParticipantQuestionnaireStateResponse> currentQuestion(
            @AuthenticationPrincipal ParticipantSessionPrincipal principal
    ) {
        return noStore(questionnaire.current(principal));
    }

    @PutMapping("/participant-session/questionnaire/answers/{itemId}")
    @Operation(operationId = "submitParticipantAnswer", summary = "Submit the current questionnaire item")
    @SecurityRequirement(name = "participantSessionToken")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Answer saved or an identical request replayed."),
            @ApiResponse(responseCode = "400", description = "PARTICIPANT_ANSWER_INVALID."),
            @ApiResponse(responseCode = "401", description = "PARTICIPANT_SESSION_UNAUTHORIZED."),
            @ApiResponse(responseCode = "404", description = "PARTICIPANT_SESSION_NOT_FOUND."),
            @ApiResponse(responseCode = "409", description = "PARTICIPANT_IDEMPOTENCY_CONFLICT, PARTICIPANT_QUESTION_NOT_CURRENT, PARTICIPANT_QUESTIONNAIRE_DISABLED, PARTICIPANT_SESSION_STATE_INVALID, or PARTICIPANT_SESSION_TERMINATED."),
            @ApiResponse(responseCode = "410", description = "STUDY_CLOSED.")
    })
    public ResponseEntity<ParticipantQuestionnaireStateResponse> answer(
            @AuthenticationPrincipal ParticipantSessionPrincipal principal,
            @PathVariable java.util.UUID itemId,
            @RequestHeader("Idempotency-Key") java.util.UUID idempotencyKey,
            @RequestBody SubmitQuestionnaireAnswerRequest request
    ) {
        return noStore(questionnaire.answer(principal, itemId, idempotencyKey, request));
    }

    @PostMapping("/participant-session/questionnaire/submission")
    @Operation(operationId = "submitParticipantQuestionnaire", summary = "Complete a questionnaire whose path reached END")
    @SecurityRequirement(name = "participantSessionToken")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Questionnaire submitted or an identical submission replayed."),
            @ApiResponse(responseCode = "401", description = "PARTICIPANT_SESSION_UNAUTHORIZED."),
            @ApiResponse(responseCode = "404", description = "PARTICIPANT_SESSION_NOT_FOUND."),
            @ApiResponse(responseCode = "409", description = "PARTICIPANT_QUESTIONNAIRE_DISABLED, PARTICIPANT_QUESTIONNAIRE_NOT_READY, PARTICIPANT_SESSION_STATE_INVALID, PARTICIPANT_SESSION_TERMINATED, or collection completion rejected."),
            @ApiResponse(responseCode = "410", description = "STUDY_CLOSED.")
    })
    public ResponseEntity<ParticipantSessionResponse> submitQuestionnaire(
            @AuthenticationPrincipal ParticipantSessionPrincipal principal
    ) {
        return noStore(sessions.completeQuestionnaire(principal));
    }

    @PostMapping("/participant-session/abandonment")
    @Operation(operationId = "abandonParticipantSession", summary = "Explicitly leave the study")
    @SecurityRequirement(name = "participantSessionToken")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Session abandoned or an identical abandonment replayed."),
            @ApiResponse(responseCode = "401", description = "PARTICIPANT_SESSION_UNAUTHORIZED."),
            @ApiResponse(responseCode = "404", description = "PARTICIPANT_SESSION_NOT_FOUND."),
            @ApiResponse(responseCode = "409", description = "PARTICIPANT_SESSION_STATE_INVALID or PARTICIPANT_SESSION_TERMINATED."),
            @ApiResponse(responseCode = "410", description = "STUDY_CLOSED.")
    })
    public ResponseEntity<ParticipantSessionResponse> abandon(
            @AuthenticationPrincipal ParticipantSessionPrincipal principal
    ) {
        return noStore(sessions.abandon(principal));
    }

    private <T> ResponseEntity<T> noStore(T body) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(body);
    }
}
