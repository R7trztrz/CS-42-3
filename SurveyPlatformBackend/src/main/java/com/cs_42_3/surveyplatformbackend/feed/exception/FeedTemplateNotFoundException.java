package com.cs_42_3.surveyplatformbackend.feed.exception;

/**
 * Rejects a template selection not present in the system catalog.
 *
 * @author Simon Tian
 */
public class FeedTemplateNotFoundException extends RuntimeException {
    public FeedTemplateNotFoundException() {
        super("The selected feed template does not exist.");
    }
}
