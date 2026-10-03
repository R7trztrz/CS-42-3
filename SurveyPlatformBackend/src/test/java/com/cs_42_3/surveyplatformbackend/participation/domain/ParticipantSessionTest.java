package com.cs_42_3.surveyplatformbackend.participation.domain;

import com.cs_42_3.surveyplatformbackend.participation.exception.ParticipationException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParticipantSessionTest {
    private static final Instant ENTERED = Instant.parse("2026-10-03T00:00:00Z");
    private static final Instant LATER = Instant.parse("2026-10-03T00:01:00Z");

    @Test
    void acceptingConsentAdvancesOnceWithoutOverwritingTimestamp() {
        ParticipantSession session = session("{\"browser\":\"Test\"}");

        assertThat(session.decideConsent(true, false, ENTERED)).isTrue();
        assertThat(session.getStatus()).isEqualTo(ParticipantSessionStatus.IN_PROGRESS);
        assertThat(session.getPhase()).isEqualTo(ParticipantSessionPhase.BROWSING);
        assertThat(session.getConsentedAt()).isEqualTo(ENTERED);

        assertThat(session.decideConsent(true, false, LATER)).isFalse();
        assertThat(session.getConsentedAt()).isEqualTo(ENTERED);
        assertThat(session.getLastActivityAt()).isEqualTo(ENTERED);
        assertThatThrownBy(() -> session.decideConsent(false, false, LATER))
                .isInstanceOf(ParticipationException.class);
    }

    @Test
    void decliningConsentLeavesOnlyMinimalAnonymousTerminalState() {
        ParticipantSession session = session("{\"browser\":\"Test\"}");

        assertThat(session.decideConsent(false, true, LATER)).isTrue();

        assertThat(session.getStatus()).isEqualTo(ParticipantSessionStatus.ABANDONED);
        assertThat(session.getPhase()).isEqualTo(ParticipantSessionPhase.FINISHED);
        assertThat(session.getAbandonmentReason())
                .isEqualTo(ParticipantAbandonmentReason.CONSENT_DECLINED);
        assertThat(session.getDeviceInfo()).isNull();
        assertThat(session.getConsentedAt()).isNull();
        assertThat(session.getAbandonedAt()).isEqualTo(LATER);
        assertThat(session.decideConsent(false, true, LATER.plusSeconds(60))).isFalse();
    }

    @Test
    void calibrationAndBrowsingFollowTheServerStateMachine() {
        ParticipantSession session = session(null);
        session.decideConsent(true, true, ENTERED);

        assertThat(session.getPhase()).isEqualTo(ParticipantSessionPhase.CALIBRATION);
        assertThat(session.completeCalibration(LATER)).isTrue();
        assertThat(session.completeCalibration(LATER.plusSeconds(1))).isFalse();
        assertThat(session.getPhase()).isEqualTo(ParticipantSessionPhase.BROWSING);

        UUID itemId = UUID.randomUUID();
        assertThat(session.completeBrowsing(true, itemId, LATER.plusSeconds(2))).isTrue();
        assertThat(session.getPhase()).isEqualTo(ParticipantSessionPhase.QUESTIONNAIRE);
        assertThat(session.getCurrentQuestionItemId()).isEqualTo(itemId);
        assertThat(session.completeBrowsing(true, itemId, LATER.plusSeconds(3))).isFalse();
    }

    @Test
    void browsingWithoutQuestionnaireCompletesAndCannotBeReopened() {
        ParticipantSession session = session(null);
        session.decideConsent(true, false, ENTERED);
        session.completeBrowsing(false, null, LATER);

        assertThat(session.getStatus()).isEqualTo(ParticipantSessionStatus.COMPLETED);
        assertThat(session.getPhase()).isEqualTo(ParticipantSessionPhase.FINISHED);
        assertThat(session.getCompletedAt()).isEqualTo(LATER);
        assertThatThrownBy(() -> session.completeCalibration(LATER.plusSeconds(1)))
                .isInstanceOf(ParticipationException.class);
    }

    @Test
    void rejectsCalibrationAndBrowsingBeforeConsent() {
        ParticipantSession session = session(null);

        assertThatThrownBy(() -> session.completeCalibration(LATER))
                .isInstanceOf(ParticipationException.class);
        assertThatThrownBy(() -> session.completeBrowsing(false, null, LATER))
                .isInstanceOf(ParticipationException.class);
        assertThat(session.getPhase()).isEqualTo(ParticipantSessionPhase.CONSENT);
    }

    private ParticipantSession session(String deviceInfo) {
        return ParticipantSession.create(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "a".repeat(64),
                deviceInfo,
                ENTERED
        );
    }
}
