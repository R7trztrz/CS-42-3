package com.cs_42_3.surveyplatformbackend.common.exception;

import com.cs_42_3.surveyplatformbackend.researcher.service.CurrentPasswordIncorrectException;
import com.cs_42_3.surveyplatformbackend.researcher.service.DuplicateEmailException;
import com.cs_42_3.surveyplatformbackend.researcher.service.InvalidCredentialsException;
import com.cs_42_3.surveyplatformbackend.researcher.service.PasswordMismatchException;
import com.cs_42_3.surveyplatformbackend.security.ratelimit.RateLimitExceededException;
import com.cs_42_3.surveyplatformbackend.security.turnstile.HumanVerificationException;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;


/**
 * Handles application exceptions and returns consistent API error responses.
 *
 * @author Jiale Chen
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

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
