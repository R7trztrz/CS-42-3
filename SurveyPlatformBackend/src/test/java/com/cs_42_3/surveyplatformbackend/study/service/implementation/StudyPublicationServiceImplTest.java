package com.cs_42_3.surveyplatformbackend.study.service.implementation;

import com.cs_42_3.surveyplatformbackend.asset.exception.AssetException;
import com.cs_42_3.surveyplatformbackend.asset.service.AssetReferences;
import com.cs_42_3.surveyplatformbackend.feed.domain.StudyFeed;
import com.cs_42_3.surveyplatformbackend.feed.repository.StudyFeedRepository;
import com.cs_42_3.surveyplatformbackend.participation.service.ConsentDocument;
import com.cs_42_3.surveyplatformbackend.participation.service.ConsentDocumentProvider;
import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.exception.QuestionnairePublicationException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.service.QuestionnaireSnapshotPublicationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudyPublicationServiceImplTest {

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID STUDY_ID = UUID.randomUUID();

    @Mock
    private StudyRepository studies;
    @Mock
    private StudyFeedRepository feeds;
    @Mock
    private QuestionnaireSnapshotPublicationService snapshots;
    @Mock
    private AssetReferences assetReferences;
    @Mock
    private StudyFeed feed;
    @Mock
    private ConsentDocumentProvider consentDocuments;

    private StudyPublicationServiceImpl service;
    private Study study;

    @BeforeEach
    void setUp() {
        lenient().when(consentDocuments.currentDocument()).thenReturn(new ConsentDocument(
                "platform-default-v1", "Test", "Test fixture", false
        ));
        service = new StudyPublicationServiceImpl(
                studies,
                feeds,
                snapshots,
                new ObjectMapper(),
                assetReferences,
                consentDocuments,
                Clock.fixed(Instant.parse("2026-10-03T00:00:00Z"), ZoneOffset.UTC)
        );
        study = new Study(OWNER_ID, "Study", null);
        ReflectionTestUtils.setField(study, "id", STUDY_ID);
        ReflectionTestUtils.setField(study, "lockVersion", 2L);
        when(studies.findOwnedByIdForUpdate(STUDY_ID, OWNER_ID)).thenReturn(Optional.of(study));
        when(feeds.findById(STUDY_ID)).thenReturn(Optional.of(feed));
        when(feed.getContent()).thenReturn("{\"ROOT\":{}}");
    }

    @Test
    void disabledQuestionnairePublishesWithoutSnapshot() {
        Study result = service.publish(OWNER_ID, STUDY_ID, 2);

        assertThat(result.getStatus()).isEqualTo(StudyStatus.COLLECTING);
        assertThat(result.getPublishedAt()).isNotNull();
        assertThat(result.getConsentDocumentVersion()).isEqualTo("platform-default-v1");
        assertThat(result.getParticipationToken()).hasSize(43);
        verify(assetReferences).validate(STUDY_ID, "{\"ROOT\":{}}");
        verifyNoInteractions(snapshots);
        verify(studies).flush();
    }

    @Test
    void enabledQuestionnaireCreatesSnapshotAtSamePublicationInstant() {
        ReflectionTestUtils.setField(study, "questionnaireEnabled", true);

        service.publish(OWNER_ID, STUDY_ID, 2);

        ArgumentCaptor<Instant> instant = ArgumentCaptor.forClass(Instant.class);
        verify(snapshots).createSnapshot(org.mockito.ArgumentMatchers.same(study), instant.capture());
        assertThat(study.getPublishedAt()).isEqualTo(instant.getValue());
        assertThat(study.getStatus()).isEqualTo(StudyStatus.COLLECTING);
    }

    @Test
    void snapshotFailureLeavesStudyInDraftAndDoesNotFlushState() {
        ReflectionTestUtils.setField(study, "questionnaireEnabled", true);
        when(snapshots.createSnapshot(any(), any())).thenThrow(
                new QuestionnairePublicationException("Invalid questionnaire", List.of())
        );

        assertThatThrownBy(() -> service.publish(OWNER_ID, STUDY_ID, 2))
                .isInstanceOf(QuestionnairePublicationException.class);
        assertThat(study.getStatus()).isEqualTo(StudyStatus.DRAFT);
        assertThat(study.getPublishedAt()).isNull();
        verify(studies, never()).flush();
    }

    @Test
    void invalidAssetReferenceStopsPublicationBeforeSnapshotCreation() {
        ReflectionTestUtils.setField(study, "questionnaireEnabled", true);
        doThrow(AssetException.reference()).when(assetReferences).validate(STUDY_ID, "{\"ROOT\":{}}");

        assertThatThrownBy(() -> service.publish(OWNER_ID, STUDY_ID, 2))
                .isInstanceOf(AssetException.class);
        assertThat(study.getStatus()).isEqualTo(StudyStatus.DRAFT);
        verifyNoInteractions(snapshots);
        verify(studies, never()).flush();
    }
}
