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
    STUDY_NOT_PUBLISHABLE("STUDY_NOT_PUBLISHABLE"),
    FEED_NOT_READY("FEED_NOT_READY"),
    PARTICIPATION_NOT_FOUND("PARTICIPATION_NOT_FOUND"),
    STUDY_CLOSED("STUDY_CLOSED"),
    FEED_NOT_FOUND("FEED_NOT_FOUND"),
    FEED_VERSION_CONFLICT("FEED_VERSION_CONFLICT"),
    FEED_TEMPLATE_NOT_FOUND("FEED_TEMPLATE_NOT_FOUND"),
    STUDY_NOT_FOUND("STUDY_NOT_FOUND"),
    STUDY_NOT_EDITABLE("STUDY_NOT_EDITABLE"),
    STUDY_VERSION_CONFLICT("STUDY_VERSION_CONFLICT");

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
