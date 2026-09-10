package com.cs_42_3.surveyplatformbackend.study.api;

import com.cs_42_3.surveyplatformbackend.study.api.dto.CreateStudyRequest;
import com.cs_42_3.surveyplatformbackend.study.api.dto.StudyResponse;
import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.service.StudyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

/**
 * Exposes FR-11 study creation using an authenticated researcher identity.
 * Resource Server signature and expiry validation must be configured by the security module.
 */
@RestController
@RequestMapping("/api/studies")
@RequiredArgsConstructor
@Tag(name = "Studies", description = "Research study management APIs.")
public class StudyController {

    private final StudyService studyService;

    /**
     * Creates a draft study. The JWT contract uses a UUID userId and role researcher.
     *
     * @param request validated client-editable study fields
     * @param authentication the identity established by Spring Security
     * @return the created study with HTTP 201
     */
    @PostMapping
    @Operation(
            operationId = "createStudy",
            summary = "Create a draft study",
            description = "Implements FR-11. Creates a study in DRAFT status using the supplied title and optional description. "
                    + "Ownership is derived from the authenticated JWT userId claim (UUID); role must be researcher. "
                    + "The request cannot assign ownership or status. This operation does not create a questionnaire or feed."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Draft study created.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StudyResponse.class))),
            @ApiResponse(responseCode = "400", description = "Malformed JSON or invalid request fields. Error response schema is not yet standardized.",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Authentication is missing or invalid, or the authenticated userId is not a UUID.",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "Access is denied, including when the authenticated role is not researcher.",
                    content = @Content)
    })
    public ResponseEntity<StudyResponse> createStudy(
            @Valid @RequestBody CreateStudyRequest request,
            @Parameter(hidden = true) Authentication authentication) {

        UUID ownerId = requireResearcherId(authentication);
        Study study = studyService.createStudy(ownerId, request.title(), request.description());

        return ResponseEntity.status(HttpStatus.CREATED).body(StudyResponse.from(study));
    }

    // Keep the endpoint fail-closed until the shared authentication adapter is available.
    private UUID requireResearcherId(Authentication authentication) {

        if (!(authentication instanceof JwtAuthenticationToken token) || !token.isAuthenticated()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required.");
        }

        Object userIdClaim = token.getToken().getClaims().get("userId");
        UUID ownerId;

        try {
            // check if the userId claim is a String
            if (!(userIdClaim instanceof String userId)) {
                throw new IllegalArgumentException();
            }

            // check if the userId is a valid UUID
            ownerId = UUID.fromString(userId);

            // check if the UUID is valid
            if (!ownerId.toString().equalsIgnoreCase(userId)) {
                throw new IllegalArgumentException();
            }

        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid authentication identity.");
        }

        if (!"researcher".equals(token.getToken().getClaims().get("role"))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Researcher access required.");
        }

        return ownerId;
    }
}
