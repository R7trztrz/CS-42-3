package com.cs_42_3.surveyplatformbackend.study.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import tools.jackson.databind.JsonNode;

/**
 * Supplies the last observed study version for publication.
 *
 * @author Simon Tian
 */
public record PublishStudyRequest(
        @Schema(type = "integer", format = "int64", minimum = "0", example = "0") JsonNode version) {
    @JsonIgnore
    @AssertTrue(message = "Version must be a nonnegative integer within the 64-bit range")
    public boolean isVersionValid() {
        return version != null && version.isIntegralNumber()
                && version.canConvertToLong() && version.longValue() >= 0;
    }
}
