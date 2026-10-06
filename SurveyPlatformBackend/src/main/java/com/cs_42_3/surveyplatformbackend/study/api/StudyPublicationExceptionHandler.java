package com.cs_42_3.surveyplatformbackend.study.api;

import com.cs_42_3.surveyplatformbackend.common.exception.ErrorCode;
import com.cs_42_3.surveyplatformbackend.study.exception.FeedNotReadyException;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyNotFoundException;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyNotPublishableException;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyVersionConflictException;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.SurveyErrorDetail;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.SurveyErrorResponse;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.exception.QuestionnairePublicationException;
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
import java.util.List;

/** Keeps every application-level publication failure on one stable error contract. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = StudyPublicationController.class)
public class StudyPublicationExceptionHandler {

    @ExceptionHandler(QuestionnairePublicationException.class)
    public ResponseEntity<SurveyErrorResponse> handleQuestionnairePublication(
            QuestionnairePublicationException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.CONFLICT,
                "QUESTIONNAIRE_PUBLICATION_VALIDATION_ERROR",
                exception.getMessage(),
                request,
                exception.getDetails()
        );
    }

    @ExceptionHandler(StudyNotPublishableException.class)
    public ResponseEntity<SurveyErrorResponse> handleStudyNotPublishable(
            StudyNotPublishableException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.CONFLICT,
                ErrorCode.STUDY_NOT_PUBLISHABLE.code(),
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(FeedNotReadyException.class)
    public ResponseEntity<SurveyErrorResponse> handleFeedNotReady(
            FeedNotReadyException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.CONFLICT,
                ErrorCode.FEED_NOT_READY.code(),
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(StudyVersionConflictException.class)
    public ResponseEntity<SurveyErrorResponse> handleStudyVersionConflict(
            StudyVersionConflictException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.CONFLICT,
                ErrorCode.STUDY_VERSION_CONFLICT.code(),
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(StudyNotFoundException.class)
    public ResponseEntity<SurveyErrorResponse> handleStudyNotFound(
            StudyNotFoundException exception,
            HttpServletRequest request
    ) {
        return response(
                HttpStatus.NOT_FOUND,
                ErrorCode.STUDY_NOT_FOUND.code(),
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
        return response(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<SurveyErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return response(
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
        return response(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_REQUEST",
                "Path or query parameter has an invalid value",
                request
        );
    }

    private ResponseEntity<SurveyErrorResponse> response(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request
    ) {
        return response(status, code, message, request, List.of());
    }

    private ResponseEntity<SurveyErrorResponse> response(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            List<SurveyErrorDetail> details
    ) {
        return ResponseEntity.status(status).body(new SurveyErrorResponse(
                code,
                message,
                Instant.now(),
                request.getRequestURI(),
                details
        ));
    }
}
