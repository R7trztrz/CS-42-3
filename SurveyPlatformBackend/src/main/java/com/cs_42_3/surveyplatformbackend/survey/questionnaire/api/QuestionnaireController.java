package com.cs_42_3.surveyplatformbackend.survey.questionnaire.api;

import com.cs_42_3.surveyplatformbackend.survey.api.dto.SurveyErrorResponse;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.QuestionnaireResponse;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.QuestionnaireSaveResult;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.SaveQuestionnaireRequest;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.service.QuestionnaireService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
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
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/** REST API for reading a study's current questionnaire content and replacing its draft. */
@RestController
@RequestMapping("/api/studies/{studyId}/questionnaire")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESEARCHER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Questionnaires", description = "Researcher operations for questionnaire drafts and publications")
public class QuestionnaireController {

    private final QuestionnaireService questionnaireService;

    @GetMapping
    @Operation(
            operationId = "getQuestionnaire",
            summary = "Gets a study's questionnaire",
            description = "DRAFT studies return current question-bank data and expose deleted entries as missing items. COLLECTING and CLOSED studies return only their immutable publication snapshot."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Questionnaire returned"),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Researcher role required"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Study or questionnaire not found",
                    content = @Content(schema = @Schema(implementation = SurveyErrorResponse.class))
            )
    })
    public ResponseEntity<QuestionnaireResponse> getQuestionnaire(
            @Parameter(description = "Study UUID") @PathVariable UUID studyId
    ) {
        return ResponseEntity.ok(questionnaireService.getQuestionnaire(studyId));
    }

    @PutMapping
    @Operation(
            operationId = "saveQuestionnaire",
            summary = "Replaces a questionnaire draft",
            description = "The item array defines the complete zero-based order. A first save uses a null expectedVersion; later changes use the returned version. Exact retries are idempotent and do not advance the version. Only DRAFT studies are editable. SINGLE_CHOICE rules use sourceOptionId, SCALE rules use sourceScaleValue, and targetPosition addresses the final item array."
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Questionnaire created",
                    headers = @Header(name = "Location", description = "Questionnaire URI")
            ),
            @ApiResponse(responseCode = "200", description = "Questionnaire updated or replayed"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Questionnaire data is invalid",
                    content = @Content(schema = @Schema(implementation = SurveyErrorResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Researcher role required"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Study not found",
                    content = @Content(schema = @Schema(implementation = SurveyErrorResponse.class))
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Questionnaire is locked or its version conflicts",
                    content = @Content(schema = @Schema(implementation = SurveyErrorResponse.class))
            )
    })
    public ResponseEntity<QuestionnaireResponse> saveQuestionnaire(
            @Parameter(description = "Study UUID") @PathVariable UUID studyId,
            @Valid @RequestBody SaveQuestionnaireRequest request
    ) {
        QuestionnaireSaveResult result = questionnaireService.saveQuestionnaire(studyId, request);
        if (!result.created()) {
            return ResponseEntity.ok(result.response());
        }
        URI location = ServletUriComponentsBuilder.fromCurrentRequest().build().toUri();
        return ResponseEntity.created(location).body(result.response());
    }
}
