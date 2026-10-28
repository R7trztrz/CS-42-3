package com.cs_42_3.surveyplatformbackend.behavior.exception;

import com.cs_42_3.surveyplatformbackend.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Safe behavior upload errors without echoing client event values or raw request bodies.
 *
 * @author Simon Tian
 */
@Getter
public class BehaviorBatchException extends RuntimeException {
    private final ErrorCode code;
    private final HttpStatus status;

    private BehaviorBatchException(ErrorCode code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static BehaviorBatchException invalid(String message) {
        return new BehaviorBatchException(ErrorCode.BEHAVIOR_BATCH_INVALID, HttpStatus.BAD_REQUEST, message);
    }

    public static BehaviorBatchException tooLarge() {
        return new BehaviorBatchException(ErrorCode.BEHAVIOR_BATCH_TOO_LARGE, HttpStatus.PAYLOAD_TOO_LARGE,
                "Behavior batch exceeds the configured upload limit. Send a smaller batch.");
    }
}
