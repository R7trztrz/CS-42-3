package com.cs_42_3.surveyplatformbackend.study.service.implementation;

import com.cs_42_3.surveyplatformbackend.feed.repository.StudyFeedRepository;
import com.cs_42_3.surveyplatformbackend.study.domain.*;
import com.cs_42_3.surveyplatformbackend.study.exception.*;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.cs_42_3.surveyplatformbackend.study.service.StudyPublicationService;
import jakarta.persistence.OptimisticLockException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;

/**
 * Serializes publication with feed writes and atomically freezes study configuration.
 *
 * @author Simon Tian
 */
@Service
@RequiredArgsConstructor
public class StudyPublicationServiceImpl implements StudyPublicationService {
    private final StudyRepository studies;
    private final StudyFeedRepository feeds;
    private final ObjectMapper mapper;
    private final SecureRandom random = new SecureRandom();

    @Override
    @Transactional
    @PreAuthorize("hasRole('RESEARCHER')")
    public Study publish(UUID ownerId, UUID studyId, long expectedVersion) {
        var study = studies.findOwnedByIdForUpdate(studyId, ownerId)
                .orElseThrow(StudyNotFoundException::new);
        if (study.getStatus() != StudyStatus.DRAFT) {
            throw new StudyNotPublishableException();
        }
        if (!Objects.equals(study.getLockVersion(), expectedVersion)) {
            throw new StudyVersionConflictException();
        }
        var feed = feeds.findById(studyId).orElseThrow(FeedNotReadyException::new);
        if (feed.getContent() == null) {
            throw new FeedNotReadyException();
        }
        var document = mapper.readTree(feed.getContent());
        // Only establish that a node-map document exists; detailed Craft.js validation is deferred.
        if (document == null || !document.isObject() || document.isEmpty()) {
            throw new FeedNotReadyException();
        }
        if (study.isQuestionnaireEnabled()) {
            // TODO: Validate questionnaire readiness and freeze question content after module integration.
            // Temporarily allow publication without checking or snapshotting the questionnaire.
        }
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        study.publish(Base64.getUrlEncoder().withoutPadding().encodeToString(bytes), Instant.now());
        try {
            studies.flush();
        } catch (OptimisticLockingFailureException | OptimisticLockException exception) {
            throw new StudyVersionConflictException(exception);
        }
        return study;
    }
}
