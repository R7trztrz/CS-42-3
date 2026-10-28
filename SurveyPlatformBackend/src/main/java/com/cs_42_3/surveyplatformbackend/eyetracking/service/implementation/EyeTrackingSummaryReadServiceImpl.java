package com.cs_42_3.surveyplatformbackend.eyetracking.service.implementation;

import com.cs_42_3.surveyplatformbackend.eyetracking.api.dto.EyeTrackingSessionSummaryResponse;
import com.cs_42_3.surveyplatformbackend.eyetracking.repository.EyeTrackingSummaryQueryRepository;
import com.cs_42_3.surveyplatformbackend.eyetracking.service.EyeTrackingSummaryReadService;
import com.cs_42_3.surveyplatformbackend.participation.exception.ParticipationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.UUID;

/**
 * Joins the researcher's snapshot transaction so JPA session and JDBC quality reads remain consistent.
 *
 * @author Simon Tian
 */
@Service
@RequiredArgsConstructor
@Transactional(propagation = Propagation.MANDATORY, readOnly = true)
public class EyeTrackingSummaryReadServiceImpl implements EyeTrackingSummaryReadService {
    private final EyeTrackingSummaryQueryRepository queries;

    @Override
    public EyeTrackingSessionSummaryResponse getSummary(UUID studyId, UUID sessionId) {
        return queries.find(studyId, sessionId).orElseThrow(ParticipationException::sessionNotFound);
    }
}
