package com.cs_42_3.surveyplatformbackend.feed.repository;

import com.cs_42_3.surveyplatformbackend.feed.domain.FeedTemplate;
import org.springframework.data.repository.Repository;
import java.util.Optional;

/**
 * Read-only access to the system template catalog; mutations belong to migrations.
 *
 * @author Simon Tian
 */
public interface FeedTemplateRepository extends Repository<FeedTemplate, String> {
    Optional<FeedTemplate> findById(String code);
}
