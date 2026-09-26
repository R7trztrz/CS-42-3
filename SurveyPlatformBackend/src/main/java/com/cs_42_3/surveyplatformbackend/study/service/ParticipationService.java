package com.cs_42_3.surveyplatformbackend.study.service;

import com.cs_42_3.surveyplatformbackend.study.api.dto.ParticipationResponse;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.exception.*;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.cs_42_3.surveyplatformbackend.feed.repository.StudyFeedRepository;
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
    private final StudyRepository studies;
    private final StudyFeedRepository feeds;
    private final ObjectMapper mapper;

    @Transactional(readOnly = true)
    public ParticipationResponse get(String token) {
        if (!token.matches("[A-Za-z0-9_-]{43}")) {
            throw new ParticipationNotFoundException();
        }
        var study = studies.findByParticipationToken(token).orElseThrow(ParticipationNotFoundException::new);
        if (study.getStatus() == StudyStatus.CLOSED) {
            throw new StudyClosedException();
        }
        if (study.getStatus() != StudyStatus.COLLECTING) {
            throw new ParticipationNotFoundException();
        }
        var feed = feeds.findById(study.getId()).orElseThrow(FeedNotReadyException::new);
        if (feed.getContent() == null) {
            throw new FeedNotReadyException();
        }
        return new ParticipationResponse(study.getTitle(), study.getDescription(),
                study.isEyeTrackingEnabled(), study.isQuestionnaireEnabled(), feed.getTheme(),
                mapper.readTree(feed.getContent()));
    }
}
