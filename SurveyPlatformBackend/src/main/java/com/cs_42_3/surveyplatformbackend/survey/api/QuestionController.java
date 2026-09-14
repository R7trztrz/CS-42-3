package com.cs_42_3.surveyplatformbackend.survey.api;

import com.cs_42_3.surveyplatformbackend.survey.api.dto.CreateQuestionRequest;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.QuestionResponse;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.QuestionSummaryResponse;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.SurveyErrorResponse;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.UpdateQuestionRequest;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.service.QuestionService;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * REST controller for researcher-owned question-bank endpoints.
 * <p>
 * Accepts and validates HTTP input while delegating FR32-FR35 business rules to the service layer.
 */
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESEARCHER')")
@SecurityRequirement(name = "bearerAuth")
@Tag(name = "Question Bank", description = "Researcher operations for reusable survey questions")
public class QuestionController {

    private final QuestionService questionService;

    @GetMapping
    @Operation(
            operationId = "listQuestions",
            summary = "Lists the current researcher's reusable questions"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Questions returned"),
            @ApiResponse(
                    responseCode = "400",
                    description = "A filter value is malformed",
                    content = @Content(schema = @Schema(implementation = SurveyErrorResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Researcher role required")
    })
    public ResponseEntity<List<QuestionSummaryResponse>> listQuestions(
            @Parameter(description = "Optional exact question type filter")
            @RequestParam(required = false) QuestionType type,
            @Parameter(description = "Optional case-insensitive question text fragment")
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(questionService.listQuestions(type, keyword));
    }

    @GetMapping("/{questionId}")
    @Operation(
            operationId = "getQuestion",
            summary = "Gets one question owned by the current researcher"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Question returned"),
            @ApiResponse(
                    responseCode = "400",
                    description = "The question identifier is malformed",
                    content = @Content(schema = @Schema(implementation = SurveyErrorResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Researcher role required"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Owned question not found",
                    content = @Content(schema = @Schema(implementation = SurveyErrorResponse.class))
            )
    })
    public ResponseEntity<QuestionResponse> getQuestion(
            @Parameter(description = "Question UUID")
            @PathVariable UUID questionId
    ) {
        return ResponseEntity.ok(questionService.getQuestion(questionId));
    }

    @PostMapping
    @Operation(
            operationId = "createQuestion",
            summary = "Creates a reusable question for the current researcher"
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Question created",
                    headers = @Header(
                            name = "Location",
                            description = "URI of the created question"
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Question data is invalid",
                    content = @Content(schema = @Schema(implementation = SurveyErrorResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Researcher role required")
    })
    public ResponseEntity<QuestionResponse> createQuestion(
            @Valid @RequestBody CreateQuestionRequest request
    ) {
        QuestionResponse response = questionService.createQuestion(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{questionId}")
                .buildAndExpand(response.id())
                .toUri();
        return ResponseEntity.created(location).body(response);
    }

    @PutMapping("/{questionId}")
    @Operation(
            operationId = "updateQuestion",
            summary = "Replaces an owned reusable question"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Question updated"),
            @ApiResponse(
                    responseCode = "400",
                    description = "Question data or identifier is invalid",
                    content = @Content(schema = @Schema(implementation = SurveyErrorResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Researcher role required"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Owned question not found",
                    content = @Content(schema = @Schema(implementation = SurveyErrorResponse.class))
            )
    })
    public ResponseEntity<QuestionResponse> updateQuestion(
            @Parameter(description = "Question UUID")
            @PathVariable UUID questionId,
            @Valid @RequestBody UpdateQuestionRequest request
    ) {
        return ResponseEntity.ok(questionService.updateQuestion(questionId, request));
    }

    @DeleteMapping("/{questionId}")
    @Operation(
            operationId = "deleteQuestion",
            summary = "Deletes an owned reusable question"
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Question deleted"),
            @ApiResponse(
                    responseCode = "400",
                    description = "The question identifier is malformed",
                    content = @Content(schema = @Schema(implementation = SurveyErrorResponse.class))
            ),
            @ApiResponse(responseCode = "401", description = "Authentication required"),
            @ApiResponse(responseCode = "403", description = "Researcher role required"),
            @ApiResponse(
                    responseCode = "404",
                    description = "Owned question not found",
                    content = @Content(schema = @Schema(implementation = SurveyErrorResponse.class))
            )
    })
    public ResponseEntity<Void> deleteQuestion(
            @Parameter(description = "Question UUID")
            @PathVariable UUID questionId
    ) {
        questionService.deleteQuestion(questionId);
        return ResponseEntity.noContent().build();
    }
}
