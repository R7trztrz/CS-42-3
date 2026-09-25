package com.cs_42_3.surveyplatformbackend.study.domain;

/**
 * A partial update independent of HTTP and JSON. Null values mean no change,
 * except that descriptionPresent distinguishes clearing a description from omission.
 *
 * @author Simon Tian
 */
public record StudyUpdate(
        long version,
        String title,
        boolean descriptionPresent,
        String description,
        Boolean eyeTrackingEnabled,
        Boolean questionnaireEnabled) {}
