package com.cs_42_3.surveyplatformbackend.researcher.service;

/**
 * Thrown when registration is attempted with an email
 * that is already associated with an existing researcher account.
 *
 * @author Jiale Chen
 */
public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String message) {
        super(message);
    }
}