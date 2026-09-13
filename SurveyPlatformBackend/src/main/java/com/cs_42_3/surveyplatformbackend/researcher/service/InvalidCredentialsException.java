package com.cs_42_3.surveyplatformbackend.researcher.service;

/**
 * Thrown when researcher authentication fails because
 * the supplied email or password is invalid.
 *
 * @author Jiale Chen
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}