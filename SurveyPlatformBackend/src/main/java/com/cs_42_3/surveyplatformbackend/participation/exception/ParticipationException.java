package com.cs_42_3.surveyplatformbackend.participation.exception;

import com.cs_42_3.surveyplatformbackend.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ParticipationException extends RuntimeException {
    private final ErrorCode code;
    private final HttpStatus status;

    private ParticipationException(ErrorCode code, HttpStatus status, String message) {
        super(message);
        this.code = code;
        this.status = status;
    }

    public static ParticipationException sessionNotFound() {
        return new ParticipationException(
                ErrorCode.PARTICIPANT_SESSION_NOT_FOUND,
                HttpStatus.NOT_FOUND,
                "Participant session was not found."
        );
    }

    public static ParticipationException invalidState() {
        return new ParticipationException(
                ErrorCode.PARTICIPANT_SESSION_STATE_INVALID,
                HttpStatus.CONFLICT,
                "Participant session is not in a valid state for this operation."
        );
    }

    public static ParticipationException terminated() {
        return new ParticipationException(
                ErrorCode.PARTICIPANT_SESSION_TERMINATED,
                HttpStatus.CONFLICT,
                "Participant session has already ended."
        );
    }

    public static ParticipationException questionnaireDisabled() {
        return new ParticipationException(
                ErrorCode.PARTICIPANT_QUESTIONNAIRE_DISABLED,
                HttpStatus.CONFLICT,
                "Questionnaire is not enabled for this study."
        );
    }

    public static ParticipationException questionnaireNotReady() {
        return new ParticipationException(
                ErrorCode.PARTICIPANT_QUESTIONNAIRE_NOT_READY,
                HttpStatus.CONFLICT,
                "Published questionnaire content is unavailable."
        );
    }

    public static ParticipationException deviceInfoInvalid() {
        return new ParticipationException(
                ErrorCode.REQUEST_VALIDATION_FAILED,
                HttpStatus.BAD_REQUEST,
                "Device information exceeds the allowed size."
        );
    }
}
