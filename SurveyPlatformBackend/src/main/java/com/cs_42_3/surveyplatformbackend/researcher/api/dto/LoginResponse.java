package com.cs_42_3.surveyplatformbackend.researcher.api.dto;

import lombok.Getter;

/**
 * Response returned after successful researcher authentication.
 *
 * @author Jiale Chen
 */
@Getter
public class LoginResponse {

    private final String token;
    private final String tokenType;
    private final long expiresIn;

    public LoginResponse(
            String token,
            String tokenType,
            long expiresIn
    ) {
        this.token = token;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
    }
}
