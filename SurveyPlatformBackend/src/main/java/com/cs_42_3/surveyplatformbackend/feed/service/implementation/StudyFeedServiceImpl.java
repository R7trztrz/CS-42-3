package com.cs_42_3.surveyplatformbackend.feed.service.implementation;

import com.cs_42_3.surveyplatformbackend.feed.domain.StudyFeed;
import com.cs_42_3.surveyplatformbackend.feed.exception.FeedNotFoundException;
import com.cs_42_3.surveyplatformbackend.feed.exception.FeedVersionConflictException;
import com.cs_42_3.surveyplatformbackend.feed.repository.StudyFeedRepository;
import com.cs_42_3.surveyplatformbackend.feed.service.StudyFeedService;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyNotEditableException;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyNotFoundException;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;
import java.util.UUID;

/**
 * Enforces ownership, draft-only editing and independent feed concurrency control.
 *
 * @author Simon Tian
 */
@Service
@RequiredArgsConstructor
@PreAuthorize("hasRole('RESEARCHER')")
public class StudyFeedServiceImpl implements StudyFeedService {
    private final StudyRepository studies;
    private final StudyFeedRepository feeds;

    @Override
    @Transactional(readOnly = true)
    public StudyFeed getFeed(UUID ownerId, UUID studyId) {
        studies.findByIdAndOwnerId(studyId, ownerId).orElseThrow(StudyNotFoundException::new);
        return findFeed(studyId);
    }

    @Override
    @Transactional
    public StudyFeed saveFeed(UUID ownerId, UUID studyId, String content, long expectedVersion) {
        // Lock the study before checking its lifecycle so a status update cannot race this save.
        // Future publication must acquire this same study lock before reading the feed.
        var study = studies.findOwnedByIdForUpdate(studyId, ownerId)
                .orElseThrow(StudyNotFoundException::new);
        if (study.getStatus() != StudyStatus.DRAFT) {
            throw new StudyNotEditableException();
        }
        var feed = findFeed(studyId);
        if (!Objects.equals(feed.getLockVersion(), expectedVersion)) {
            throw new FeedVersionConflictException();
        }
        feed.replaceContent(content);
        try {
            feeds.flush();
        } catch (OptimisticLockingFailureException | OptimisticLockException exception) {
            throw new FeedVersionConflictException();
        }
        return feed;
    }

    private StudyFeed findFeed(UUID studyId) {
        return feeds.findById(studyId).orElseThrow(FeedNotFoundException::new);
    }
}
