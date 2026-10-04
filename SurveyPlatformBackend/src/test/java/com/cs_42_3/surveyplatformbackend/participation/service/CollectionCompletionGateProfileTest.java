package com.cs_42_3.surveyplatformbackend.participation.service;

import com.cs_42_3.surveyplatformbackend.participation.auth.ParticipantSessionTokenService;
import com.cs_42_3.surveyplatformbackend.participation.config.ParticipationProperties;
import com.cs_42_3.surveyplatformbackend.participation.repository.ParticipantSessionRepository;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.service.QuestionnaireSnapshotReadService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CollectionCompletionGateProfileTest {

    @Test
    void developmentAndTestProfilesProvideTheExplicitNoOpAdapter() {
        context("dev").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(CollectionCompletionGate.class);
            assertThat(context.getBean(CollectionCompletionGate.class))
                    .isInstanceOf(NoOpCollectionCompletionGate.class);
        });
        context("test").run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(CollectionCompletionGate.class);
            assertThat(context.getBean(CollectionCompletionGate.class))
                    .isInstanceOf(NoOpCollectionCompletionGate.class);
        });
    }

    @Test
    void productionFailsToCreateTheSessionServiceWithoutAnM6Adapter() {
        context("prod").run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure())
                    .hasStackTraceContaining("CollectionCompletionGate");
        });
    }

    private ApplicationContextRunner context(String profile) {
        return new ApplicationContextRunner()
                .withPropertyValues("spring.profiles.active=" + profile)
                .withUserConfiguration(
                        ParticipantSessionService.class,
                        NoOpCollectionCompletionGate.class
                )
                .withBean(ParticipantSessionRepository.class,
                        () -> mock(ParticipantSessionRepository.class))
                .withBean(StudyRepository.class, () -> mock(StudyRepository.class))
                .withBean(ParticipationReadinessService.class,
                        () -> mock(ParticipationReadinessService.class))
                .withBean(ParticipantSessionTokenService.class,
                        () -> mock(ParticipantSessionTokenService.class))
                .withBean(ConsentDocumentProvider.class,
                        () -> mock(ConsentDocumentProvider.class))
                .withBean(QuestionnaireSnapshotReadService.class,
                        () -> mock(QuestionnaireSnapshotReadService.class))
                .withBean(ParticipantQuestionnaireService.class,
                        () -> mock(ParticipantQuestionnaireService.class))
                .withBean(ParticipationProperties.class,
                        () -> mock(ParticipationProperties.class))
                .withBean(ObjectMapper.class, () -> mock(ObjectMapper.class))
                .withBean(Clock.class, Clock::systemUTC);
    }
}
