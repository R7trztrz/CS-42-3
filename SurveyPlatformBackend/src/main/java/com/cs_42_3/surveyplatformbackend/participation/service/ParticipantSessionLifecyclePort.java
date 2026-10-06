package com.cs_42_3.surveyplatformbackend.participation.service;

import java.time.Instant;
import java.util.UUID;

/** M5 boundary invoked by the future M2 Study-close transaction. */
public interface ParticipantSessionLifecyclePort {
    void abandonActiveSessionsForStudy(UUID studyId, Instant closedAt);
}
