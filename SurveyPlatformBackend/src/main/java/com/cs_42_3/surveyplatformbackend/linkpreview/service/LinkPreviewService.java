package com.cs_42_3.surveyplatformbackend.linkpreview.service;

import com.cs_42_3.surveyplatformbackend.linkpreview.api.dto.LinkPreviewResponse;
import java.util.UUID;

/**
 * Generates editable preview data for an owned draft study.
 * @author Simon Tian
 */
public interface LinkPreviewService {
    LinkPreviewResponse preview(UUID ownerId, UUID studyId, String url);
}

