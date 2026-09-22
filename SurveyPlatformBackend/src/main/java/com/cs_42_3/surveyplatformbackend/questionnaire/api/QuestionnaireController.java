package com.cs_42_3.surveyplatformbackend.questionnaire.api;

import com.cs_42_3.surveyplatformbackend.questionnaire.api.dto.QuestionnaireErrorResponse;
import com.cs_42_3.surveyplatformbackend.questionnaire.api.dto.QuestionnaireResponse;
import com.cs_42_3.surveyplatformbackend.questionnaire.api.dto.SaveQuestionnaireRequest;
import com.cs_42_3.surveyplatformbackend.questionnaire.service.QuestionnaireService;

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
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * REST controller for researcher-owned questionnaire editing.
 * <p>
 * Accepts and validates HTTP input while delegating FR36-FR39 business rules to the
 * service layer. Nested under a study because one questionnaire belongs to exactly
 * one study.
 */
@RestController
@RequestMapping("/api/studies/{studyId}/questionnaire")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESEARCHER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Questionnaire", description = "Researcher operations for a study's questionnaire structure")
public class QuestionnaireController {

    private final QuestionnaireService questionnaireService;

    @GetMapping
    @Operation(
            operationId = "getQuestionnaire",
            summary = "Gets the current researcher's questionnaire draft for one study"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Questionnaire returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Researcher role required"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Owned study not found",
                    content = @Content(schema = @Schema(implementation = QuestionnaireErrorResponse.class))
            )
    })
    public ResponseEntity<QuestionnaireResponse> getQuestionnaire(
            @Parameter(description = "Study UUID")
            @PathVariable UUID studyId
    ) {
        return ResponseEntity.ok(questionnaireService.getQuestionnaire(studyId));
    }

    @PutMapping
    @Operation(
            operationId = "saveQuestionnaire",
            summary = "Replaces the enabled items, order, and branch rules of one study's questionnaire",
            description = "Implements FR-36 (select from the question bank), FR-37 (reorder/remove), "
                    + "FR-38 (per-answer branch rules), and FR-39 (save as one unit). The request's item "
                    + "list order is the questionnaire's display and default-jump order; branch rules "
                    + "reference their jump target by index into that same list."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Questionnaire saved"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Item list, question reference, or branch rule is invalid",
                    content = @Content(schema = @Schema(implementation = QuestionnaireErrorResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Researcher role required"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Owned study not found",
                    content = @Content(schema = @Schema(implementation = QuestionnaireErrorResponse.class))
            )
    })
    public ResponseEntity<QuestionnaireResponse> saveQuestionnaire(
            @Parameter(description = "Study UUID")
            @PathVariable UUID studyId,
            @Valid @RequestBody SaveQuestionnaireRequest request
    ) {
        return ResponseEntity.ok(questionnaireService.saveQuestionnaire(studyId, request));
    }
}
