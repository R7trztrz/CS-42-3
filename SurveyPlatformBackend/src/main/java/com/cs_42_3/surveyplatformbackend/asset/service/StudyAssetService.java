package com.cs_42_3.surveyplatformbackend.asset.service;

import com.cs_42_3.surveyplatformbackend.asset.api.dto.AssetResponse;
import org.springframework.web.multipart.MultipartFile;
import java.util.UUID;

/**
 * Provides study-owned image uploads, imports and access-controlled reads.
 *
 * @author Simon Tian
 */
public interface StudyAssetService {
    /** Validates and stores a browser upload for an owned draft study. */
    AssetResponse upload(UUID ownerId, UUID studyId, MultipartFile file);

    /**
     * Validates and stores downloaded image bytes for an owned draft study.
     * The caller handles safe fetching and bounded downloads; this method performs no network access.
     * The detected format supplies a filename, without relying on a URL suffix or remote MIME header.
     */
    AssetResponse importImage(UUID ownerId, UUID studyId, byte[] bytes);

    /** Reads an image belonging to the researcher's study. */
    AssetContent readOwned(UUID ownerId, UUID studyId, UUID assetId);

    /** Reads a referenced image through an active study participation token. */
    AssetContent readParticipant(String token, UUID assetId);
}
