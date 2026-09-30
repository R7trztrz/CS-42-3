package com.cs_42_3.surveyplatformbackend.study.exception;

/**
 * Reports this study has closed.
 *
 * @author Simon Tian
 */
public class StudyClosedException extends RuntimeException {
    public StudyClosedException() {
        super("This study has closed.");
    }
}
