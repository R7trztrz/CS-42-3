package com.cs_42_3.surveyplatformbackend.eyetracking.service;

import com.cs_42_3.surveyplatformbackend.eyetracking.api.dto.EyeTrackingSessionSummaryResponse;
import java.util.UUID;

/**
 * Supplies final M6 calibration metadata to an already-authorized researcher query.
 * The caller must authorize the study and session in the same read transaction.
 *
 * @author Simon Tian
 */
public interface EyeTrackingSummaryReadService {
    EyeTrackingSessionSummaryResponse getSummary(UUID studyId, UUID sessionId);
}
