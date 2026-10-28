package com.cs_42_3.surveyplatformbackend.export.api.dto;

import com.cs_42_3.surveyplatformbackend.export.domain.ExportDataType;
import jakarta.validation.constraints.*;
import java.util.Set;

/**
 * Explicit category selection; unsupported enum values are rejected rather than ignored.
 *
 * @author Simon Tian
 */
public record CreateExportRequest(
        @NotEmpty(message = "Select at least one supported data category.")
        Set<@NotNull ExportDataType> dataTypes) {
}
