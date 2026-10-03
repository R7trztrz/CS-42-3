package com.cs_42_3.surveyplatformbackend.participation.api.dto;

import jakarta.validation.constraints.NotNull;

public record ConsentDecisionRequest(@NotNull Boolean accepted) {
}
