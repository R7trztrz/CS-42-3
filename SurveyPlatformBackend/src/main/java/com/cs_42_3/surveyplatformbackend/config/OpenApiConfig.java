package com.cs_42_3.surveyplatformbackend.config;

import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Declares API documentation metadata; this does not configure JWT verification.
 */
@Configuration(proxyBeanMethods = false)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        description = "Enter a researcher JWT. Signature and expiry validation require Resource Server configuration."
)
public class OpenApiConfig {
}
