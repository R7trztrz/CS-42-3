package com.cs_42_3.surveyplatformbackend.survey.exception;

import com.cs_42_3.surveyplatformbackend.survey.api.QuestionController;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.SurveyErrorResponse;

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
 * Converts exceptions from {@link QuestionController} into a stable survey error response.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = QuestionController.class)
public class SurveyExceptionHandler {

    @ExceptionHandler(QuestionNotFoundException.class)
    public ResponseEntity<SurveyErrorResponse> handleQuestionNotFound(
            QuestionNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "QUESTION_NOT_FOUND",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidQuestionDataException.class)
    public ResponseEntity<SurveyErrorResponse> handleInvalidQuestionData(
            InvalidQuestionDataException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "QUESTION_VALIDATION_ERROR",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(InvalidResearcherIdentityException.class)
    public ResponseEntity<SurveyErrorResponse> handleInvalidResearcherIdentity(
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
    public ResponseEntity<SurveyErrorResponse> handleMethodArgumentNotValid(
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
    public ResponseEntity<SurveyErrorResponse> handleHttpMessageNotReadable(
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
    public ResponseEntity<SurveyErrorResponse> handleMethodArgumentTypeMismatch(
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

    private ResponseEntity<SurveyErrorResponse> buildResponse(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request
    ) {
        SurveyErrorResponse response = new SurveyErrorResponse(
                code,
                message,
                Instant.now(),
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(response);
    }
}
