package com.cs_42_3.surveyplatformbackend.study.exception;

/**
 * Reports a nonempty feed document is required before participation.
 *
 * @author Simon Tian
 */
public class FeedNotReadyException extends RuntimeException {
    public FeedNotReadyException() {
        super("A nonempty feed document is required before participation.");
    }
}
