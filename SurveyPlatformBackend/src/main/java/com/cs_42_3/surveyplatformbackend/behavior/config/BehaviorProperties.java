package com.cs_42_3.surveyplatformbackend.behavior.config;

import jakarta.validation.constraints.*;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Caps incoming bytes before parsing and limits the number of events persisted per transaction.
 *
 * @author Simon Tian
 */
@Validated
@ConfigurationProperties(prefix = "app.behavior")
public record BehaviorProperties(
        @DefaultValue("1000") @Min(1) @Max(10000) int maxEventsPerBatch,
        @DefaultValue("1048576") @Min(1024) @Max(8388608) int maxPayloadBytes) { }
