package com.cs_42_3.surveyplatformbackend.feed.api.dto;

import com.cs_42_3.surveyplatformbackend.feed.domain.StudyFeed;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.UUID;

/**
 * Returns the stored document as JSON rather than an escaped JSON string.
 *
 * @author Simon Tian
 */
public record FeedResponse(UUID studyId,
                           String templateCode,
                           String theme,
                           JsonNode content,
                           Integer schemaVersion,
                           Long version,
                           Instant updatedAt) {
    /** Preserves null content for templates that have not been populated yet. */
    public static FeedResponse from(StudyFeed feed, ObjectMapper mapper) {
        return new FeedResponse(feed.getStudyId(), feed.getTemplateCode(), feed.getTheme(),
                feed.getContent() == null ? null : mapper.readTree(feed.getContent()),
                feed.getSchemaVersion(), feed.getLockVersion(), feed.getUpdatedAt());
    }
}
