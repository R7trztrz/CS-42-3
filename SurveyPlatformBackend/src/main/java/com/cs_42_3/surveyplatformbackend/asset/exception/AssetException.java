package com.cs_42_3.surveyplatformbackend.asset.exception;

import com.cs_42_3.surveyplatformbackend.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Carries a safe asset error response without exposing disk paths or image parser details.
 *
 * @author Simon Tian
 */
@Getter
public class AssetException extends RuntimeException {
    private final ErrorCode code;
    private final HttpStatus status;

    public AssetException(ErrorCode code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static AssetException invalid() {
        return new AssetException(ErrorCode.ASSET_INVALID_IMAGE, HttpStatus.BAD_REQUEST,
                "Upload a valid JPG, PNG or WebP image within the configured dimension limits.");
    }

    public static AssetException tooLarge() {
        return new AssetException(ErrorCode.ASSET_TOO_LARGE, HttpStatus.PAYLOAD_TOO_LARGE,
                "Each image must not exceed 5 MiB.");
    }

    public static AssetException notFound() {
        return new AssetException(ErrorCode.ASSET_NOT_FOUND, HttpStatus.NOT_FOUND, "Image not found.");
    }

    public static AssetException reference() {
        return new AssetException(ErrorCode.ASSET_REFERENCE_INVALID, HttpStatus.BAD_REQUEST,
                "Image references must use assetId and belong to this study.");
    }
}
