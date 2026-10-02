package com.cs_42_3.surveyplatformbackend.linkpreview.service.implementation;

import com.cs_42_3.surveyplatformbackend.linkpreview.service.LinkPreviewService;
import com.cs_42_3.surveyplatformbackend.linkpreview.api.dto.LinkPreviewResponse;
import com.cs_42_3.surveyplatformbackend.linkpreview.api.dto.LinkPreviewResponse.PreviewWarning;
import com.cs_42_3.surveyplatformbackend.linkpreview.infrastructure.*;
import com.cs_42_3.surveyplatformbackend.linkpreview.exception.LinkPreviewException;
import com.cs_42_3.surveyplatformbackend.asset.service.StudyAssetService;
import com.cs_42_3.surveyplatformbackend.asset.api.dto.AssetResponse;
import com.cs_42_3.surveyplatformbackend.asset.exception.AssetException;
import com.cs_42_3.surveyplatformbackend.common.exception.ErrorCode;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.exception.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.security.access.prepost.PreAuthorize;
import java.util.*;

/**
 * Builds previews outside database transactions and rechecks the study after network activity.
 * @author Simon Tian
 */
@Service
@RequiredArgsConstructor
public class LinkPreviewServiceImpl implements LinkPreviewService {
    private final StudyRepository studies;
    private final SafeHttpFetcher fetcher;
    private final MetadataParser parser;
    private final StudyAssetService assets;

    @Override
    @PreAuthorize("hasRole('RESEARCHER')")
    public LinkPreviewResponse preview(UUID ownerId, UUID studyId, String url) {
        requireDraft(ownerId, studyId);
        var page = fetcher.fetch(url, 2 * 1024 * 1024);
        var metadata = parser.parse(page);
        List<PreviewWarning> warnings = new ArrayList<>();
        AssetResponse image = null;
        if (metadata.imageUrl() != null) {
            try {
                var downloaded = fetcher.fetch(metadata.imageUrl(), 5 * 1024 * 1024);
                image = assets.importImage(ownerId, studyId, downloaded.bytes());
            } catch (LinkPreviewException exception) {
                warnings.add(imageWarning());
            } catch (AssetException exception) {
                // Operational failures remain errors; only invalid remote image data is optional.
                if (exception.getCode() != ErrorCode.ASSET_INVALID_IMAGE
                        && exception.getCode() != ErrorCode.ASSET_TOO_LARGE) throw exception;
                warnings.add(imageWarning());
            }
        }
        if (metadata.title() == null || metadata.description() == null) {
            warnings.add(new PreviewWarning(ErrorCode.PREVIEW_METADATA_INCOMPLETE.code(),
                    "Some metadata is missing. Complete the draft manually."));
        }
        requireDraft(ownerId, studyId);
        return new LinkPreviewResponse(url.trim(), metadata.title(), metadata.description(), image, List.copyOf(warnings));
    }

    private PreviewWarning imageWarning() {
        return new PreviewWarning(ErrorCode.PREVIEW_IMAGE_UNAVAILABLE.code(),
                "The image could not be imported. You can upload one manually.");
    }

    private void requireDraft(UUID ownerId, UUID studyId) {
        var study = studies.findByIdAndOwnerId(studyId, ownerId).orElseThrow(StudyNotFoundException::new);
        if (study.getStatus() != StudyStatus.DRAFT) throw new StudyNotEditableException();
    }
}
