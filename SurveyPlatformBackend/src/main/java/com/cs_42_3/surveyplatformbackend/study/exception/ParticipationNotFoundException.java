package com.cs_42_3.surveyplatformbackend.study.exception;

/**
 * Reports participation link not found.
 *
 * @author Simon Tian
 */
public class ParticipationNotFoundException extends RuntimeException {
    public ParticipationNotFoundException() {
        super("Participation link not found.");
    }
}
