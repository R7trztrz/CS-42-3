package com.cs_42_3.surveyplatformbackend.calibration.api;

import com.cs_42_3.surveyplatformbackend.calibration.api.dto.CalibrationRecordResponse;
import com.cs_42_3.surveyplatformbackend.calibration.api.dto.SubmitCalibrationRequest;
import com.cs_42_3.surveyplatformbackend.calibration.api.security.ParticipantSessionAuthenticator;
import com.cs_42_3.surveyplatformbackend.calibration.service.CalibrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Exposes FR-49 calibration ingest for the participant client.
 *
 * <p>Participants are anonymous (UC-28), so this endpoint authenticates with a participant token
 * rather than a researcher JWT. The token scheme belongs to M5; see
 * {@link ParticipantSessionAuthenticator}. Until M5 supplies an implementation the endpoint
 * refuses every request, so it cannot be used to write data anonymously.
 *
 * <p>The endpoint must also be excluded from the researcher security chain when M1 configures
 * Spring Security, in the same way as the other participant endpoints (UC-04 acceptance
 * criterion 4).
 *
 * @author Shuo Gu
 */
@RestController
@RequestMapping("/api/participant/sessions/{sessionId}/calibration")
@RequiredArgsConstructor
@Tag(name = "Calibration", description = "Eye-tracking calibration ingest for participant sessions.")
public class CalibrationController {

    private final CalibrationService calibrationService;
    private final ObjectProvider<ParticipantSessionAuthenticator> authenticatorProvider;

    /**
     * Stores a finished calibration attempt.
     *
     * <p>The attempt is recorded whatever its outcome. A poor or abandoned calibration does not
     * stop eye tracking; its quality is retained as a data label instead (FR-49 acceptance
     * criteria 2 and 3).
     *
     * @param sessionId the session named in the path, which must match the token's session
     * @param participantToken the anonymous participant token
     * @param request the finished attempt
     * @return the stored attempt, with 201 when this call created it and 200 when it already existed
     */
    @PostMapping
    @Operation(
            operationId = "submitCalibrationAttempt",
            summary = "Submit a finished calibration attempt",
            description = "Implements FR-49. Records one calibration attempt of a participant session, "
                    + "including attempts that ended without a usable calibration and sessions where the "
                    + "camera was unavailable. Quality is stored as raw residuals; the server derives no "
                    + "graded score. The session is taken from the participant token, never from the body. "
                    + "Resubmitting the same attempt number returns the stored attempt unchanged."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Attempt stored.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CalibrationRecordResponse.class))),
            @ApiResponse(responseCode = "200", description = "Attempt was already stored; the stored attempt is returned unchanged.",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CalibrationRecordResponse.class))),
            @ApiResponse(responseCode = "400", description = "Malformed JSON, invalid fields, or an internally inconsistent attempt. Error response schema is not yet standardized.",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "The participant token is missing, invalid, or participant authentication is not configured.",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "The token belongs to a different session than the one named in the path.",
                    content = @Content)
    })
    public ResponseEntity<CalibrationRecordResponse> submitCalibrationAttempt(
            @Parameter(description = "The participant session the attempt belongs to.")
            @PathVariable UUID sessionId,
            @Parameter(description = "Anonymous participant token issued when the session was created.")
            @RequestHeader(name = "X-Participant-Token", required = false) String participantToken,
            @Valid @RequestBody SubmitCalibrationRequest request) {

        requireSession(sessionId, participantToken);

        CalibrationService.SubmissionResult result =
                calibrationService.submitAttempt(sessionId, request);

        return ResponseEntity
                .status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(CalibrationRecordResponse.from(result.record()));
    }

    // Keep the endpoint fail-closed until M5 supplies the participant token scheme.
    private void requireSession(UUID sessionId, String participantToken) {
        ParticipantSessionAuthenticator authenticator = authenticatorProvider.getIfAvailable();

        if (authenticator == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Participant session authentication is not configured.");
        }
        if (participantToken == null || participantToken.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Participant token required.");
        }

        UUID tokenSessionId = authenticator.resolveSessionId(participantToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Invalid participant token."));

        // Refuse to write one session's calibration under another session's identifier (NFR-23).
        if (!tokenSessionId.equals(sessionId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "The participant token does not belong to this session.");
        }
    }
}
