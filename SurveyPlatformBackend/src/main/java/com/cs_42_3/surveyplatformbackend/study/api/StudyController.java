package com.cs_42_3.surveyplatformbackend.study.api;

import com.cs_42_3.surveyplatformbackend.common.exception.ErrorResponse;
import com.cs_42_3.surveyplatformbackend.security.CurrentResearcher;
import com.cs_42_3.surveyplatformbackend.study.api.dto.CreateStudyRequest;
import com.cs_42_3.surveyplatformbackend.study.api.dto.StudyResponse;
import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.service.StudyService;
import io.swagger.v3.oas.annotations.Operation;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Exposes FR-11 study creation using an authenticated researcher identity.
 * Delegates business authorization and creation to the study service.
 *
 * @author Simon Tian
 */
@RestController
@RequestMapping("/api/studies")
@RequiredArgsConstructor
@Tag(name = "Studies", description = "Research study management APIs.")
public class StudyController {

    private final StudyService studyService;
    private final CurrentResearcher currentResearcher;

    /**
     * Creates a draft study using the UUID in the authenticated JWT subject.
     *
     * @param request validated client-editable study fields
     * @return the created study with HTTP 201
     */
    @PostMapping
    @Operation(
            operationId = "createStudy",
            summary = "Create a draft study",
            description = "Implements FR-11. Creates a study in DRAFT status using the supplied title and optional description. "
                    + "Ownership is derived from the authenticated JWT sub claim (UUID); the RESEARCHER role is required. "
                    + "The request cannot assign ownership or status. This operation does not create a questionnaire or feed."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Draft study created.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StudyResponse.class))),
            @ApiResponse(responseCode = "400", description = "Malformed JSON or invalid request fields. Error response schema is not yet standardized.",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Authentication is missing or invalid, or the JWT subject is not a canonical UUID.",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated identity does not have the RESEARCHER role.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<StudyResponse> createStudy(
            @Valid @RequestBody CreateStudyRequest request) {

        UUID ownerId = currentResearcher.getId();
        Study study = studyService.createStudy(ownerId, request.title(), request.description());

        return ResponseEntity.status(HttpStatus.CREATED).body(StudyResponse.from(study));
    }
}
