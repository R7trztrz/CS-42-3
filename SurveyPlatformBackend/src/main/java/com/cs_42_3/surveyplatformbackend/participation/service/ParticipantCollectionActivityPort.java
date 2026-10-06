package com.cs_42_3.surveyplatformbackend.participation.service;

import java.util.UUID;

/**
 * Records server-visible activity after M6 has accepted an event batch.
 *
 * <p>The call belongs to the same transaction as permission checking and M6
 * event persistence. It intentionally operates per accepted batch rather than
 * per raw event so high-frequency collection cannot contend on the session row.
 */
public interface ParticipantCollectionActivityPort {
    void recordAcceptedBatch(
            UUID sessionId,
            ParticipantCollectionPolicy.CollectionKind kind
    );
}
