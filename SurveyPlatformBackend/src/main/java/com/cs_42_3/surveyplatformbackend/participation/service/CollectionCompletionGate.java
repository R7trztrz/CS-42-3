package com.cs_42_3.surveyplatformbackend.participation.service;

import java.util.UUID;

/**
 * M6-owned completion checks invoked inside M5 completion transactions.
 *
 * <p>M5 supplies only development/test no-op wiring. A production deployment
 * must provide an adapter backed by M6 collection state.
 */
public interface CollectionCompletionGate {
    void assertReadyForBrowsingCompletion(UUID sessionId);

    void assertReadyForSessionCompletion(UUID sessionId);
}
