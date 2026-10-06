package com.cs_42_3.surveyplatformbackend.study.service;

import com.cs_42_3.surveyplatformbackend.study.api.dto.ParticipationResponse;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipationReadinessService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * Resolves public participation tokens and checks the current lifecycle on every read.
 *
 * @author Simon Tian
 */
@Service
@RequiredArgsConstructor
public class ParticipationService {
    private final ParticipationReadinessService readiness;
    private final ObjectMapper mapper;

    @Transactional(readOnly = true)
    public ParticipationResponse get(String token) {
        var ready = readiness.resolveForRead(token);
        var study = ready.study();
        var feed = ready.feed();
        return new ParticipationResponse(study.getTitle(), study.getDescription(),
                study.isEyeTrackingEnabled(), study.isQuestionnaireEnabled(), feed.getTheme(),
                mapper.readTree(feed.getContent()));
    }
}
