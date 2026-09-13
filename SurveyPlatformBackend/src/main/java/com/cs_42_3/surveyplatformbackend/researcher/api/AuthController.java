package com.cs_42_3.surveyplatformbackend.researcher.api;

import com.cs_42_3.surveyplatformbackend.researcher.api.dto.LoginRequest;
import com.cs_42_3.surveyplatformbackend.researcher.api.dto.LoginResponse;
import com.cs_42_3.surveyplatformbackend.researcher.api.dto.RegisterRequest;
import com.cs_42_3.surveyplatformbackend.researcher.api.dto.RegisterResponse;
import com.cs_42_3.surveyplatformbackend.researcher.domain.Researcher;
import com.cs_42_3.surveyplatformbackend.researcher.service.JwtService;
import com.cs_42_3.surveyplatformbackend.researcher.service.ResearcherService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Provides authentication-related endpoints for researcher accounts.
 *
 * @author Jiale Chen
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final ResearcherService researcherService;
    private final JwtService jwtService;

    public AuthController(
            ResearcherService researcherService,
            JwtService jwtService
    ) {
        this.researcherService = researcherService;
        this.jwtService = jwtService;
    }

    /**
     * Registers a new researcher account.
     *
     * @param request researcher registration request
     * @return registered researcher information
     */
    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        Researcher researcher = researcherService.register(request);

        RegisterResponse response = new RegisterResponse(
                researcher.getId(),
                researcher.getEmail(),
                researcher.getRole()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    /**
     * Authenticates a researcher account and issues a JWT access token.
     *
     * @param request researcher login request
     * @return JWT login response
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        Researcher researcher = researcherService.login(request);

        String token = jwtService.generateToken(researcher);

        LoginResponse response = new LoginResponse(
                token,
                "Bearer",
                3600
        );

        return ResponseEntity.ok(response);
    }
}