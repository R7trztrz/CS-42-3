package com.cs_42_3.surveyplatformbackend.participation.api.dto;

import jakarta.validation.Valid;

public record CreateParticipantSessionRequest(@Valid ParticipantDeviceInfoRequest deviceInfo) {
}
