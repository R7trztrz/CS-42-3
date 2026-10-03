package com.cs_42_3.surveyplatformbackend.participation.service;

import java.util.UUID;

/** Authorization boundary for future M6 event-batch writes. */
public interface ParticipantCollectionPolicy {
    void assertCollectionAllowed(UUID sessionId, CollectionKind kind);

    enum CollectionKind {
        BEHAVIOR,
        GAZE
    }
}
