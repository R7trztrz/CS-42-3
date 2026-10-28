package com.cs_42_3.surveyplatformbackend.behavior.service;

import com.cs_42_3.surveyplatformbackend.behavior.api.dto.*;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionPrincipal;

/**
 * Participant-only write contract for atomic behavior batch acceptance.
 *
 * @author Simon Tian
 */
public interface BehaviorBatchService {
    BatchResult accept(ParticipantSessionPrincipal principal, BehaviorBatchRequest request);

    /**
     * Distinguishes newly committed data from an identical replay without changing its acknowledgement.
     *
     * @author Simon Tian
     */
    record BatchResult(boolean created, BehaviorBatchResponse response) { }
}
