package com.cs_42_3.surveyplatformbackend.participation.service;

import com.cs_42_3.surveyplatformbackend.feed.domain.StudyFeed;
import com.cs_42_3.surveyplatformbackend.feed.repository.StudyFeedRepository;
import com.cs_42_3.surveyplatformbackend.participation.exception.ParticipationException;
import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.exception.FeedNotReadyException;
import com.cs_42_3.surveyplatformbackend.study.exception.ParticipationNotFoundException;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyClosedException;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.repository.QuestionnaireSnapshotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
@RequiredArgsConstructor
public class ParticipationReadinessService {
    private static final String PARTICIPATION_TOKEN_PATTERN = "[A-Za-z0-9_-]{43}";

    private final StudyRepository studies;
    private final StudyFeedRepository feeds;
    private final QuestionnaireSnapshotRepository snapshots;
    private final ObjectMapper objectMapper;

    public ReadyParticipation resolveForRead(String token) {
        validateToken(token);
        Study study = studies.findByParticipationToken(token)
                .orElseThrow(ParticipationNotFoundException::new);
        return validate(study);
    }

    /** Must be called inside the session-creation transaction. */
    public ReadyParticipation resolveForSessionCreation(String token) {
        validateToken(token);
        Study study = studies.findByParticipationTokenForParticipation(token)
                .orElseThrow(ParticipationNotFoundException::new);
        return validate(study);
    }

    private ReadyParticipation validate(Study study) {
        if (study.getStatus() == StudyStatus.CLOSED) {
            throw new StudyClosedException();
        }
        if (study.getStatus() != StudyStatus.COLLECTING) {
            throw new ParticipationNotFoundException();
        }
        StudyFeed feed = feeds.findById(study.getId()).orElseThrow(FeedNotReadyException::new);
        if (feed.getContent() == null) {
            throw new FeedNotReadyException();
        }
        var document = objectMapper.readTree(feed.getContent());
        if (document == null || !document.isObject() || document.isEmpty()) {
            throw new FeedNotReadyException();
        }
        if (study.isQuestionnaireEnabled() && !snapshots.existsByStudyId(study.getId())) {
            throw ParticipationException.questionnaireNotReady();
        }
        if (study.getConsentDocumentVersion() == null) {
            throw ParticipationException.invalidState();
        }
        return new ReadyParticipation(study, feed);
    }

    private void validateToken(String token) {
        if (token == null || !token.matches(PARTICIPATION_TOKEN_PATTERN)) {
            throw new ParticipationNotFoundException();
        }
    }

    public record ReadyParticipation(Study study, StudyFeed feed) {
    }
}
