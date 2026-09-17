package com.cs_42_3.surveyplatformbackend.common.exception;

/**
 * Standard error response returned by the API.
 *
 * @param error human-readable error message
 * @author Jiale Chen
 */
public record ErrorResponse(String error) {
}