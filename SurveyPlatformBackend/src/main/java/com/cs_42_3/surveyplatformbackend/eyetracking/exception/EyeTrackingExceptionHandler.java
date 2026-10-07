package com.cs_42_3.surveyplatformbackend.eyetracking.exception;

import com.cs_42_3.surveyplatformbackend.common.exception.ErrorResponse;
import com.cs_42_3.surveyplatformbackend.eyetracking.api.EyeTrackingController;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps M6 contract violations for the eye-tracking endpoints. Every other failure, including
 * M5 session-state errors raised by the participation ports, falls through to
 * {@code GlobalExceptionHandler} unchanged.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = EyeTrackingController.class)
public class EyeTrackingExceptionHandler {

    @ExceptionHandler(EyeTrackingRequestException.class)
    public ResponseEntity<ErrorResponse> handleInvalidRequest(EyeTrackingRequestException exception) {
        return ResponseEntity.status(exception.getStatus())
                .body(new ErrorResponse(exception.getCode().code(), exception.getMessage()));
    }
}
