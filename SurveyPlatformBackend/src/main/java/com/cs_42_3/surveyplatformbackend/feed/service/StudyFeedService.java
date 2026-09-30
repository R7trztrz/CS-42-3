package com.cs_42_3.surveyplatformbackend.feed.service;

import com.cs_42_3.surveyplatformbackend.feed.domain.StudyFeed;
import java.util.UUID;

/**
 * Reads and replaces study-owned feed drafts.
 *
 * @author Simon Tian
 */
public interface StudyFeedService {
    StudyFeed getFeed(UUID ownerId, UUID studyId);
    StudyFeed saveFeed(UUID ownerId, UUID studyId, String content, long expectedVersion);
}
