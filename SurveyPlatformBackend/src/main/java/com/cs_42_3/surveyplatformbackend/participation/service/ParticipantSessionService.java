package com.cs_42_3.surveyplatformbackend.participation.service;

import com.cs_42_3.surveyplatformbackend.participation.api.dto.ConsentDocumentResponse;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.CreateParticipantSessionRequest;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.CreateParticipantSessionResponse;
import com.cs_42_3.surveyplatformbackend.participation.api.dto.ParticipantSessionResponse;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionPrincipal;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionTokenService;
import com.cs_42_3.surveyplatformbackend.participation.config.ParticipationProperties;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSession;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSessionPhase;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSessionStatus;
import com.cs_42_3.surveyplatformbackend.participation.exception.ParticipationException;
import com.cs_42_3.surveyplatformbackend.participation.repository.ParticipantSessionRepository;
import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyClosedException;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireNotFoundException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.domain.QuestionnaireSnapshotPayload;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.service.QuestionnaireSnapshotReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ParticipantSessionService implements ParticipantCalibrationLifecyclePort {
    private final ParticipantSessionRepository sessions;
    private final StudyRepository studies;
    private final ParticipationReadinessService readiness;
    private final ParticipantSessionTokenService tokens;
    private final ConsentDocumentProvider consentDocuments;
    private final QuestionnaireSnapshotReadService questionnaireSnapshots;
    private final ParticipantQuestionnaireService questionnaire;
    private final CollectionCompletionGate completionGate;
    private final ParticipationProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional
    public CreateParticipantSessionResponse create(
            String studyToken,
            CreateParticipantSessionRequest request
    ) {
        var ready = readiness.resolveForSessionCreation(studyToken);
        Study study = ready.study();
        String deviceInfo = serializeDeviceInfo(request.deviceInfo());
        var issuedToken = tokens.issue();
        Instant now = now();
        ParticipantSession session = ParticipantSession.create(
                study.getId(),
                UUID.randomUUID(),
                issuedToken.hash(),
                deviceInfo,
                now
        );
        session = sessions.saveAndFlush(session);
        ConsentDocumentResponse consent = ConsentDocumentResponse.from(
                consentDocuments.get(study.getConsentDocumentVersion())
        );
        return new CreateParticipantSessionResponse(
                session.getId(),
                issuedToken.rawToken(),
                session.getStatus(),
                session.getPhase(),
                study.getTitle(),
                study.getDescription(),
                study.isEyeTrackingEnabled(),
                study.isQuestionnaireEnabled(),
                consent,
                session.getEnteredAt()
        );
    }

    @Transactional(readOnly = true)
    public ParticipantSessionResponse getCurrent(ParticipantSessionPrincipal principal) {
        ParticipantSession session = sessions.findById(principal.sessionId())
                .orElseThrow(ParticipationException::sessionNotFound);
        assertPrincipalOwns(session, principal);
        Study study = studies.findById(principal.studyId())
                .orElseThrow(ParticipationException::sessionNotFound);
        if (session.getStatus() == ParticipantSessionStatus.IN_PROGRESS
                && study.getStatus() != StudyStatus.COLLECTING) {
            if (study.getStatus() == StudyStatus.CLOSED) {
                throw new StudyClosedException();
            }
            throw ParticipationException.invalidState();
        }
        return response(session, study);
    }

    @Transactional
    public ParticipantSessionResponse decideConsent(
            ParticipantSessionPrincipal principal,
            boolean accepted
    ) {
        Study study = lockCollectingStudy(principal.studyId());
        ParticipantSession session = lockOwnedSession(principal);
        session.decideConsent(accepted, study.isEyeTrackingEnabled(), now());
        return response(session, study);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void completeCalibration(UUID sessionId) {
        UUID studyId = sessions.findStudyIdById(sessionId)
                .orElseThrow(ParticipationException::sessionNotFound);
        Study study = lockCollectingStudy(studyId);
        ParticipantSession session = sessions.findByIdForUpdate(sessionId)
                .orElseThrow(ParticipationException::sessionNotFound);
        if (!session.getStudyId().equals(study.getId()) || !study.isEyeTrackingEnabled()) {
            throw ParticipationException.invalidState();
        }
        session.completeCalibration(now());
    }

    @Transactional
    public ParticipantSessionResponse completeBrowsing(ParticipantSessionPrincipal principal) {
        Study study = lockCollectingStudy(principal.studyId());
        ParticipantSession session = lockOwnedSession(principal);
        if (session.getBrowsingCompletedAt() == null) {
            assertActivePhase(session, ParticipantSessionPhase.BROWSING);
            completionGate.assertReadyForBrowsingCompletion(session.getId());
            if (!study.isQuestionnaireEnabled()) {
                completionGate.assertReadyForSessionCompletion(session.getId());
            }
        }
        UUID firstItemId = study.isQuestionnaireEnabled()
                ? firstQuestionItem(study.getId())
                : null;
        session.completeBrowsing(study.isQuestionnaireEnabled(), firstItemId, now());
        return response(session, study);
    }

    @Transactional
    public ParticipantSessionResponse completeQuestionnaire(ParticipantSessionPrincipal principal) {
        Study study = lockCollectingStudy(principal.studyId());
        ParticipantSession session = lockOwnedSession(principal);
        if (session.getStatus() == ParticipantSessionStatus.COMPLETED) {
            return response(session, study);
        }
        if (session.getStatus() == ParticipantSessionStatus.ABANDONED) {
            throw ParticipationException.terminated();
        }
        if (!study.isQuestionnaireEnabled()) {
            throw ParticipationException.questionnaireDisabled();
        }
        if (session.getCurrentQuestionItemId() != null
                || session.getQuestionnaireReadyAt() == null) {
            throw ParticipationException.questionnaireNotReady();
        }
        questionnaire.assertCompletedPath(session.getId(), study.getId());
        completionGate.assertReadyForSessionCompletion(session.getId());
        session.completeQuestionnaire(now());
        return response(session, study);
    }

    @Transactional
    public ParticipantSessionResponse abandon(ParticipantSessionPrincipal principal) {
        Study study = lockCollectingStudy(principal.studyId());
        ParticipantSession session = lockOwnedSession(principal);
        session.abandonByParticipant(now());
        return response(session, study);
    }

    private Study lockCollectingStudy(UUID studyId) {
        Study study = studies.findByIdForParticipation(studyId)
                .orElseThrow(ParticipationException::sessionNotFound);
        if (study.getStatus() == StudyStatus.CLOSED) {
            throw new StudyClosedException();
        }
        if (study.getStatus() != StudyStatus.COLLECTING) {
            throw ParticipationException.invalidState();
        }
        return study;
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    private void assertActivePhase(
            ParticipantSession session,
            ParticipantSessionPhase phase
    ) {
        if (session.getStatus() != ParticipantSessionStatus.IN_PROGRESS) {
            throw ParticipationException.terminated();
        }
        if (session.getPhase() != phase) {
            throw ParticipationException.invalidState();
        }
    }

    private ParticipantSession lockOwnedSession(ParticipantSessionPrincipal principal) {
        ParticipantSession session = sessions.findByIdForUpdate(principal.sessionId())
                .orElseThrow(ParticipationException::sessionNotFound);
        assertPrincipalOwns(session, principal);
        return session;
    }

    private void assertPrincipalOwns(
            ParticipantSession session,
            ParticipantSessionPrincipal principal
    ) {
        if (!session.getId().equals(principal.sessionId())
                || !session.getStudyId().equals(principal.studyId())) {
            throw ParticipationException.sessionNotFound();
        }
    }

    private UUID firstQuestionItem(UUID studyId) {
        try {
            return questionnaireSnapshots.getPayload(studyId).items().stream()
                    .min(Comparator.comparingInt(QuestionnaireSnapshotPayload.Item::position))
                    .map(QuestionnaireSnapshotPayload.Item::itemId)
                    .orElseThrow(ParticipationException::questionnaireNotReady);
        } catch (QuestionnaireNotFoundException exception) {
            throw ParticipationException.questionnaireNotReady();
        }
    }

    private String serializeDeviceInfo(Object deviceInfo) {
        if (deviceInfo == null) {
            return null;
        }
        String json = objectMapper.writeValueAsString(deviceInfo);
        if (json.getBytes(StandardCharsets.UTF_8).length > properties.maxDeviceInfoBytes()) {
            throw ParticipationException.deviceInfoInvalid();
        }
        return json;
    }

    private ParticipantSessionResponse response(ParticipantSession session, Study study) {
        ConsentDocumentResponse consent = ConsentDocumentResponse.from(
                consentDocuments.get(study.getConsentDocumentVersion())
        );
        return new ParticipantSessionResponse(
                session.getId(),
                session.getStatus(),
                session.getPhase(),
                session.getAbandonmentReason(),
                session.getCurrentQuestionItemId(),
                session.getQuestionnaireReadyAt() != null,
                study.getStatus() == StudyStatus.COLLECTING,
                study.getTitle(),
                study.getDescription(),
                study.isEyeTrackingEnabled(),
                study.isQuestionnaireEnabled(),
                consent,
                session.getEnteredAt(),
                session.getConsentedAt(),
                session.getCalibrationCompletedAt(),
                session.getBrowsingCompletedAt(),
                session.getCompletedAt(),
                session.getAbandonedAt(),
                session.getLastActivityAt()
        );
    }
}
