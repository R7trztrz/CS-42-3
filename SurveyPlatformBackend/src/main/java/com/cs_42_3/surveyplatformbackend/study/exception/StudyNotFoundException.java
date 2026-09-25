package com.cs_42_3.surveyplatformbackend.study.exception;

/**
 * Indicates that a study is unavailable to the requesting researcher.
 *
 * @author Simon Tian
 */
public class StudyNotFoundException extends RuntimeException {
    public StudyNotFoundException() {
        super("Study not found.");
    }
}
