package com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.service;

import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.QuestionnaireContentSource;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.domain.QuestionnaireSnapshot;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.domain.QuestionnaireSnapshotPayload;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.repository.QuestionnaireSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionnaireSnapshotReadServiceTest {

    @Mock
    private QuestionnaireSnapshotRepository repository;

    @Test
    void reconstructsPublishedResponseOnlyFromSnapshotContent() {
        UUID studyId = UUID.randomUUID();
        UUID questionnaireId = UUID.randomUUID();
        UUID snapshotId = UUID.randomUUID();
        UUID itemId = UUID.randomUUID();
        UUID questionId = UUID.randomUUID();
        Instant publishedAt = Instant.parse("2026-10-02T06:00:00Z");
        QuestionnaireSnapshotPayload payload = new QuestionnaireSnapshotPayload(
                questionnaireId,
                studyId,
                7,
                Instant.parse("2026-10-02T05:59:00Z"),
                List.of(new QuestionnaireSnapshotPayload.Item(
                        itemId,
                        questionId,
                        0,
                        QuestionType.TEXT,
                        "Published wording",
                        true,
                        List.of(),
                        null,
                        null,
                        null,
                        null,
                        Instant.parse("2026-10-01T00:00:00Z"),
                        Instant.parse("2026-10-02T05:00:00Z"),
                        null,
                        List.of()
                ))
        );
        ObjectMapper mapper = new ObjectMapper();
        QuestionnaireSnapshot snapshot = QuestionnaireSnapshot.create(
                studyId,
                questionnaireId,
                7,
                publishedAt,
                mapper.writeValueAsString(payload)
        );
        ReflectionTestUtils.setField(snapshot, "id", snapshotId);
        when(repository.findByStudyId(studyId)).thenReturn(Optional.of(snapshot));

        var response = new QuestionnaireSnapshotReadService(repository, mapper)
                .getPublishedQuestionnaire(studyId);

        assertThat(response.contentSource()).isEqualTo(QuestionnaireContentSource.PUBLISHED_SNAPSHOT);
        assertThat(response.snapshotId()).isEqualTo(snapshotId);
        assertThat(response.publishedAt()).isEqualTo(publishedAt);
        assertThat(response.items()).singleElement().satisfies(item -> {
            assertThat(item.itemId()).isEqualTo(itemId);
            assertThat(item.question().questionId()).isEqualTo(questionId);
            assertThat(item.question().questionText()).isEqualTo("Published wording");
            assertThat(item.defaultNextItemId()).isNull();
        });
    }
}
