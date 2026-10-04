package com.cs_42_3.surveyplatformbackend.participation.service;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

/** Development/test-only completion gate used before the real M6 adapter exists. */
@Component
@Profile({"dev", "test"})
public class NoOpCollectionCompletionGate implements CollectionCompletionGate {
    @Override
    public void assertReadyForBrowsingCompletion(UUID sessionId) {
        // Intentionally empty only in explicitly selected non-production profiles.
    }

    @Override
    public void assertReadyForSessionCompletion(UUID sessionId) {
        // Intentionally empty only in explicitly selected non-production profiles.
    }
}
