package com.cs_42_3.surveyplatformbackend.researcher;

import com.cs_42_3.surveyplatformbackend.researcher.domain.Researcher;
import com.cs_42_3.surveyplatformbackend.researcher.repository.ResearcherRepository;

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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
}
