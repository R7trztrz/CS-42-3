package com.cs_42_3.surveyplatformbackend.participation.domain;

import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "participant_answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParticipantAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "step_id", nullable = false, updatable = false, unique = true)
    private UUID stepId;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type", nullable = false, updatable = false, length = 24)
    private QuestionType questionType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "answer_payload", nullable = false, updatable = false, columnDefinition = "jsonb")
    private String answerPayload;

    @Column(name = "answered_at", nullable = false, updatable = false,
            columnDefinition = "timestamptz")
    private Instant answeredAt;

    public static ParticipantAnswer create(
            UUID stepId,
            QuestionType questionType,
            String answerPayload,
            Instant answeredAt
    ) {
        ParticipantAnswer answer = new ParticipantAnswer();
        answer.stepId = Objects.requireNonNull(stepId);
        answer.questionType = Objects.requireNonNull(questionType);
        answer.answerPayload = Objects.requireNonNull(answerPayload);
        answer.answeredAt = Objects.requireNonNull(answeredAt);
        return answer;
    }
}
