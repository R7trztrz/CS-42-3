package com.cs_42_3.surveyplatformbackend.researcher.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
@Schema(description = "Credentials used to authenticate a researcher account.")
public class LoginRequest {

    @Schema(
            description = "Email address associated with the researcher account.",
            example = "researcher@example.com"
    )
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    private String email;

    @Schema(
            description = "Password for the researcher account.",
            example = "SecurePass123"
    )
    @NotBlank(message = "Password is required")
    private String password;
}