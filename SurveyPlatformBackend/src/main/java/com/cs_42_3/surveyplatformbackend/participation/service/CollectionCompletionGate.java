package com.cs_42_3.surveyplatformbackend.participation.service;

import java.util.UUID;

/** M6-owned completion checks; the production adapter is delivered in phase three. */
public interface CollectionCompletionGate {
    void assertReadyForBrowsingCompletion(UUID sessionId);

    void assertReadyForSessionCompletion(UUID sessionId);
}
