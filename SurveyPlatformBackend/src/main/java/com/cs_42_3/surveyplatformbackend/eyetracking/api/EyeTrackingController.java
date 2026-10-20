package com.cs_42_3.surveyplatformbackend.eyetracking.api;

import com.cs_42_3.surveyplatformbackend.eyetracking.api.dto.CalibrationAttemptRequest;
import com.cs_42_3.surveyplatformbackend.eyetracking.api.dto.CalibrationAttemptResponse;
import com.cs_42_3.surveyplatformbackend.eyetracking.api.dto.GazeBatchRequest;
import com.cs_42_3.surveyplatformbackend.eyetracking.api.dto.GazeBatchResponse;
import com.cs_42_3.surveyplatformbackend.eyetracking.service.CalibrationRecordService;
import com.cs_42_3.surveyplatformbackend.eyetracking.service.GazeBatchService;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.ParticipantSessionResponse;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionPrincipal;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantSessionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/** M6 participant endpoints for FR-49 calibration records and FR-50 gaze batches. */
@RestController
@RequestMapping("/api/participant-session")
@RequiredArgsConstructor
@Tag(name = "Participant eye tracking")
public class EyeTrackingController {
    private final CalibrationRecordService calibrations;
    private final GazeBatchService gazeBatches;
    private final ParticipantSessionService sessions;

    @PutMapping("/calibration-attempts/{attemptId}")
    @Operation(
            operationId = "recordCalibrationAttempt",
            summary = "Store one finished calibration attempt",
            description = "Idempotent by (session, attemptId). Accepted for consented, in-progress "
                    + "eye-tracking sessions from CALIBRATION onwards, so a delayed delivery still "
                    + "lands. Never changes the session phase."
    )
    @SecurityRequirement(name = "participantSessionToken")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Attempt stored."),
            @ApiResponse(responseCode = "200", description = "An identical attempt was already stored."),
            @ApiResponse(responseCode = "400", description = "REQUEST_BODY_INVALID, REQUEST_VALIDATION_FAILED or ARGUMENT_TYPE_MISMATCH."),
            @ApiResponse(responseCode = "401", description = "PARTICIPANT_SESSION_UNAUTHORIZED."),
            @ApiResponse(responseCode = "404", description = "PARTICIPANT_SESSION_NOT_FOUND."),
            @ApiResponse(responseCode = "409", description = "PARTICIPANT_IDEMPOTENCY_CONFLICT, PARTICIPANT_SESSION_STATE_INVALID or PARTICIPANT_SESSION_TERMINATED."),
            @ApiResponse(responseCode = "410", description = "STUDY_CLOSED.")
    })
    public ResponseEntity<CalibrationAttemptResponse> recordCalibrationAttempt(
            @AuthenticationPrincipal ParticipantSessionPrincipal principal,
            @PathVariable UUID attemptId,
            @Valid @RequestBody CalibrationAttemptRequest request
    ) {
        var result = calibrations.record(principal, attemptId, request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .cacheControl(CacheControl.noStore())
                .body(result.response());
    }

    @PostMapping("/calibration-completion")
    @Operation(
            operationId = "completeParticipantCalibration",
            summary = "Finish calibration with the attempt the participant continued with",
            description = "Stores the attempt and moves the session from CALIBRATION to BROWSING in "
                    + "one transaction. Any outcome is accepted, including ABANDONED and "
                    + "UNAVAILABLE. Replaying the same attempt is a no-op."
    )
    @SecurityRequirement(name = "participantSessionToken")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Calibration completed or an identical completion replayed."),
            @ApiResponse(responseCode = "400", description = "REQUEST_BODY_INVALID or REQUEST_VALIDATION_FAILED."),
            @ApiResponse(responseCode = "401", description = "PARTICIPANT_SESSION_UNAUTHORIZED."),
            @ApiResponse(responseCode = "404", description = "PARTICIPANT_SESSION_NOT_FOUND."),
            @ApiResponse(responseCode = "409", description = "PARTICIPANT_IDEMPOTENCY_CONFLICT, PARTICIPANT_SESSION_STATE_INVALID or PARTICIPANT_SESSION_TERMINATED."),
            @ApiResponse(responseCode = "410", description = "STUDY_CLOSED.")
    })
    public ResponseEntity<ParticipantSessionResponse> completeCalibration(
            @AuthenticationPrincipal ParticipantSessionPrincipal principal,
            @Valid @RequestBody CalibrationAttemptRequest request
    ) {
        calibrations.complete(principal, request);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(sessions.getCurrent(principal));
    }

    @PostMapping("/gaze-batches")
    @Operation(
            operationId = "submitGazeBatch",
            summary = "Store one batch of browsing-phase gaze events",
            description = "Idempotent by (session, sequence). Allowed while the M5 collection policy "
                    + "permits GAZE for the session. The batch is stored whole or not at all."
    )
    @SecurityRequirement(name = "participantSessionToken")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Batch stored."),
            @ApiResponse(responseCode = "200", description = "An identical batch was already stored."),
            @ApiResponse(responseCode = "400", description = "REQUEST_BODY_INVALID or REQUEST_VALIDATION_FAILED."),
            @ApiResponse(responseCode = "401", description = "PARTICIPANT_SESSION_UNAUTHORIZED."),
            @ApiResponse(responseCode = "404", description = "PARTICIPANT_SESSION_NOT_FOUND."),
            @ApiResponse(responseCode = "409", description = "PARTICIPANT_IDEMPOTENCY_CONFLICT, PARTICIPANT_SESSION_STATE_INVALID or PARTICIPANT_SESSION_TERMINATED."),
            @ApiResponse(responseCode = "410", description = "STUDY_CLOSED.")
    })
    public ResponseEntity<GazeBatchResponse> submitGazeBatch(
            @AuthenticationPrincipal ParticipantSessionPrincipal principal,
            @Valid @RequestBody GazeBatchRequest request
    ) {
        var result = gazeBatches.accept(principal, request);
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .cacheControl(CacheControl.noStore())
                .body(result.response());
    }
}
