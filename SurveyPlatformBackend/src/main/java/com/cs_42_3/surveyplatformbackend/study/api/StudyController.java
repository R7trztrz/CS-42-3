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
import com.cs_42_3.surveyplatformbackend.study.api.dto.StudyPageResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import io.swagger.v3.oas.annotations.Parameter;
import com.cs_42_3.surveyplatformbackend.study.api.dto.UpdateStudyRequest;
import org.springframework.web.bind.annotation.PatchMapping;

/**
 * Exposes FR-11 creation, FR-12 queries and FR-13 partial draft updates.
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

    /** Updates only supplied fields of an owned draft, using the expected edit version. */
    @PatchMapping(value = "/{studyId}", consumes = "application/json")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(operationId = "updateStudy", summary = "Update my draft study",
            description = "FR-13: supply version and at least one of title, description, eyeTrackingEnabled, "
                    + "questionnaireEnabled. Omitted fields remain unchanged; description:null clears it. "
                    + "Other nulls and unknown fields are rejected. Only DRAFT is editable. "
                    + "All changes are atomic. A no-op may retain its version and timestamp. "
                    + "Published snapshots, participation and questionnaire execution are separate features.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated details including the current version.",
                    content = @Content(schema = @Schema(implementation = StudyResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input. Framework error bodies are not yet standardized.", content = @Content),
            @ApiResponse(responseCode = "401", description = "Authentication required.", content = @Content),
            @ApiResponse(responseCode = "403", description = "RESEARCHER role required.", content = @Content),
            @ApiResponse(responseCode = "404", description = "STUDY_NOT_FOUND: missing or foreign-owned study.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "STUDY_NOT_EDITABLE or STUDY_VERSION_CONFLICT.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public StudyResponse updateStudy(@PathVariable UUID studyId,
                                    @Valid @RequestBody UpdateStudyRequest request) {
        return StudyResponse.from(studyService.updateStudy(
                currentResearcher.getId(), studyId, request.toUpdate()));
    }

    /** Lists studies owned by the authenticated researcher; an empty page is successful. */
    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(operationId = "listStudies", summary = "List my studies",
            description = "Implements FR-12. Ordered by study update time descending, then ID ascending, before pagination. "
                    + "Feed and questionnaire edits do not affect the study update time.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Owned studies, possibly empty.",
                    content = @Content(schema = @Schema(implementation = StudyPageResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid pagination. Framework error bodies are not yet standardized.", content = @Content),
            @ApiResponse(responseCode = "401", description = "Authentication required.", content = @Content),
            @ApiResponse(responseCode = "403", description = "RESEARCHER role required.", content = @Content)
    })
    public StudyPageResponse listStudies(
            @Parameter(description = "Zero-based page index.", schema = @Schema(minimum = "0"))
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Page size.", schema = @Schema(minimum = "1", maximum = "100"))
            @RequestParam(defaultValue = "20") int size) {
        return StudyPageResponse.from(studyService.listStudies(currentResearcher.getId(), page, size));
    }

    /** Returns basic details without revealing whether another researcher's study exists. */
    @GetMapping("/{studyId}")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(operationId = "getStudy", summary = "Get my study",
            description = "Implements FR-12 basic details. Participation links will be integrated with FR-14 publishing.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Owned study details.",
                    content = @Content(schema = @Schema(implementation = StudyResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid study UUID. Framework error bodies are not yet standardized.", content = @Content),
            @ApiResponse(responseCode = "401", description = "Authentication required.", content = @Content),
            @ApiResponse(responseCode = "403", description = "RESEARCHER role required.", content = @Content),
            @ApiResponse(responseCode = "404", description = "Study absent or not owned by the current researcher; code STUDY_NOT_FOUND.",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public StudyResponse getStudy(@PathVariable UUID studyId) {
        return StudyResponse.from(studyService.getStudy(currentResearcher.getId(), studyId));
    }

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
                    + "The request cannot assign ownership or status. A required templateCode initializes an independent feed "
                    + "in the same transaction. Placeholder templates have null content; no questionnaire is created."
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Draft study created.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = StudyResponse.class))),
            @ApiResponse(responseCode = "400", description = "Malformed JSON, invalid fields or unknown templateCode (FEED_TEMPLATE_NOT_FOUND). Framework error response schema is not yet standardized.",
                    content = @Content),
            @ApiResponse(responseCode = "401", description = "Authentication is missing or invalid, or the JWT subject is not a canonical UUID.",
                    content = @Content),
            @ApiResponse(responseCode = "403", description = "The authenticated identity does not have the RESEARCHER role.",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<StudyResponse> createStudy(
            @Valid @RequestBody CreateStudyRequest request) {

        UUID ownerId = currentResearcher.getId();
        Study study = studyService.createStudy(ownerId, request.title(), request.description(), request.templateCode());

        return ResponseEntity.status(HttpStatus.CREATED).body(StudyResponse.from(study));
    }
}
