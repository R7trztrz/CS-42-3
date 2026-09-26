package com.cs_42_3.surveyplatformbackend.study.exception;

/**
 * Reports only draft studies can be published.
 *
 * @author Simon Tian
 */
public class StudyNotPublishableException extends RuntimeException {
    public StudyNotPublishableException() {
        super("Only draft studies can be published.");
    }
}
