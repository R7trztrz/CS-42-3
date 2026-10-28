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
    private final java.util.UUID failedSessionId;

    public ExportException(ErrorCode code, HttpStatus status, String message) {
        this(code, status, message, null);
    }

    private ExportException(ErrorCode code, HttpStatus status, String message, java.util.UUID session) {
        super(message);
        this.code = code;
        this.status = status;
        this.failedSessionId = session;
    }

    /** Adds only session identity to failures, never source answers or event payloads. */
    public static ExportException forSession(java.util.UUID session, Exception failure) {
        ExportException contextual = failure instanceof ExportException known
                ? new ExportException(known.getCode(), known.getStatus(), "Session " + session + ": " + known.getMessage(), session)
                : new ExportException(ErrorCode.EXPORT_GENERATION_FAILED, HttpStatus.INTERNAL_SERVER_ERROR,
                        "Export generation failed for session " + session + ". Create a new task to retry.", session);
        contextual.initCause(failure);
        return contextual;
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
