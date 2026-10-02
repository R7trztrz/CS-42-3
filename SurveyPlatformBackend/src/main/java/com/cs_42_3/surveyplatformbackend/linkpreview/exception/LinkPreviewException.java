package com.cs_42_3.surveyplatformbackend.linkpreview.exception;

import com.cs_42_3.surveyplatformbackend.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Describes safe failures when retrieving remote preview data.
 *
 * @author Simon Tian
 */
@Getter
public class LinkPreviewException extends RuntimeException {
    private final ErrorCode code;
    private final HttpStatus status;

    private LinkPreviewException(ErrorCode code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static LinkPreviewException invalidUrl() {
        return new LinkPreviewException(ErrorCode.PREVIEW_URL_INVALID, HttpStatus.BAD_REQUEST,
                "Use a public HTTP or HTTPS URL on its standard port.");
    }

    public static LinkPreviewException fetchFailed() {
        return new LinkPreviewException(ErrorCode.PREVIEW_FETCH_FAILED, HttpStatus.BAD_GATEWAY,
                "The remote resource could not be retrieved within the preview limits.");
    }

    public static LinkPreviewException unsupported() {
        return new LinkPreviewException(ErrorCode.PREVIEW_CONTENT_UNSUPPORTED, HttpStatus.UNPROCESSABLE_ENTITY,
                "The URL must return an HTML webpage.");
    }
}
