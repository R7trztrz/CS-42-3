package com.cs_42_3.surveyplatformbackend.participation.api.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

public record ParticipantDeviceInfoRequest(
        @Size(max = 64) String browser,
        @Size(max = 32) String browserVersion,
        @Size(max = 64) String os,
        @Min(1) @Max(32768) Integer screenWidth,
        @Min(1) @Max(32768) Integer screenHeight,
        @Size(max = 64) String timezone
) {
}
