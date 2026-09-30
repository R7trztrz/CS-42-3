package com.cs_42_3.surveyplatformbackend.study.api.dto;

import tools.jackson.databind.JsonNode;

/**
 * Exposes only participant-facing configuration, without owner or management metadata.
 *
 * @author Simon Tian
 */
public record ParticipationResponse(String title,
                                    String description,
                                    boolean eyeTrackingEnabled,
                                    boolean questionnaireEnabled,
                                    String theme,
                                    JsonNode content) {}
