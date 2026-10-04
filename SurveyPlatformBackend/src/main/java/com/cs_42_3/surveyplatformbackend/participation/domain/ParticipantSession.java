package com.cs_42_3.surveyplatformbackend.participation.domain;

import com.cs_42_3.surveyplatformbackend.participation.exception.ParticipationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "participant_sessions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ParticipantSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "study_id", nullable = false, updatable = false)
    private UUID studyId;

    @Column(name = "anonymous_participant_id", nullable = false, updatable = false)
    private UUID anonymousParticipantId;

    @Column(name = "session_token_hash", nullable = false, updatable = false,
            length = 64, unique = true, columnDefinition = "char(64)")
    @JdbcTypeCode(SqlTypes.CHAR)
    private String sessionTokenHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ParticipantSessionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "phase", nullable = false, length = 24)
    private ParticipantSessionPhase phase;

    @Enumerated(EnumType.STRING)
    @Column(name = "abandonment_reason", length = 32)
    private ParticipantAbandonmentReason abandonmentReason;

    @Column(name = "current_question_item_id")
    private UUID currentQuestionItemId;

    @Column(name = "questionnaire_ready_at", columnDefinition = "timestamptz")
    private Instant questionnaireReadyAt;

    @Column(name = "consented_at", columnDefinition = "timestamptz")
    private Instant consentedAt;

    @Column(name = "calibration_completed_at", columnDefinition = "timestamptz")
    private Instant calibrationCompletedAt;

    @Column(name = "browsing_completed_at", columnDefinition = "timestamptz")
    private Instant browsingCompletedAt;

    @Column(name = "entered_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant enteredAt;

    @Column(name = "last_activity_at", nullable = false, columnDefinition = "timestamptz")
    private Instant lastActivityAt;

    @Column(name = "completed_at", columnDefinition = "timestamptz")
    private Instant completedAt;

    @Column(name = "abandoned_at", columnDefinition = "timestamptz")
    private Instant abandonedAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "device_info", columnDefinition = "jsonb")
    private String deviceInfo;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion;

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    private Instant updatedAt;

    public static ParticipantSession create(
            UUID studyId,
            UUID anonymousParticipantId,
            String sessionTokenHash,
            String deviceInfo,
            Instant now
    ) {
        ParticipantSession session = new ParticipantSession();
        session.studyId = Objects.requireNonNull(studyId);
        session.anonymousParticipantId = Objects.requireNonNull(anonymousParticipantId);
        session.sessionTokenHash = Objects.requireNonNull(sessionTokenHash);
        session.deviceInfo = deviceInfo;
        session.status = ParticipantSessionStatus.IN_PROGRESS;
        session.phase = ParticipantSessionPhase.CONSENT;
        session.enteredAt = Objects.requireNonNull(now);
        session.lastActivityAt = now;
        session.createdAt = now;
        session.updatedAt = now;
        return session;
    }

    /** Applies a consent decision once and treats an identical retry as a no-op. */
    public boolean decideConsent(boolean accepted, boolean eyeTrackingEnabled, Instant now) {
        if (accepted && consentedAt != null) {
            return false;
        }
        if (!accepted && abandonmentReason == ParticipantAbandonmentReason.CONSENT_DECLINED) {
            return false;
        }
        if (status != ParticipantSessionStatus.IN_PROGRESS) {
            throw ParticipationException.terminated();
        }
        if (phase != ParticipantSessionPhase.CONSENT) {
            throw ParticipationException.invalidState();
        }
        if (accepted) {
            consentedAt = now;
            phase = eyeTrackingEnabled
                    ? ParticipantSessionPhase.CALIBRATION
                    : ParticipantSessionPhase.BROWSING;
        } else {
            deviceInfo = null;
            status = ParticipantSessionStatus.ABANDONED;
            phase = ParticipantSessionPhase.FINISHED;
            abandonmentReason = ParticipantAbandonmentReason.CONSENT_DECLINED;
            abandonedAt = now;
        }
        touch(now);
        return true;
    }

    /** Moves the M5 session after M6 has atomically persisted its calibration outcome. */
    public boolean completeCalibration(Instant now) {
        if (calibrationCompletedAt != null) {
            return false;
        }
        requireInProgress(ParticipantSessionPhase.CALIBRATION);
        calibrationCompletedAt = now;
        phase = ParticipantSessionPhase.BROWSING;
        touch(now);
        return true;
    }

    /** Completes browsing, either finishing the study or exposing the first published item. */
    public boolean completeBrowsing(boolean questionnaireEnabled, UUID firstQuestionItemId, Instant now) {
        if (browsingCompletedAt != null) {
            return false;
        }
        requireInProgress(ParticipantSessionPhase.BROWSING);
        browsingCompletedAt = now;
        if (questionnaireEnabled) {
            currentQuestionItemId = Objects.requireNonNull(firstQuestionItemId);
            phase = ParticipantSessionPhase.QUESTIONNAIRE;
        } else {
            status = ParticipantSessionStatus.COMPLETED;
            phase = ParticipantSessionPhase.FINISHED;
            completedAt = now;
        }
        touch(now);
        return true;
    }

    /** Advances exactly one published questionnaire item in the same answer transaction. */
    public void advanceQuestionnaire(UUID expectedItemId, UUID nextItemId, Instant now) {
        requireInProgress(ParticipantSessionPhase.QUESTIONNAIRE);
        if (!Objects.equals(currentQuestionItemId, expectedItemId)) {
            throw ParticipationException.questionNotCurrent();
        }
        currentQuestionItemId = nextItemId;
        questionnaireReadyAt = nextItemId == null ? Objects.requireNonNull(now) : null;
        touch(now);
    }

    /** Completes a questionnaire only after its saved path has reached END. */
    public boolean completeQuestionnaire(Instant now) {
        if (status == ParticipantSessionStatus.COMPLETED) {
            return false;
        }
        requireInProgress(ParticipantSessionPhase.QUESTIONNAIRE);
        if (currentQuestionItemId != null || questionnaireReadyAt == null) {
            throw ParticipationException.questionnaireNotReady();
        }
        status = ParticipantSessionStatus.COMPLETED;
        phase = ParticipantSessionPhase.FINISHED;
        completedAt = Objects.requireNonNull(now);
        questionnaireReadyAt = null;
        touch(now);
        return true;
    }

    /** Applies an explicit participant exit without overwriting an existing terminal outcome. */
    public boolean abandonByParticipant(Instant now) {
        if (status == ParticipantSessionStatus.ABANDONED) {
            return false;
        }
        if (status == ParticipantSessionStatus.COMPLETED) {
            throw ParticipationException.terminated();
        }
        finishAsAbandoned(ParticipantAbandonmentReason.PARTICIPANT_EXIT, now);
        return true;
    }

    /** Times out only the still-inactive state observed while holding the session lock. */
    public boolean abandonIfInactive(Instant cutoff, Instant now) {
        if (status != ParticipantSessionStatus.IN_PROGRESS
                || !lastActivityAt.isBefore(Objects.requireNonNull(cutoff))) {
            return false;
        }
        finishAsAbandoned(ParticipantAbandonmentReason.INACTIVITY_TIMEOUT, now);
        return true;
    }

    /** Used by the M5 lifecycle adapter inside the Study-close transaction. */
    public boolean abandonForStudyClosure(Instant now) {
        if (status != ParticipantSessionStatus.IN_PROGRESS) {
            return false;
        }
        finishAsAbandoned(ParticipantAbandonmentReason.STUDY_CLOSED, now);
        return true;
    }

    private void requireInProgress(ParticipantSessionPhase requiredPhase) {
        if (status != ParticipantSessionStatus.IN_PROGRESS) {
            throw ParticipationException.terminated();
        }
        if (phase != requiredPhase) {
            throw ParticipationException.invalidState();
        }
    }

    private void finishAsAbandoned(ParticipantAbandonmentReason reason, Instant now) {
        status = ParticipantSessionStatus.ABANDONED;
        phase = ParticipantSessionPhase.FINISHED;
        abandonmentReason = Objects.requireNonNull(reason);
        abandonedAt = Objects.requireNonNull(now);
        currentQuestionItemId = null;
        questionnaireReadyAt = null;
        touch(now);
    }

    private void touch(Instant now) {
        lastActivityAt = Objects.requireNonNull(now);
        updatedAt = now;
    }
}
