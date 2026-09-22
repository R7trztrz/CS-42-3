package com.cs_42_3.surveyplatformbackend.questionnaire.exception;

import com.cs_42_3.surveyplatformbackend.questionnaire.api.QuestionnaireController;
import com.cs_42_3.surveyplatformbackend.questionnaire.api.dto.QuestionnaireErrorResponse;
import com.cs_42_3.surveyplatformbackend.survey.exception.InvalidResearcherIdentityException;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;

/**
 * Converts exceptions from {@link QuestionnaireController} into a stable error response.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = QuestionnaireController.class)
public class QuestionnaireExceptionHandler {

    @ExceptionHandler(QuestionnaireStudyNotFoundException.class)
    public ResponseEntity<QuestionnaireErrorResponse> handleStudyNotFound(
            QuestionnaireStudyNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, "STUDY_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidQuestionnaireDataException.class)
    public ResponseEntity<QuestionnaireErrorResponse> handleInvalidQuestionnaireData(
            InvalidQuestionnaireDataException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "QUESTIONNAIRE_VALIDATION_ERROR",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidResearcherIdentityException.class)
    public ResponseEntity<QuestionnaireErrorResponse> handleInvalidResearcherIdentity(
            InvalidResearcherIdentityException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "INVALID_RESEARCHER_IDENTITY",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<QuestionnaireErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Request validation failed");
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<QuestionnaireErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_REQUEST",
                "Request body is malformed or contains an unsupported value",
                request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<QuestionnaireErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_REQUEST",
                "Path or query parameter has an invalid value",
                request
        );
    }

    private ResponseEntity<QuestionnaireErrorResponse> buildResponse(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request
    ) {
        QuestionnaireErrorResponse response = new QuestionnaireErrorResponse(
                code,
                message,
                Instant.now(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(response);
    }
}
