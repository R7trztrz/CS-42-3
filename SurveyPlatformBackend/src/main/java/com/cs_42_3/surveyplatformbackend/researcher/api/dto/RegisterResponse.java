package com.cs_42_3.surveyplatformbackend.researcher.api.dto;

import com.cs_42_3.surveyplatformbackend.researcher.domain.ResearcherRole;
import lombok.Getter;

import java.util.UUID;

/**
 * Response returned after a researcher account is successfully registered.
 *
 * @author Jiale Chen
 */
@Getter
public class RegisterResponse {

    private final UUID id;
    private final String email;
    private final ResearcherRole role;

    public RegisterResponse(
            UUID id,
            String email,
            ResearcherRole role
    ) {
        this.id = id;
        this.email = email;
        this.role = role;
    }
}
