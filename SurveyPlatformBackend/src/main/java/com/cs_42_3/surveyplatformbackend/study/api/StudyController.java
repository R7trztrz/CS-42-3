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

/**
 * Exposes FR-11 creation and FR-12 owner-scoped study queries.
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

    /** Lists studies owned by the authenticated researcher; an empty page is successful. */
    @GetMapping
    @SecurityRequirement(name = "bearerAuth")
    @Operation(operationId = "listStudies", summary = "List my studies",
            description = "Implements FR-12. Ordered by creation time descending, then ID ascending.")
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
