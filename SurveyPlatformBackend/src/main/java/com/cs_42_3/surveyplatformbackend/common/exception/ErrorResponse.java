package com.cs_42_3.surveyplatformbackend.common.exception;

/**
 * Standard error response returned by the API.
 *
 * @param code stable business error code, or null for legacy handlers
 * @param error human-readable error message
 * @author Jiale Chen
 * @author Simon Tian
 */
public record ErrorResponse(String code, String error) {
    /** Preserves existing handler calls and the frontend's error message field. */
    public ErrorResponse(String error) {
        this(null, error);
    }
}
