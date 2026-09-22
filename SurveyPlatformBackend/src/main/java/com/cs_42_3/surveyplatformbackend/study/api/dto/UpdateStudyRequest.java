package com.cs_42_3.surveyplatformbackend.study.api.dto;

import com.cs_42_3.surveyplatformbackend.study.domain.StudyUpdate;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import tools.jackson.databind.JsonNode;

/**
 * Presence-aware PATCH input. Missing nodes are Java null; explicit JSON null
 * remains a null node. Strict node validation prevents scalar coercion.
 *
 * @author Simon Tian
 */
@Schema(description = "Supply version and at least one editable field. Omitted fields remain unchanged.")
public record UpdateStudyRequest(
        @Schema(type = "string", maxLength = 255, description = "Optional; when supplied must be nonblank.", example = "Updated study")
        JsonNode title,
        @Schema(type = "string", nullable = true, description = "Omit to retain; null clears the description.")
        JsonNode description,
        @Schema(type = "boolean", description = "Optional; null is invalid.", example = "false")
        JsonNode eyeTrackingEnabled,
        @Schema(type = "boolean", description = "Optional; null is invalid.", example = "false")
        JsonNode questionnaireEnabled,
        @Schema(type = "integer", format = "int64", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
        JsonNode version
) {
    @AssertTrue(message = "Title must be a nonblank string of at most 255 characters")
    @JsonIgnore
    public boolean isTitleValid() {
        return title == null || (title.isString() && !title.asString().isBlank()
                && title.asString().length() <= 255);
    }

    @AssertTrue(message = "Description must be a string or null")
    @JsonIgnore
    public boolean isDescriptionValid() {
        return description == null || description.isNull() || description.isString();
    }

    @AssertTrue(message = "Runtime settings must be booleans and cannot be null")
    @JsonIgnore
    public boolean isSettingsValid() {
        return (eyeTrackingEnabled == null || eyeTrackingEnabled.isBoolean())
                && (questionnaireEnabled == null || questionnaireEnabled.isBoolean());
    }

    @AssertTrue(message = "Version must be a nonnegative integer within the 64-bit range")
    @JsonIgnore
    public boolean isVersionValid() {
        return version != null && version.isIntegralNumber() && version.canConvertToLong()
                && version.longValue() >= 0;
    }

    @AssertTrue(message = "At least one editable field must be supplied")
    @JsonIgnore
    public boolean isUpdatePresent() {
        return title != null || description != null || eyeTrackingEnabled != null || questionnaireEnabled != null;
    }

    /** Rejects unknown fields locally without changing other API deserialization rules. */
    @JsonAnySetter
    public void rejectUnknownField(String name, JsonNode value) {
        throw new IllegalArgumentException("Unsupported study update field.");
    }

    /** Called after Bean Validation; preserves explicit description clearing. */
    public StudyUpdate toUpdate() {
        return new StudyUpdate(version.longValue(), title == null ? null : title.asString(),
                description != null, description == null || description.isNull() ? null : description.asString(),
                eyeTrackingEnabled == null ? null : eyeTrackingEnabled.booleanValue(),
                questionnaireEnabled == null ? null : questionnaireEnabled.booleanValue());
    }
}
