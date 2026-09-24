package com.cs_42_3.surveyplatformbackend.study.exception;

/**
 * Signals an outdated edit or a concurrent database update.
 *
 * @author Simon Tian
 */
public class StudyVersionConflictException extends RuntimeException {
    public StudyVersionConflictException() {
        this(null);
    }

    public StudyVersionConflictException(Throwable cause) {
        super("The study has changed. Reload it before saving.", cause);
    }
}
