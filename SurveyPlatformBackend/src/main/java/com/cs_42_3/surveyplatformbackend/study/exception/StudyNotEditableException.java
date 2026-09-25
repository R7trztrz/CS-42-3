package com.cs_42_3.surveyplatformbackend.study.exception;

/**
 * Indicates that the study has left the editable draft state.
 *
 * @author Simon Tian
 */
public class StudyNotEditableException extends RuntimeException {
    public StudyNotEditableException() {
        super("Only draft studies can be edited.");
    }
}
