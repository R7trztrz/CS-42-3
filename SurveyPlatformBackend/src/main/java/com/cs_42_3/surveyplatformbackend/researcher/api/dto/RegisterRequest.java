package com.cs_42_3.surveyplatformbackend.researcher.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
@Schema(description = "Input for registering a new researcher account.")
public class RegisterRequest {

    @Schema(
            description = "Email address for the new researcher account.",
            example = "researcher@example.com"
    )
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 255)
    private String email;

    @Schema(
            description = "Password for the new account. Must contain at least 8 characters.",
            example = "SecurePass123",
            minLength = 8
    )
    @NotBlank(message = "Password is required")
    @Size(
            min = 8,
            message = "Password must be at least 8 characters long"
    )
    private String password;

    @Schema(
            description = "Password confirmation. Must exactly match the password field.",
            example = "SecurePass123",
            minLength = 8
    )
    @NotBlank(message = "Password confirmation is required")
    @Size(
            min = 8,
            message = "Password confirmation must be at least 8 characters long"
    )
    private String confirmPassword;
}