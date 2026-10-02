package com.cs_42_3.surveyplatformbackend.linkpreview.api.dto;

import com.cs_42_3.surveyplatformbackend.asset.api.dto.AssetResponse;
import java.util.List;

/**
 * Returns editable text and an optional study-owned image without changing the feed.
 *
 * @author Simon Tian
 */
public record LinkPreviewResponse(String sourceUrl, String title, String description,
                                  AssetResponse image, List<PreviewWarning> warnings) {
    /**
     * Reports a recoverable extraction failure.
     * @author Simon Tian
     */
    public record PreviewWarning(String code, String message) {}
}
