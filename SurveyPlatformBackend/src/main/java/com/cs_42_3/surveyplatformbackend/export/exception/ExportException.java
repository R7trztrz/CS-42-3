package com.cs_42_3.surveyplatformbackend.export.exception;

import com.cs_42_3.surveyplatformbackend.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Stable, safe export failures shared by task metadata and HTTP responses.
 *
 * @author Simon Tian
 */
@Getter
public class ExportException extends RuntimeException {
    private final ErrorCode code;
    private final HttpStatus status;

    public ExportException(ErrorCode code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static ExportException invalidData() {
        return new ExportException(ErrorCode.EXPORT_DATA_INVALID, HttpStatus.CONFLICT,
                "Stored questionnaire data is inconsistent. Contact the project administrator.");
    }

    public static ExportException invalidGazeData() {
        return new ExportException(ErrorCode.EXPORT_DATA_INVALID, HttpStatus.CONFLICT,
                "Stored eye-tracking data is inconsistent. Contact the project administrator.");
    }

    public static ExportException invalidBehaviorData() {
        return new ExportException(ErrorCode.EXPORT_DATA_INVALID, HttpStatus.CONFLICT,
                "Stored behavior data is inconsistent. Contact the project administrator.");
    }
}
