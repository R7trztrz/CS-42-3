package com.cs_42_3.surveyplatformbackend.study.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Allow-lists the device fields collected by M5 without exposing arbitrary persisted JSON keys.
 *
 * @author Simon Tian
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record StudyParticipantDeviceResponse(
        String browser,
        String browserVersion,
        String os,
        Integer screenWidth,
        Integer screenHeight,
        String timezone
) {}
