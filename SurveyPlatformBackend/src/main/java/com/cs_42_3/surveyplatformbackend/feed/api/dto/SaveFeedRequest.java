package com.cs_42_3.surveyplatformbackend.feed.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import tools.jackson.databind.JsonNode;

/**
 * Carries a complete JSON document and the feed version last read by the client.
 *
 * @author Simon Tian
 */
public record SaveFeedRequest(
        @Schema(type = "object", description = "Complete JSON document; internal structure is not validated yet.")
        JsonNode content,
        @Schema(type = "integer", format = "int64", minimum = "0", example = "0")
        JsonNode version) {

    @JsonIgnore
    @AssertTrue(message = "Content must be a non-null JSON value")
    public boolean isContentPresent() {
        return content != null && !content.isNull();
    }

    @JsonIgnore
    @AssertTrue(message = "Version must be a nonnegative integer within the 64-bit range")
    public boolean isVersionValid() {
        return version != null && version.isIntegralNumber()
                && version.canConvertToLong() && version.longValue() >= 0;
    }
}
