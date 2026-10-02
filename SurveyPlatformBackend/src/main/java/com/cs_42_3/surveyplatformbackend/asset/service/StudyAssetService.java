package com.cs_42_3.surveyplatformbackend.asset.service;

import com.cs_42_3.surveyplatformbackend.asset.api.dto.AssetResponse;
import com.cs_42_3.surveyplatformbackend.asset.domain.StudyAsset;
import com.cs_42_3.surveyplatformbackend.asset.exception.AssetException;
import com.cs_42_3.surveyplatformbackend.asset.repository.StudyAssetRepository;
import com.cs_42_3.surveyplatformbackend.asset.storage.AssetStorage;
import com.cs_42_3.surveyplatformbackend.feed.repository.StudyFeedRepository;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.exception.*;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

/**
 * Coordinates ownership, lifecycle, durable image storage and rollback compensation.
 *
 * @author Simon Tian
 */
@Service
@RequiredArgsConstructor
public class StudyAssetService {
    private static final Logger log = LoggerFactory.getLogger(StudyAssetService.class);
    private final StudyRepository studies;
    private final StudyAssetRepository assets;
    private final StudyFeedRepository feeds;
    private final AssetStorage storage;
    private final ImageValidator validator;
    private final AssetReferences references;
    private final PlatformTransactionManager transactions;

    @PreAuthorize("hasRole('RESEARCHER')")
    public AssetResponse upload(UUID ownerId, UUID studyId, MultipartFile file) {
        var initial = studies.findByIdAndOwnerId(studyId, ownerId).orElseThrow(StudyNotFoundException::new);
        if (initial.getStatus() != StudyStatus.DRAFT) throw new StudyNotEditableException();
        // Decode before taking the study row lock; publication may proceed while this happens.
        var image = validator.validate(file);
        return new TransactionTemplate(transactions).execute(status -> {
            var study = studies.findOwnedByIdForUpdate(studyId, ownerId).orElseThrow(StudyNotFoundException::new);
            if (study.getStatus() != StudyStatus.DRAFT) throw new StudyNotEditableException();
            UUID id = UUID.randomUUID();
            String key = id + "." + image.extension();
            storage.write(key, image.bytes());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCompletion(int completion) {
                    if (completion == STATUS_ROLLED_BACK) {
                        try {
                            storage.delete(key);
                        } catch (RuntimeException exception) {
                            log.error("Failed to clean up rolled-back image {}", id, exception);
                        }
                    }
                }
            });
            var asset = new StudyAsset(id, studyId, key, image.filename(), image.contentType(),
                    image.bytes().length, image.width(), image.height());
            return AssetResponse.from(assets.saveAndFlush(asset));
        });
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('RESEARCHER')")
    public AssetContent readOwned(UUID ownerId, UUID studyId, UUID assetId) {
        studies.findByIdAndOwnerId(studyId, ownerId).orElseThrow(StudyNotFoundException::new);
        return read(studyId, assetId);
    }

    @Transactional(readOnly = true)
    public AssetContent readParticipant(String token, UUID assetId) {
        if (!token.matches("[A-Za-z0-9_-]{43}")) throw new ParticipationNotFoundException();
        var study = studies.findByParticipationToken(token).orElseThrow(ParticipationNotFoundException::new);
        if (study.getStatus() == StudyStatus.CLOSED) throw new StudyClosedException();
        if (study.getStatus() != StudyStatus.COLLECTING) throw new ParticipationNotFoundException();
        var feed = feeds.findById(study.getId()).orElseThrow(AssetException::notFound);
        if (!references.ids(feed.getContent()).contains(assetId)) throw AssetException.notFound();
        return read(study.getId(), assetId);
    }

    private AssetContent read(UUID studyId, UUID assetId) {
        var asset = assets.findById(assetId).filter(a -> a.getStudyId().equals(studyId))
                .orElseThrow(AssetException::notFound);
        return new AssetContent(storage.read(asset.getStorageKey()), asset.getContentType());
    }
}
