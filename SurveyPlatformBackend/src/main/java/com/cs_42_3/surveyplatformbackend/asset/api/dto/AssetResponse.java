package com.cs_42_3.surveyplatformbackend.asset.api.dto;

import com.cs_42_3.surveyplatformbackend.asset.domain.StudyAsset;
import java.time.Instant;
import java.util.UUID;

/**
 * Returns a reusable asset identifier and an authenticated backend-relative content path.
 *
 * @author Simon Tian
 */
public record AssetResponse(UUID assetId,
                            String filename,
                            String contentType,
                            long sizeBytes,
                            int width,
                            int height,
                            Instant createdAt,
                            String contentUrl) {
    public static AssetResponse from(StudyAsset asset) {
        return new AssetResponse(asset.getId(), asset.getOriginalFilename(), asset.getContentType(),
                asset.getSizeBytes(), asset.getWidth(), asset.getHeight(), asset.getCreatedAt(),
                "/api/studies/" + asset.getStudyId() + "/assets/" + asset.getId() + "/content");
    }
}
