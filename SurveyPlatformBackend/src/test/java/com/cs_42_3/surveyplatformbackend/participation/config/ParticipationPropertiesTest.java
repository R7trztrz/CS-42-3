package com.cs_42_3.surveyplatformbackend.participation.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParticipationPropertiesTest {

    @Test
    void acceptsPositiveTimeoutDurations() {
        assertThatCode(() -> properties(Duration.ofMinutes(30), Duration.ofMinutes(1)))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsZeroOrNegativeTimeoutDurations() {
        assertThatThrownBy(() -> properties(Duration.ZERO, Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inactivity-timeout");
        assertThatThrownBy(() -> properties(Duration.ofMinutes(-1), Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("inactivity-timeout");
        assertThatThrownBy(() -> properties(Duration.ofMinutes(30), Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeout-scan-interval");
        assertThatThrownBy(() -> properties(Duration.ofMinutes(30), Duration.ofSeconds(-1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("timeout-scan-interval");
    }

    private ParticipationProperties properties(Duration timeout, Duration interval) {
        return new ParticipationProperties(
                timeout,
                interval,
                true,
                4096,
                "platform-default-v1"
        );
    }
}
