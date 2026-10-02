package com.cs_42_3.surveyplatformbackend.linkpreview.infrastructure;

import java.net.URI;

/**
 * Holds a bounded response and the final URL used to resolve relative metadata.
 *
 * @author Simon Tian
 */
public record RemoteDocument(URI uri, String contentType, byte[] bytes) {}
