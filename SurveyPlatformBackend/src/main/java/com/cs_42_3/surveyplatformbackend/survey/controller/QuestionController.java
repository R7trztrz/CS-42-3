package com.cs_42_3.surveyplatformbackend.survey.controller;

import com.cs_42_3.surveyplatformbackend.survey.dto.QuestionRequest;
import com.cs_42_3.surveyplatformbackend.survey.dto.QuestionResponse;
import com.cs_42_3.surveyplatformbackend.survey.entity.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * UC-21/22/23: question bank management. All endpoints operate on the authenticated
 * researcher's own questions only (enforced in QuestionService, not here).
 */
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@Tag(name = "Question Bank", description = "Account-level question bank management (UC-21/22/23)")
public class QuestionController {

    private final QuestionService questionService;

    @Operation(summary = "List/search the current researcher's question bank (UC-21)")
    @GetMapping
    public ResponseEntity<List<QuestionResponse>> listQuestions(
            @RequestParam(required = false) QuestionType type,
            @RequestParam(required = false) String keyword
    ) {
        return ResponseEntity.ok(questionService.listQuestions(type, keyword));
    }

    @Operation(summary = "Get a single question's detail (UC-21)")
    @GetMapping("/{id}")
    public ResponseEntity<QuestionResponse> getQuestion(@PathVariable Long id) {
        return ResponseEntity.ok(questionService.getQuestion(id));
    }

    @Operation(summary = "Add a question to the bank (UC-22)")
    @PostMapping
    public ResponseEntity<QuestionResponse> createQuestion(@Valid @RequestBody QuestionRequest request) {
        QuestionResponse created = questionService.createQuestion(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @Operation(summary = "Edit an existing bank question (UC-23)")
    @PutMapping("/{id}")
    public ResponseEntity<QuestionResponse> updateQuestion(
            @PathVariable Long id,
            @Valid @RequestBody QuestionRequest request
    ) {
        return ResponseEntity.ok(questionService.updateQuestion(id, request));
    }

    @Operation(summary = "Delete a bank question (UC-23)")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteQuestion(@PathVariable Long id) {
        questionService.deleteQuestion(id);
        return ResponseEntity.noContent().build();
    }
}
