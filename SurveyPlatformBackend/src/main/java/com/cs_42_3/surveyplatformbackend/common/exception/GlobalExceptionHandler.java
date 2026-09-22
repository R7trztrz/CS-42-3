package com.cs_42_3.surveyplatformbackend.common.exception;

import com.cs_42_3.surveyplatformbackend.researcher.service.CurrentPasswordIncorrectException;
import com.cs_42_3.surveyplatformbackend.researcher.service.DuplicateEmailException;
import com.cs_42_3.surveyplatformbackend.researcher.service.InvalidCredentialsException;
import com.cs_42_3.surveyplatformbackend.researcher.service.PasswordMismatchException;
import com.cs_42_3.surveyplatformbackend.security.ratelimit.RateLimitExceededException;
import com.cs_42_3.surveyplatformbackend.security.turnstile.HumanVerificationException;

import jakarta.validation.ConstraintViolationException;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyNotFoundException;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyNotEditableException;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyVersionConflictException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;


/**
 * Handles application exceptions and returns consistent API error responses.
 *
 * @author Jiale Chen
 * @author Simon Tian
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

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

    /*
     * TODO(global exception coverage): the framework exceptions listed below have no
     * handler yet, so they fall through to Spring Boot's default /error response and
     * return a body shape that differs from ErrorResponse. None of them is specific to
     * one module, so they belong here rather than in a module-level advice.
     *
     * - HttpMessageNotReadableException (400)
     *     Thrown while reading a @RequestBody. Triggered by POST /api/studies when the
     *     JSON is malformed, the body is missing, or a field carries the wrong JSON type.
     * - HttpMediaTypeNotSupportedException (415)
     *     Thrown when no converter matches the request Content-Type. Triggered by
     *     POST /api/studies with a non-JSON Content-Type.
     * - HttpRequestMethodNotSupportedException (405)
     *     Thrown by handler mapping when the path matches but the HTTP method does not.
     *     Triggered by e.g. DELETE /api/studies, which has no matching operation.
     * - MethodArgumentTypeMismatchException (400)
     *     Triggered by malformed study UUIDs or nonnumeric pagination parameters.
     * - DataIntegrityViolationException (500)
     *     Thrown when a database constraint fails, e.g. the studies.owner_id foreign key
     *     or a CHECK constraint, surfaced at transaction commit. Its message embeds the
     *     failing SQL statement and constraint name, so a handler must not put
     *     getMessage() into the response.
     * - DataAccessResourceFailureException and CannotCreateTransactionException (500)
     *     Thrown when a database connection cannot be obtained.
     * - InvalidDataAccessResourceUsageException (500)
     *     Thrown on SQL grammar failures such as a missing table or column.
     *
     * A catch-all Exception handler would cover the last three, provided it passes the
     * status of framework errors through instead of forcing 500.
     */

    @ExceptionHandler(DuplicateEmailException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateEmail(
            DuplicateEmailException exception) {

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(PasswordMismatchException.class)
    public ResponseEntity<ErrorResponse> handlePasswordMismatch(
            PasswordMismatchException exception) {

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleInvalidCredentials(
            InvalidCredentialsException exception) {

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse(exception.getMessage()));
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
                .body(new ErrorResponse(message));
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

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse("An internal server error occurred."));
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
                .body(new ErrorResponse("Access denied."));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatus(
            ResponseStatusException exception) {

        String message = exception.getReason() != null
                ? exception.getReason()
                : "Request failed";

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(new ErrorResponse(message));
    }

    @ExceptionHandler(HumanVerificationException.class)
    public ResponseEntity<ErrorResponse> handleHumanVerificationException(
            HumanVerificationException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(RateLimitExceededException.class)
    public ResponseEntity<ErrorResponse> handleRateLimitExceeded(
            RateLimitExceededException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.TOO_MANY_REQUESTS)
                .body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(CurrentPasswordIncorrectException.class)
    public ResponseEntity<ErrorResponse> handleCurrentPasswordIncorrect(
            CurrentPasswordIncorrectException exception
    ) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse(exception.getMessage()));
    }
}
