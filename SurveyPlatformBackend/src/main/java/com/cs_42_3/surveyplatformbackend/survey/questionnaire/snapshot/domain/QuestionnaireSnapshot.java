package com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Immutable database record for one study's published questionnaire. */
@Entity
@Immutable
@Table(name = "questionnaire_publication_snapshots")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class QuestionnaireSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "study_id", nullable = false, updatable = false, unique = true)
    private UUID studyId;

    @Column(name = "source_questionnaire_id", nullable = false, updatable = false)
    private UUID sourceQuestionnaireId;

    @Column(name = "questionnaire_version", nullable = false, updatable = false)
    private long questionnaireVersion;

    @Column(name = "published_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant publishedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "content", nullable = false, updatable = false, columnDefinition = "jsonb")
    private String content;

    public static QuestionnaireSnapshot create(
            UUID studyId,
            UUID sourceQuestionnaireId,
            long questionnaireVersion,
            Instant publishedAt,
            String content
    ) {
        QuestionnaireSnapshot snapshot = new QuestionnaireSnapshot();
        snapshot.studyId = Objects.requireNonNull(studyId, "Study ID is required");
        snapshot.sourceQuestionnaireId = Objects.requireNonNull(
                sourceQuestionnaireId,
                "Source questionnaire ID is required"
        );
        if (questionnaireVersion < 0) {
            throw new IllegalArgumentException("Questionnaire version must be nonnegative");
        }
        snapshot.questionnaireVersion = questionnaireVersion;
        snapshot.publishedAt = Objects.requireNonNull(publishedAt, "Publication time is required");
        snapshot.content = Objects.requireNonNull(content, "Snapshot content is required");
        return snapshot;
    }
}
