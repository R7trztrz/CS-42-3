package com.cs_42_3.surveyplatformbackend.researcher.service;

/**
 * Thrown when the password and confirmation password
 * provided during registration do not match.
 *
 * @author Jiale Chen
 */
public class PasswordMismatchException extends RuntimeException {

    public PasswordMismatchException(String message) {
        super(message);
    }
}