package com.cs_42_3.surveyplatformbackend.feed.exception;

/**
 * Reports a stale feed edit.
 *
 * @author Simon Tian
 */
public class FeedVersionConflictException extends RuntimeException {
    public FeedVersionConflictException() {
        super("The feed has changed. Reload it before saving again.");
    }
}
