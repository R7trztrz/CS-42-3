package com.cs_42_3.surveyplatformbackend.participation.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;

@Validated
@ConfigurationProperties(prefix = "app.participation")
public record ParticipationProperties(
        @NotNull Duration inactivityTimeout,
        @NotNull Duration timeoutScanInterval,
        boolean timeoutSchedulerEnabled,
        @Min(256) int maxDeviceInfoBytes,
        @NotBlank String consentDocumentVersion
) {
    public ParticipationProperties {
        requirePositive(inactivityTimeout, "inactivity-timeout");
        requirePositive(timeoutScanInterval, "timeout-scan-interval");
    }

    private static void requirePositive(Duration value, String property) {
        if (value != null && (value.isZero() || value.isNegative())) {
            throw new IllegalArgumentException(property + " must be greater than zero");
        }
    }
}
