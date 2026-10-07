package com.cs_42_3.surveyplatformbackend.eyetracking.exception;

import com.cs_42_3.surveyplatformbackend.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * A calibration record or gaze batch that is well-formed JSON but breaks the FR-49/FR-50
 * contract. Reported as {@code REQUEST_VALIDATION_FAILED}, the code the rest of the API uses
 * for semantically invalid input.
 */
@Getter
public class EyeTrackingRequestException extends RuntimeException {
    private final ErrorCode code;
    private final HttpStatus status;

    private EyeTrackingRequestException(String message) {
        super(message);
        this.code = ErrorCode.REQUEST_VALIDATION_FAILED;
        this.status = HttpStatus.BAD_REQUEST;
    }

    public static EyeTrackingRequestException invalid(String message) {
        return new EyeTrackingRequestException(message);
    }
}
