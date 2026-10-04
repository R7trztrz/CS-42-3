package com.cs_42_3.surveyplatformbackend.participation.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.participation",
        name = "timeout-scheduler-enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class ParticipantSessionTimeoutScheduler {
    private final ParticipantSessionTimeoutService timeouts;

    @Scheduled(
            fixedDelayString = "${app.participation.timeout-scan-interval:1m}",
            initialDelayString = "${app.participation.timeout-scan-interval:1m}"
    )
    public void scan() {
        timeouts.scanOnce();
    }
}
