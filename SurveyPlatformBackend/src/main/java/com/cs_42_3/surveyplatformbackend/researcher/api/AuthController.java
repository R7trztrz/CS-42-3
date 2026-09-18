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
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

/**
 * Provides authentication-related endpoints for researcher accounts.
 *
 * @author Jiale Chen
 */
@RestController
@RequestMapping("/auth")
@Tag(
        name = "Authentication",
        description = "Researcher account registration and authentication APIs."
)
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
    @Operation(
            summary = "Register a researcher account",
            description = """
                Creates a new researcher account using an email address and password.

                The email must be valid and must not already be registered.
                The password must contain at least 8 characters, and password and
                confirmPassword must match.

                New accounts are automatically assigned the RESEARCHER role.
                The password is stored as a BCrypt hash and is never returned by the API.

                This endpoint is public and does not require authentication.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "201",
                    description = "Researcher account created successfully",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = RegisterResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "id": "7b68f56c-5717-4562-b3fc-2c963f6d6f46",
                                          "email": "researcher@example.com",
                                          "role": "RESEARCHER"
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid registration data",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Invalid email",
                                            value = """
                                                {
                                                  "error": "Email must be a valid email address"
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "Weak password",
                                            value = """
                                                {
                                                  "error": "Password must be at least 8 characters long"
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "Password mismatch",
                                            value = """
                                                {
                                                  "error": "Passwords do not match"
                                                }
                                                """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "409",
                    description = "Email address is already registered",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": "Email is already registered. Please log in instead."
                                        }
                                        """
                            )
                    )
            )
    })
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
    @Operation(
            summary = "Log in as a researcher",
            description = """
                Authenticates a researcher using an email address and password.

                If the credentials are valid, the API returns a JWT access token.
                The token must be sent in the Authorization header as a Bearer token
                when accessing protected endpoints.

                Invalid email addresses or passwords return the same authentication
                error so that the API does not reveal whether an account exists.

                This endpoint is public and does not require authentication.
                """
    )
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "Authentication successful",
                    content = @Content(
                            mediaType = "application/json",
                            schema = @Schema(implementation = LoginResponse.class),
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "token": "eyJhbGciOiJIUzI1NiJ9.example.signature",
                                          "tokenType": "Bearer",
                                          "expiresIn": 7200
                                        }
                                        """
                            )
                    )
            ),
            @ApiResponse(
                    responseCode = "400",
                    description = "Invalid login request",
                    content = @Content(
                            mediaType = "application/json",
                            examples = {
                                    @ExampleObject(
                                            name = "Invalid email format",
                                            value = """
                                                {
                                                  "error": "Email must be a valid email address"
                                                }
                                                """
                                    ),
                                    @ExampleObject(
                                            name = "Missing password",
                                            value = """
                                                {
                                                  "error": "Password is required"
                                                }
                                                """
                                    )
                            }
                    )
            ),
            @ApiResponse(
                    responseCode = "401",
                    description = "Invalid email or password",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    value = """
                                        {
                                          "error": "Invalid email or password"
                                        }
                                        """
                            )
                    )
            )
    })
    public ResponseEntity<LoginResponse> login(
            @Valid @RequestBody LoginRequest request
    ) {
        Researcher researcher = researcherService.login(request);

        String token = jwtService.generateToken(researcher);

        LoginResponse response = new LoginResponse(
                token,
                "Bearer",
                7200
        );

        return ResponseEntity.ok(response);
    }
}