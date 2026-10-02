package com.cs_42_3.surveyplatformbackend.common.exception;

/**
 * Stable, machine-readable error codes returned in {@link ErrorResponse}.
 *
 * <p>Clients branch on these values instead of matching human-readable
 * messages, which may be reworded at any time. Treat every code as part of the
 * published API contract: add a new constant when a new failure category
 * appears, but never rename or remove an existing one.
 *
 * <p>Each constant carries its own wire value so that a Java-side rename cannot
 * silently change what clients receive. Keep the string identical to the
 * constant name unless there is a deliberate reason not to.
 *
 * @author Simon Tian
 */
public enum ErrorCode {
    PREVIEW_URL_INVALID("PREVIEW_URL_INVALID"),
    PREVIEW_FETCH_FAILED("PREVIEW_FETCH_FAILED"),
    PREVIEW_CONTENT_UNSUPPORTED("PREVIEW_CONTENT_UNSUPPORTED"),
    PREVIEW_IMAGE_UNAVAILABLE("PREVIEW_IMAGE_UNAVAILABLE"),
    PREVIEW_METADATA_INCOMPLETE("PREVIEW_METADATA_INCOMPLETE"),
    ASSET_INVALID_IMAGE("ASSET_INVALID_IMAGE"),
    ASSET_TOO_LARGE("ASSET_TOO_LARGE"),
    ASSET_NOT_FOUND("ASSET_NOT_FOUND"),
    ASSET_REFERENCE_INVALID("ASSET_REFERENCE_INVALID"),
    ASSET_STORAGE_FAILED("ASSET_STORAGE_FAILED"),
    STUDY_NOT_PUBLISHABLE("STUDY_NOT_PUBLISHABLE"),
    FEED_NOT_READY("FEED_NOT_READY"),
    PARTICIPATION_NOT_FOUND("PARTICIPATION_NOT_FOUND"),
    STUDY_CLOSED("STUDY_CLOSED"),
    FEED_NOT_FOUND("FEED_NOT_FOUND"),
    FEED_VERSION_CONFLICT("FEED_VERSION_CONFLICT"),
    FEED_TEMPLATE_NOT_FOUND("FEED_TEMPLATE_NOT_FOUND"),
    STUDY_NOT_FOUND("STUDY_NOT_FOUND"),
    STUDY_NOT_EDITABLE("STUDY_NOT_EDITABLE"),
    STUDY_VERSION_CONFLICT("STUDY_VERSION_CONFLICT"),

    AUTH_DUPLICATE_EMAIL("AUTH_DUPLICATE_EMAIL"),
    AUTH_PASSWORD_MISMATCH("AUTH_PASSWORD_MISMATCH"),
    AUTH_INVALID_CREDENTIALS("AUTH_INVALID_CREDENTIALS"),
    AUTH_CURRENT_PASSWORD_INCORRECT("AUTH_CURRENT_PASSWORD_INCORRECT"),
    AUTH_HUMAN_VERIFICATION_FAILED("AUTH_HUMAN_VERIFICATION_FAILED"),
    AUTH_RATE_LIMIT_EXCEEDED("AUTH_RATE_LIMIT_EXCEEDED"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR"),

    AUTH_UNAUTHORIZED("AUTH_UNAUTHORIZED"),
    AUTH_FORBIDDEN("AUTH_FORBIDDEN"),

    REQUEST_FAILED("REQUEST_FAILED"),
    REQUEST_VALIDATION_FAILED("REQUEST_VALIDATION_FAILED"),

    REQUEST_BODY_INVALID("REQUEST_BODY_INVALID"),
    MEDIA_TYPE_NOT_SUPPORTED("MEDIA_TYPE_NOT_SUPPORTED"),
    METHOD_NOT_SUPPORTED("METHOD_NOT_SUPPORTED"),
    ARGUMENT_TYPE_MISMATCH("ARGUMENT_TYPE_MISMATCH");

    private final String code;

    ErrorCode(String code) {
        this.code = code;
    }

    /**
     * Returns the wire value sent to clients.
     *
     * @return the stable error code string
     */
    public String code() {
        return code;
    }
}
