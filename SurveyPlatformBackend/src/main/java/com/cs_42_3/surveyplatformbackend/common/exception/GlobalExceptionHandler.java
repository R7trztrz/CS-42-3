package com.cs_42_3.surveyplatformbackend.common.exception;

import com.cs_42_3.surveyplatformbackend.feed.exception.FeedTemplateNotFoundException;
import com.cs_42_3.surveyplatformbackend.researcher.service.CurrentPasswordIncorrectException;
import com.cs_42_3.surveyplatformbackend.researcher.service.DuplicateEmailException;
import com.cs_42_3.surveyplatformbackend.researcher.service.InvalidCredentialsException;
import com.cs_42_3.surveyplatformbackend.researcher.service.PasswordMismatchException;
import com.cs_42_3.surveyplatformbackend.security.ratelimit.RateLimitExceededException;
import com.cs_42_3.surveyplatformbackend.security.turnstile.HumanVerificationException;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyNotEditableException;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyNotFoundException;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyVersionConflictException;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.SurveyErrorResponse;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.exception.QuestionnairePublicationException;
import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.CannotCreateTransactionException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

/**
 * Handles application exceptions and returns consistent API error responses.
 *
 * @author Jiale Chen
 * @author Simon Tian
 */
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(com.cs_42_3.surveyplatformbackend.participation.exception.ParticipationException.class)
    public ResponseEntity<ErrorResponse> handleParticipation(
            com.cs_42_3.surveyplatformbackend.participation.exception.ParticipationException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ErrorResponse(exception.getCode().code(), exception.getMessage()));
    }

    /** Returns stable preview failures without disclosing remote response bodies. */
    @ExceptionHandler(com.cs_42_3.surveyplatformbackend.linkpreview.exception.LinkPreviewException.class)
    public ResponseEntity<ErrorResponse> handlePreview(
            com.cs_42_3.surveyplatformbackend.linkpreview.exception.LinkPreviewException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ErrorResponse(exception.getCode().code(), exception.getMessage()));
    }

    /** Reports safe image errors consistently with other business failures. */
    @ExceptionHandler(com.cs_42_3.surveyplatformbackend.asset.exception.AssetException.class)
    public ResponseEntity<ErrorResponse> handleAsset(
            com.cs_42_3.surveyplatformbackend.asset.exception.AssetException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ErrorResponse(exception.getCode().code(), exception.getMessage()));
    }

    /** Covers multipart rejection before the upload controller is invoked. */
    @ExceptionHandler(org.springframework.web.multipart.MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleUploadSize() {
        return handleAsset(com.cs_42_3.surveyplatformbackend.asset.exception.AssetException.tooLarge());
    }

    private static final Logger log =
            LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Returns structured questionnaire readiness failures from the atomic publish command. */
    @ExceptionHandler(QuestionnairePublicationException.class)
    public ResponseEntity<SurveyErrorResponse>
            handleQuestionnairePublication(
                    QuestionnairePublicationException exception,
                    HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new SurveyErrorResponse(
                        "QUESTIONNAIRE_PUBLICATION_VALIDATION_ERROR",
                        exception.getMessage(),
                        Instant.now(),
                        request.getRequestURI(),
                        exception.getDetails()
                )
        );
    }

    /** Maps the STUDY_NOT_PUBLISHABLE business failure without persistence details. */
    @ExceptionHandler(com.cs_42_3.surveyplatformbackend.study.exception.StudyNotPublishableException.class)
    public ResponseEntity<ErrorResponse> handleStudyNotPublishable(
            com.cs_42_3.surveyplatformbackend.study.exception.StudyNotPublishableException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ErrorCode.STUDY_NOT_PUBLISHABLE.code(), exception.getMessage()));
    }

    /** Maps the FEED_NOT_READY business failure without persistence details. */
    @ExceptionHandler(com.cs_42_3.surveyplatformbackend.study.exception.FeedNotReadyException.class)
    public ResponseEntity<ErrorResponse> handleFeedNotReady(
            com.cs_42_3.surveyplatformbackend.study.exception.FeedNotReadyException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ErrorCode.FEED_NOT_READY.code(), exception.getMessage()));
    }

    /** Maps the PARTICIPATION_NOT_FOUND business failure without persistence details. */
    @ExceptionHandler(com.cs_42_3.surveyplatformbackend.study.exception.ParticipationNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleParticipationNotFound(
            com.cs_42_3.surveyplatformbackend.study.exception.ParticipationNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ErrorCode.PARTICIPATION_NOT_FOUND.code(), exception.getMessage()));
    }

    /** Maps the STUDY_CLOSED business failure without persistence details. */
    @ExceptionHandler(com.cs_42_3.surveyplatformbackend.study.exception.StudyClosedException.class)
    public ResponseEntity<ErrorResponse> handleStudyClosed(
            com.cs_42_3.surveyplatformbackend.study.exception.StudyClosedException exception) {
        return ResponseEntity.status(HttpStatus.GONE)
                .body(new ErrorResponse(ErrorCode.STUDY_CLOSED.code(), exception.getMessage()));
    }

    /** Reports missing feed rows, including legacy studies without an initialized feed. */
    @ExceptionHandler(com.cs_42_3.surveyplatformbackend.feed.exception.FeedNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFeedNotFound(
            com.cs_42_3.surveyplatformbackend.feed.exception.FeedNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ErrorCode.FEED_NOT_FOUND.code(), exception.getMessage()));
    }

    /** Asks the client to reload rather than overwrite a newer feed document. */
    @ExceptionHandler(com.cs_42_3.surveyplatformbackend.feed.exception.FeedVersionConflictException.class)
    public ResponseEntity<ErrorResponse> handleFeedVersionConflict(
            com.cs_42_3.surveyplatformbackend.feed.exception.FeedVersionConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ErrorCode.FEED_VERSION_CONFLICT.code(), exception.getMessage()));
    }

    /** Rejects an invalid template selection before creating any study records. */
    @ExceptionHandler(FeedTemplateNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleFeedTemplateNotFound(
            FeedTemplateNotFoundException exception) {
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ErrorCode.FEED_TEMPLATE_NOT_FOUND.code(), exception.getMessage()));
    }

    /** Maps the study lifecycle rule without exposing persistence details. */
    @ExceptionHandler(StudyNotEditableException.class)
    public ResponseEntity<ErrorResponse> handleStudyNotEditable(StudyNotEditableException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ErrorCode.STUDY_NOT_EDITABLE.code(), exception.getMessage()));
    }

    /** Covers both stale client versions and races detected at flush time. */
    @ExceptionHandler(StudyVersionConflictException.class)
    public ResponseEntity<ErrorResponse> handleStudyVersionConflict(StudyVersionConflictException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ErrorCode.STUDY_VERSION_CONFLICT.code(), exception.getMessage()));
    }

    /** Does not distinguish absent studies from studies belonging to another owner. */
    @ExceptionHandler(StudyNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleStudyNotFound(StudyNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ErrorCode.STUDY_NOT_FOUND.code(), exception.getMessage()));
    }

    /** Returns a safe 400 response when the request body cannot be parsed. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        ErrorCode.REQUEST_BODY_INVALID.code(),
                        "Request body is invalid."
                ));
    }

    /** Rejects request content types that the endpoint cannot consume. */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpMediaTypeNotSupported(
            HttpMediaTypeNotSupportedException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(new ErrorResponse(
                        ErrorCode.MEDIA_TYPE_NOT_SUPPORTED.code(),
                        "Content type is not supported."
                ));
    }

    /** Rejects HTTP methods that are not supported by the matched endpoint. */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleHttpRequestMethodNotSupported(
            HttpRequestMethodNotSupportedException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(new ErrorResponse(
                        ErrorCode.METHOD_NOT_SUPPORTED.code(),
                        "HTTP method is not supported for this endpoint."
                ));
    }

    /** Reports invalid path or query parameter types without exposing framework details. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        ErrorCode.ARGUMENT_TYPE_MISMATCH.code(),
                        "Request parameter has an invalid type."
                ));
    }

    /** Hides database constraint details while logging the full integrity violation server-side. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException exception
    ) {
        log.error("Database integrity violation", exception);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        ErrorCode.INTERNAL_SERVER_ERROR.code(),
                        "An internal server error occurred."
                ));
    }

    /** Handles database connectivity failures without exposing infrastructure details. */
    @ExceptionHandler({
            DataAccessResourceFailureException.class,
            CannotCreateTransactionException.class
    })
    public ResponseEntity<ErrorResponse> handleDatabaseUnavailable(
            RuntimeException exception
    ) {
        log.error("Database connection failure", exception);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        ErrorCode.INTERNAL_SERVER_ERROR.code(),
                        "An internal server error occurred."
                ));
    }

    /** Hides SQL/schema details while logging invalid database resource usage server-side. */
    @ExceptionHandler(InvalidDataAccessResourceUsageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDataAccessResourceUsage(
            InvalidDataAccessResourceUsageException exception
    ) {
        log.error("Invalid database resource usage", exception);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        ErrorCode.INTERNAL_SERVER_ERROR.code(),
                        "An internal server error occurred."
                ));
    }

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(
            DuplicateEmailException exception) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(
                        ErrorCode.AUTH_DUPLICATE_EMAIL.code(),
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ErrorResponse> handlePasswordMismatch(
            PasswordMismatchException exception) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        ErrorCode.AUTH_PASSWORD_MISMATCH.code(),
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException exception) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(
                        ErrorCode.AUTH_INVALID_CREDENTIALS.code(),
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException exception) {

        String message = exception.getBindingResult()
                .getFieldErrors()
                .stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Invalid request");

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        ErrorCode.REQUEST_VALIDATION_FAILED.code(),
                        message
                ));
    }

    /**
     * Handles entity constraint violations that survive DTO validation.
     *
     * <p>Client-editable fields are already rejected by {@code @Valid} on the request
     * DTO, so the constraints that remain here cover server-managed data. Reaching
     * this handler therefore means either the DTO and entity rules have diverged, or
     * server-managed data was not populated. Both are server faults, so the response
     * stays neutral.
     *
     * @param exception the failed entity constraints
     * @return the standard internal-error response
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(
            ConstraintViolationException exception) {

        log.error("Constraint violation", exception);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        ErrorCode.INTERNAL_SERVER_ERROR.code(),
                        "An internal server error occurred."
                ));
    }

    /**
     * Handles authorization denials raised by method security.
     *
     * <p>Only denials from annotations such as {@code @PreAuthorize} reach this
     * handler. Denials from request-level rules are raised inside the security
     * filter chain, so they never reach MVC and keep the framework's own response.
     *
     * <p>The message is fixed instead of taken from the exception: the framework
     * throws a generic {@code "Access Denied"} carrying no useful detail, and a
     * fixed message also keeps any future detail out of the response.
     *
     * @return the standard forbidden response
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied() {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ErrorResponse(
                        ErrorCode.AUTH_FORBIDDEN.code(),
                        "Access denied."
                ));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(
            ResponseStatusException exception) {

        String message = exception.getReason() != null
                ? exception.getReason()
                : "Request failed";

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(new ErrorResponse(
                        ErrorCode.REQUEST_FAILED.code(),
                        message
                ));
    }

    @ExceptionHandler(HumanVerificationException.class)
    public ResponseEntity<ErrorResponse> handleHumanVerificationException(
            HumanVerificationException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        ErrorCode.AUTH_HUMAN_VERIFICATION_FAILED.code(),
                        exception.getMessage()
                ));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(
            RateLimitExceededException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorResponse(
                ErrorCode.AUTH_RATE_LIMIT_EXCEEDED.code(),
                exception.getMessage()
        ));
    }

    @ExceptionHandler(CurrentPasswordIncorrectException.class)
    public ResponseEntity<ErrorResponse> handleCurrentPasswordIncorrect(
            CurrentPasswordIncorrectException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(
                        ErrorCode.AUTH_CURRENT_PASSWORD_INCORRECT.code(),
                        exception.getMessage()
                ));
    }

    /** Handles unexpected errors while preserving status codes from Spring framework errors. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpectedException(
            Exception exception
    ) {
        if (exception instanceof org.springframework.web.ErrorResponse frameworkError) {
            return ResponseEntity
                    .status(frameworkError.getStatusCode())
                    .body(new ErrorResponse(
                            ErrorCode.REQUEST_FAILED.code(),
                            "Request failed."
                    ));
        }

        log.error("Unexpected server error", exception);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(
                        ErrorCode.INTERNAL_SERVER_ERROR.code(),
                        "An internal server error occurred."
                ));
    }
}
