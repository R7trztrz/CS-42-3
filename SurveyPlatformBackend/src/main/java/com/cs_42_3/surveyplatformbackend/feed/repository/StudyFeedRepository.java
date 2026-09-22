package com.cs_42_3.surveyplatformbackend.feed.repository;

import com.cs_42_3.surveyplatformbackend.feed.domain.StudyFeed;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

/**
 * Stores independent study feed documents.
 *
 * @author Simon Tian
 */
public interface StudyFeedRepository extends JpaRepository<StudyFeed, UUID> {
}
