package com.cs_42_3.surveyplatformbackend.feed.exception;

/**
 * Reports a missing study feed.
 *
 * @author Simon Tian
 */
public class FeedNotFoundException extends RuntimeException {
    public FeedNotFoundException() {
        super("The study feed does not exist.");
    }
}
