package com.cs_42_3.surveyplatformbackend.participation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "participant_questionnaire_steps")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParticipantQuestionnaireStep {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "session_id", nullable = false, updatable = false)
    private UUID sessionId;

    @Column(name = "item_id", nullable = false, updatable = false)
    private UUID itemId;

    @Column(name = "sequence_number", nullable = false, updatable = false)
    private int sequenceNumber;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private UUID idempotencyKey;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "request_hash", nullable = false, updatable = false,
            length = 64, columnDefinition = "char(64)")
    private String requestHash;

    @Column(name = "next_item_id", updatable = false)
    private UUID nextItemId;

    @Column(name = "submitted_at", nullable = false, updatable = false,
            columnDefinition = "timestamptz")
    private Instant submittedAt;

    public static ParticipantQuestionnaireStep create(
            UUID sessionId,
            UUID itemId,
            int sequenceNumber,
            UUID idempotencyKey,
            String requestHash,
            UUID nextItemId,
            Instant submittedAt
    ) {
        if (sequenceNumber < 1) {
            throw new IllegalArgumentException("Questionnaire sequence numbers start at one");
        }
        ParticipantQuestionnaireStep step = new ParticipantQuestionnaireStep();
        step.sessionId = Objects.requireNonNull(sessionId);
        step.itemId = Objects.requireNonNull(itemId);
        step.sequenceNumber = sequenceNumber;
        step.idempotencyKey = Objects.requireNonNull(idempotencyKey);
        step.requestHash = Objects.requireNonNull(requestHash);
        step.nextItemId = nextItemId;
        step.submittedAt = Objects.requireNonNull(submittedAt);
        return step;
    }
}
