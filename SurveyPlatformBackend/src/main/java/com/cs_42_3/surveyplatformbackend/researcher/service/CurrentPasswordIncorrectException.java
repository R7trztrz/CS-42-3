package com.cs_42_3.surveyplatformbackend.researcher.service;

/**
 * Thrown when the current password provided for a password change is incorrect.
 *
 * @author Jiale Chen
 */
public class CurrentPasswordIncorrectException extends RuntimeException {

    public CurrentPasswordIncorrectException() {
        super("Current password is incorrect");
    }
}