package com.cs_42_3.surveyplatformbackend.researcher.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for changing a researcher's password.
 *
 * @author Jiale Chen
 */
public class ChangePasswordRequest {

    @Schema(
            description = "Researcher's current password",
            example = "OldPassword123"
    )
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @Schema(
            description = "New password. Must contain at least 8 characters.",
            example = "NewPassword123",
            minLength = 8
    )
    @NotBlank(message = "New password is required")
    @Size(min = 8, message = "New password must be at least 8 characters long")
    private String newPassword;

    @Schema(
            description = "Confirmation of the new password. Must match newPassword.",
            example = "NewPassword123"
    )
    @NotBlank(message = "Password confirmation is required")
    private String confirmNewPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmNewPassword() {
        return confirmNewPassword;
    }

    public void setConfirmNewPassword(String confirmNewPassword) {
        this.confirmNewPassword = confirmNewPassword;
    }
}