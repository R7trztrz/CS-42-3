package com.cs_42_3.surveyplatformbackend.study.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.net.URI;

/**
 * Builds participant page links from deployment configuration, never request headers.
 *
 * @author Simon Tian
 */
@Component
public class ParticipationLinks {
    private final String baseUrl;

    public ParticipationLinks(@Value("${app.participant-base-url}") String baseUrl) {
        URI uri = URI.create(baseUrl);
        if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                || uri.getHost() == null || uri.getUserInfo() != null
                || uri.getQuery() != null || uri.getFragment() != null) {
            throw new IllegalArgumentException("Participant base URL must be an absolute HTTP(S) URL without credentials, query or fragment.");
        }
        this.baseUrl = baseUrl.replaceAll("/+$", "");
    }

    public String forToken(String token) {
        return token == null ? null : baseUrl + "/participate/" + token;
    }
}
