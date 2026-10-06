package com.cs_42_3.surveyplatformbackend.participation.service;

import java.util.UUID;

/**
 * Authorization boundary for M6 event-batch writes.
 *
 * <p>M6 must call this port from the same transaction that persists the
 * accepted batch. The implementation keeps the Study and participant-session
 * locks until that caller transaction completes.
 */
public interface ParticipantCollectionPolicy {
    void assertCollectionAllowed(UUID sessionId, CollectionKind kind);

    enum CollectionKind {
        BEHAVIOR,
        GAZE
    }
}
