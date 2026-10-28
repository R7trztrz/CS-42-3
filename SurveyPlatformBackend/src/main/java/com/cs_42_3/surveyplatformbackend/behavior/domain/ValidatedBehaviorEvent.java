package com.cs_42_3.surveyplatformbackend.behavior.domain;

import java.time.Instant;

/**
 * Validated common columns and canonical, allow-listed JSON payload for one event.
 *
 * @author Simon Tian
 */
public record ValidatedBehaviorEvent(int index, BehaviorEventType type, Instant occurredAt, double sessionElapsedMs,
        String pageId, String componentId, String componentType, String payload) { }
