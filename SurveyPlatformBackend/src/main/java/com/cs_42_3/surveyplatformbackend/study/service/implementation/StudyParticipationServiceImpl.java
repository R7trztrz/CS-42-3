package com.cs_42_3.surveyplatformbackend.study.service.implementation;

import com.cs_42_3.surveyplatformbackend.study.api.dto.*;
import com.cs_42_3.surveyplatformbackend.eyetracking.service.EyeTrackingSummaryReadService;
import com.cs_42_3.surveyplatformbackend.participation.exception.ParticipationException;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyNotFoundException;
import com.cs_42_3.surveyplatformbackend.study.repository.*;
import com.cs_42_3.surveyplatformbackend.study.service.StudyParticipationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.util.UUID;
import tools.jackson.databind.ObjectMapper;

/**
 * Authorizes FR-19/FR-20 and reads operational data from a repeatable-read snapshot without write locks.
 * M5 remains the sole authority for session state transitions.
 *
 * @author Simon Tian
 */
@Service
@RequiredArgsConstructor
public class StudyParticipationServiceImpl implements StudyParticipationService {
    private final StudyRepository studies;
    private final StudyParticipationQueryRepository queries;
    private final EyeTrackingSummaryReadService eyeTracking;
    private final ObjectMapper objectMapper;

    @Override
    @PreAuthorize("hasRole('RESEARCHER')")
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public StudyParticipantDetailResponse getParticipant(UUID ownerId, UUID studyId, UUID sessionId) {
        studies.findByIdAndOwnerId(studyId, ownerId).orElseThrow(StudyNotFoundException::new);
        var session = queries.findSessionDetails(studyId, sessionId)
                .orElseThrow(ParticipationException::sessionNotFound);
        // Explicit DTO mapping ignores unknown persisted keys and never serializes the source JSON.
        var device = session.deviceInfo() == null ? null
                : objectMapper.readValue(session.deviceInfo(), StudyParticipantDeviceResponse.class);
        return new StudyParticipantDetailResponse(studyId, session.sessionId(), session.anonymousParticipantId(),
                session.status(), session.phase(), session.abandonmentReason(), session.enteredAt(),
                session.consentedAt(), session.calibrationCompletedAt(), session.browsingCompletedAt(),
                session.completedAt(), session.abandonedAt(), session.lastActivityAt(), device,
                eyeTracking.getSummary(studyId, sessionId));
    }

    @Override
    @PreAuthorize("hasRole('RESEARCHER')")
    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public StudyParticipationResponse getParticipation(UUID ownerId, UUID studyId,
            int page, int size, String sortBy, String direction) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > Integer.MAX_VALUE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Page must be nonnegative, size must be between 1 and 100, and the page offset must fit a 32-bit integer.");
        }
        if (!"enteredAt".equals(sortBy) && !"status".equals(sortBy)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sort field must be enteredAt or status.");
        }
        if (!"asc".equals(direction) && !"desc".equals(direction)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sort direction must be asc or desc.");
        }
        // Missing and foreign-owned resources have the same response, including studies with no sessions.
        studies.findByIdAndOwnerId(studyId, ownerId).orElseThrow(StudyNotFoundException::new);
        var summary = queries.summarize(studyId);
        var sessions = queries.findSessions(studyId, page, size, sortBy, direction, summary.total());
        return new StudyParticipationResponse(summary, StudyParticipantPageResponse.from(sessions));
    }
}
