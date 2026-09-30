package com.cs_42_3.surveyplatformbackend.researcher;

import com.cs_42_3.surveyplatformbackend.researcher.domain.Researcher;
import com.cs_42_3.surveyplatformbackend.researcher.repository.ResearcherRepository;
import com.cs_42_3.surveyplatformbackend.security.turnstile.TurnstileService;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.when;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("survey_platform_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add(
                "spring.datasource.url",
                postgres::getJdbcUrl
        );

        registry.add(
                "spring.datasource.username",
                postgres::getUsername
        );

        registry.add(
                "spring.datasource.password",
                postgres::getPassword
        );

        registry.add(
                "security.jwt.secret",
                () -> "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
        );
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResearcherRepository researcherRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private TurnstileService turnstileService;

    private Researcher researcher;

    private static final String EMAIL =
            "auth-test@example.com";

    private static final String OLD_PASSWORD =
            "OldPassword123";

    private static final String NEW_PASSWORD =
            "NewPassword123";

    @BeforeEach
    void setUp() {
        researcher = researcherRepository.saveAndFlush(
                new Researcher(
                        EMAIL,
                        passwordEncoder.encode(OLD_PASSWORD)
                )
        );

        when(turnstileService.verify("test-token"))
                .thenReturn(true);
    }

    /** Verifies that valid researcher credentials return a JWT login response. */
    @Test
    void shouldLoginWithValidCredentials() throws Exception {

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "email": "auth-test@example.com",
                                  "password": "OldPassword123"
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tokenType")
                        .value("Bearer"))
                .andExpect(jsonPath("$.expiresIn")
                        .value(7200));
    }

    /** Verifies that wrong passwords and unknown emails return the same login error. */
    @Test
    void shouldReturnSameErrorForWrongPasswordAndUnknownEmail()
            throws Exception {

        String wrongPasswordResponse =
                mockMvc.perform(
                                post("/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                        {
                                          "email": "auth-test@example.com",
                                          "password": "WrongPassword123"
                                        }
                                        """)
                        )
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code")
                                .value("AUTH_INVALID_CREDENTIALS"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String unknownEmailResponse =
                mockMvc.perform(
                                post("/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                        {
                                          "email": "nobody@example.com",
                                          "password": "WrongPassword123"
                                        }
                                        """)
                        )
                        .andExpect(status().isUnauthorized())
                        .andExpect(jsonPath("$.code")
                                .value("AUTH_INVALID_CREDENTIALS"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        assertEquals(
                wrongPasswordResponse,
                unknownEmailResponse
        );
    }

    /** Verifies that a successful login returns a JWT with the expected identity, role, and expiry. */
    @Test
    void shouldReturnJwtWithExpectedClaimsAndExpiry() throws Exception {

        String responseBody =
                mockMvc.perform(
                                post("/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                        {
                                          "email": "auth-test@example.com",
                                          "password": "OldPassword123"
                                        }
                                        """)
                        )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String token = JsonPath.read(responseBody, "$.token");

        Jwt jwt = jwtDecoder.decode(token);

        assertEquals(
                researcher.getId().toString(),
                jwt.getSubject()
        );

        assertEquals(
                researcher.getId().toString(),
                jwt.getClaimAsString("userId")
        );

        assertEquals(
                "RESEARCHER",
                jwt.getClaimAsString("role")
        );

        assertNotNull(jwt.getIssuedAt());
        assertNotNull(jwt.getExpiresAt());

        assertEquals(
                Duration.ofHours(2),
                Duration.between(
                        jwt.getIssuedAt(),
                        jwt.getExpiresAt()
                )
        );
    }

    /** Verifies that changing the password invalidates the old password and enables the new one. */
    @Test
    void shouldChangePasswordAndUseNewPasswordForLogin() throws Exception {

        String loginResponse =
                mockMvc.perform(
                                post("/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                        {
                                          "email": "auth-test@example.com",
                                          "password": "OldPassword123"
                                        }
                                        """)
                        )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String token = JsonPath.read(loginResponse, "$.token");

        mockMvc.perform(
                        post("/auth/change-password")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "currentPassword": "OldPassword123",
                                  "newPassword": "NewPassword123",
                                  "confirmNewPassword": "NewPassword123"
                                }
                                """)
                )
                .andExpect(status().isNoContent());

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "email": "auth-test@example.com",
                                  "password": "OldPassword123"
                                }
                                """)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code")
                        .value("AUTH_INVALID_CREDENTIALS"));

        mockMvc.perform(
                        post("/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "email": "auth-test@example.com",
                                  "password": "NewPassword123"
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    /** Verifies that password change is rejected when the current password is incorrect. */
    @Test
    void shouldRejectPasswordChangeWhenCurrentPasswordIsIncorrect()
            throws Exception {

        String loginResponse =
                mockMvc.perform(
                                post("/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                        {
                                          "email": "auth-test@example.com",
                                          "password": "OldPassword123"
                                        }
                                        """)
                        )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String token = JsonPath.read(loginResponse, "$.token");

        mockMvc.perform(
                        post("/auth/change-password")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "currentPassword": "WrongPassword123",
                                  "newPassword": "NewPassword123",
                                  "confirmNewPassword": "NewPassword123"
                                }
                                """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("AUTH_CURRENT_PASSWORD_INCORRECT"));
    }

    /** Verifies that a valid registration creates a researcher account with the RESEARCHER role. */
    @Test
    void shouldRegisterResearcherSuccessfully() throws Exception {

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "email": "new-researcher@example.com",
                                  "password": "ValidPassword123",
                                  "confirmPassword": "ValidPassword123",
                                  "captchaToken": "test-token"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email")
                        .value("new-researcher@example.com"))
                .andExpect(jsonPath("$.role")
                        .value("RESEARCHER"));
    }

    /** Verifies that registration is rejected when the email is already registered. */
    @Test
    void shouldRejectRegistrationWithDuplicateEmail() throws Exception {

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "email": "auth-test@example.com",
                                  "password": "ValidPassword123",
                                  "confirmPassword": "ValidPassword123",
                                  "captchaToken": "test-token"
                                }
                                """)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("AUTH_DUPLICATE_EMAIL"));
    }

    /** Verifies that registration is rejected when the password confirmation does not match. */
    @Test
    void shouldRejectRegistrationWithPasswordMismatch() throws Exception {

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "email": "mismatch@example.com",
                                  "password": "ValidPassword123",
                                  "confirmPassword": "DifferentPassword123",
                                  "captchaToken": "test-token"
                                }
                                """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("AUTH_PASSWORD_MISMATCH"));
    }

    /** Verifies that registration is rejected when the password is shorter than eight characters. */
    @Test
    void shouldRejectRegistrationWithShortPassword() throws Exception {

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "email": "short-password@example.com",
                                  "password": "Short1",
                                  "confirmPassword": "Short1",
                                  "captchaToken": "test-token"
                                }
                                """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("REQUEST_VALIDATION_FAILED"));
    }

    /** Verifies that registration rejects a password containing only digits. */
    @Test
    void shouldRejectRegistrationWithNumericOnlyPassword() throws Exception {

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "email": "numeric-password@example.com",
                                  "password": "12345678",
                                  "confirmPassword": "12345678",
                                  "captchaToken": "test-token"
                                }
                                """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("REQUEST_VALIDATION_FAILED"));
    }

    /** Verifies that password change rejects a new password containing only digits. */
    @Test
    void shouldRejectPasswordChangeWithNumericOnlyNewPassword()
            throws Exception {

        String loginResponse =
                mockMvc.perform(
                                post("/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                        {
                                          "email": "auth-test@example.com",
                                          "password": "OldPassword123"
                                        }
                                        """)
                        )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String token = JsonPath.read(loginResponse, "$.token");

        mockMvc.perform(
                        post("/auth/change-password")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "currentPassword": "OldPassword123",
                                  "newPassword": "12345678",
                                  "confirmNewPassword": "12345678"
                                }
                                """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("REQUEST_VALIDATION_FAILED"));
    }

    /** Verifies that registered passwords are stored as BCrypt hashes rather than plaintext. */
    @Test
    void shouldStoreRegisteredPasswordAsBcryptHash() throws Exception {

        mockMvc.perform(
                        post("/auth/register")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "email": "bcrypt-test@example.com",
                                  "password": "SecurePassword123",
                                  "confirmPassword": "SecurePassword123",
                                  "captchaToken": "test-token"
                                }
                                """)
                )
                .andExpect(status().isCreated());

        Researcher savedResearcher = researcherRepository
                .findByEmail("bcrypt-test@example.com")
                .orElseThrow();

        assertNotEquals(
                "SecurePassword123",
                savedResearcher.getPasswordHash()
        );

        assertTrue(
                savedResearcher.getPasswordHash().startsWith("$2")
        );

        assertTrue(
                passwordEncoder.matches(
                        "SecurePassword123",
                        savedResearcher.getPasswordHash()
                )
        );
    }

    /** Verifies that password change rejects a new password shorter than eight characters. */
    @Test
    void shouldRejectPasswordChangeWithShortNewPassword()
            throws Exception {

        String loginResponse =
                mockMvc.perform(
                                post("/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                        {
                                          "email": "auth-test@example.com",
                                          "password": "OldPassword123"
                                        }
                                        """)
                        )
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String token = JsonPath.read(loginResponse, "$.token");

        mockMvc.perform(
                        post("/auth/change-password")
                                .header(
                                        "Authorization",
                                        "Bearer " + token
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "currentPassword": "OldPassword123",
                                  "newPassword": "Short1",
                                  "confirmNewPassword": "Short1"
                                }
                                """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("REQUEST_VALIDATION_FAILED"));
    }


    /** Verifies that the login endpoint returns 429 after ten requests from the same IP. */
    @Test
    void shouldReturnTooManyRequestsWhenLoginRateLimitIsExceeded()
            throws Exception {

        String clientIp = "192.0.2.210";

        for (int i = 0; i < 10; i++) {
            mockMvc.perform(
                            post("/auth/login")
                                    .with(request -> {
                                        request.setRemoteAddr(clientIp);
                                        return request;
                                    })
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                {
                                  "email": "auth-test@example.com",
                                  "password": "WrongPassword123"
                                }
                                """)
                    )
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(
                        post("/auth/login")
                                .with(request -> {
                                    request.setRemoteAddr(clientIp);
                                    return request;
                                })
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                              "email": "auth-test@example.com",
                              "password": "OldPassword123"
                            }
                            """)
                )
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code")
                        .value("AUTH_RATE_LIMIT_EXCEEDED"));
    }

    /** Verifies that the registration endpoint returns 429 after five requests from the same IP. */
    @Test
    void shouldReturnTooManyRequestsWhenRegistrationRateLimitIsExceeded()
            throws Exception {

        String clientIp = "192.0.2.211";

        for (int i = 0; i < 5; i++) {
            mockMvc.perform(
                            post("/auth/register")
                                    .with(request -> {
                                        request.setRemoteAddr(clientIp);
                                        return request;
                                    })
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                {
                                  "email": "auth-test@example.com",
                                  "password": "ValidPassword123",
                                  "confirmPassword": "ValidPassword123",
                                  "captchaToken": "test-token"
                                }
                                """)
                    )
                    .andExpect(status().isConflict());
        }

        mockMvc.perform(
                        post("/auth/register")
                                .with(request -> {
                                    request.setRemoteAddr(clientIp);
                                    return request;
                                })
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                            {
                              "email": "auth-test@example.com",
                              "password": "ValidPassword123",
                              "confirmPassword": "ValidPassword123",
                              "captchaToken": "test-token"
                            }
                            """)
                )
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code")
                        .value("AUTH_RATE_LIMIT_EXCEEDED"));
    }


    /** Verifies that unexpected registration fields are ignored and cannot change the assigned role. */
    @Test
    void shouldIgnoreUnexpectedRegistrationFields() throws Exception {

        mockMvc.perform(
                        post("/auth/register")
                                .with(request -> {
                                    request.setRemoteAddr("192.0.2.230");
                                    return request;
                                })
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "email": "minimal-data@example.com",
                                  "password": "ValidPassword123",
                                  "confirmPassword": "ValidPassword123",
                                  "captchaToken": "test-token",
                                  "role": "ADMIN",
                                  "phone": "123456789"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role")
                        .value("RESEARCHER"))
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist());

        Researcher savedResearcher = researcherRepository
                .findByEmail("minimal-data@example.com")
                .orElseThrow();

        assertEquals(
                "RESEARCHER",
                savedResearcher.getRole().name()
        );
    }


    /** Verifies that unexpected login fields cannot alter the authenticated identity or role. */
    @Test
    void shouldIgnoreUnexpectedLoginFields() throws Exception {

        String responseBody =
                mockMvc.perform(
                                post("/auth/login")
                                        .with(request -> {
                                            request.setRemoteAddr("192.0.2.231");
                                            return request;
                                        })
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                        {
                                          "email": "auth-test@example.com",
                                          "password": "OldPassword123",
                                          "role": "ADMIN",
                                          "phone": "123456789"
                                        }
                                        """)
                        )
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.phone").doesNotExist())
                        .andExpect(jsonPath("$.password").doesNotExist())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        String token = JsonPath.read(responseBody, "$.token");

        Jwt jwt = jwtDecoder.decode(token);

        assertEquals(
                researcher.getId().toString(),
                jwt.getSubject()
        );

        assertEquals(
                "RESEARCHER",
                jwt.getClaimAsString("role")
        );
    }
}
