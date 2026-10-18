package com.cs_42_3.surveyplatformbackend.study.exception;

/**
 * Reports an attempt to close a study before it has entered collection.
 *
 * @author Simon Tian
 */
public class StudyNotClosableException extends RuntimeException {
    public StudyNotClosableException() {
        super("Only a collecting study can be closed.");
    }
}
