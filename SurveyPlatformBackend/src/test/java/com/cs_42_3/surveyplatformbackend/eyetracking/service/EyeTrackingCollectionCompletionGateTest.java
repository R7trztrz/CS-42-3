package com.cs_42_3.surveyplatformbackend.eyetracking.service;

import com.cs_42_3.surveyplatformbackend.common.exception.ErrorCode;
import com.cs_42_3.surveyplatformbackend.eyetracking.repository.CalibrationRecordRepository;
import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionTokenService;
import com.cs_42_3.surveyplatformbackend.participation.config.ParticipationProperties;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSession;
import com.cs_42_3.surveyplatformbackend.participation.exception.ParticipationException;
import com.cs_42_3.surveyplatformbackend.participation.repository.ParticipantSessionRepository;
import com.cs_42_3.surveyplatformbackend.participation.service.CollectionCompletionGate;
import com.cs_42_3.surveyplatformbackend.participation.service.ConsentDocumentProvider;
import com.cs_42_3.surveyplatformbackend.participation.service.NoOpCollectionCompletionGate;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantQuestionnaireService;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipantSessionService;
import com.cs_42_3.surveyplatformbackend.participation.service.ParticipationReadinessService;
import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.service.QuestionnaireSnapshotReadService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EyeTrackingCollectionCompletionGateTest {
    private final CalibrationRecordRepository calibrations = mock(CalibrationRecordRepository.class);
    private final ParticipantSessionRepository sessions = mock(ParticipantSessionRepository.class);
    private final StudyRepository studies = mock(StudyRepository.class);
    private final EyeTrackingCollectionCompletionGate gate =
            new EyeTrackingCollectionCompletionGate(calibrations, sessions, studies);

    @Test
    void sessionsOfStudiesWithoutEyeTrackingAreNeverBlocked() {
        UUID sessionId = session(false);

        assertThatCode(() -> gate.assertReadyForBrowsingCompletion(sessionId)).doesNotThrowAnyException();
        assertThatCode(() -> gate.assertReadyForSessionCompletion(sessionId)).doesNotThrowAnyException();
    }

    @Test
    void eyeTrackedSessionsPassOnceTheirCalibrationOutcomeIsPersisted() {
        UUID sessionId = session(true);
        when(calibrations.hasCompletion(sessionId)).thenReturn(true);

        assertThatCode(() -> gate.assertReadyForBrowsingCompletion(sessionId)).doesNotThrowAnyException();
        assertThatCode(() -> gate.assertReadyForSessionCompletion(sessionId)).doesNotThrowAnyException();
    }

    @Test
    void eyeTrackedSessionsWithoutAPersistedCalibrationOutcomeAreRejected() {
        UUID sessionId = session(true);
        when(calibrations.hasCompletion(sessionId)).thenReturn(false);

        assertThatThrownBy(() -> gate.assertReadyForBrowsingCompletion(sessionId))
                .isInstanceOfSatisfying(ParticipationException.class, exception ->
                        assertThat(exception.getCode()).isEqualTo(ErrorCode.PARTICIPANT_SESSION_STATE_INVALID));
        assertThatThrownBy(() -> gate.assertReadyForSessionCompletion(sessionId))
                .isInstanceOf(ParticipationException.class);
    }

    @Test
    void productionProfilesUseThisGateAndDevelopmentProfilesKeepTheNoOpAdapter() {
        for (String profile : new String[]{"prod", "default"}) {
            context(profile).run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(CollectionCompletionGate.class);
                assertThat(context.getBean(CollectionCompletionGate.class))
                        .isInstanceOf(EyeTrackingCollectionCompletionGate.class);
            });
        }
        for (String profile : new String[]{"dev", "test"}) {
            context(profile).run(context -> {
                assertThat(context).hasNotFailed();
                assertThat(context).hasSingleBean(CollectionCompletionGate.class);
                assertThat(context.getBean(CollectionCompletionGate.class))
                        .isInstanceOf(NoOpCollectionCompletionGate.class);
            });
        }
    }

    private UUID session(boolean eyeTracking) {
        UUID sessionId = UUID.randomUUID();
        UUID studyId = UUID.randomUUID();
        ParticipantSession session = mock(ParticipantSession.class);
        when(session.getStudyId()).thenReturn(studyId);
        Study study = mock(Study.class);
        when(study.isEyeTrackingEnabled()).thenReturn(eyeTracking);
        when(sessions.findById(sessionId)).thenReturn(Optional.of(session));
        when(studies.findById(studyId)).thenReturn(Optional.of(study));
        return sessionId;
    }

    private ApplicationContextRunner context(String profile) {
        return new ApplicationContextRunner()
                .withPropertyValues("spring.profiles.active=" + profile)
                .withUserConfiguration(
                        ParticipantSessionService.class,
                        NoOpCollectionCompletionGate.class,
                        EyeTrackingCollectionCompletionGate.class
                )
                .withBean(CalibrationRecordRepository.class, () -> mock(CalibrationRecordRepository.class))
                .withBean(ParticipantSessionRepository.class, () -> mock(ParticipantSessionRepository.class))
                .withBean(StudyRepository.class, () -> mock(StudyRepository.class))
                .withBean(ParticipationReadinessService.class,
                        () -> mock(ParticipationReadinessService.class))
                .withBean(ParticipantSessionTokenService.class,
                        () -> mock(ParticipantSessionTokenService.class))
                .withBean(ConsentDocumentProvider.class, () -> mock(ConsentDocumentProvider.class))
                .withBean(QuestionnaireSnapshotReadService.class,
                        () -> mock(QuestionnaireSnapshotReadService.class))
                .withBean(ParticipantQuestionnaireService.class,
                        () -> mock(ParticipantQuestionnaireService.class))
                .withBean(ParticipationProperties.class, () -> mock(ParticipationProperties.class))
                .withBean(ObjectMapper.class, () -> mock(ObjectMapper.class))
                .withBean(Clock.class, Clock::systemUTC);
    }
}
