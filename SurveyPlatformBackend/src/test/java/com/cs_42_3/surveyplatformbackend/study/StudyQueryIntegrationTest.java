
package com.cs_42_3.surveyplatformbackend.study;

import com.cs_42_3.surveyplatformbackend.researcher.domain.Researcher;
import com.cs_42_3.surveyplatformbackend.researcher.repository.ResearcherRepository;
import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StudyQueryIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("survey_platform_test")
                    .withUsername("test")
                    .withPassword("test");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {

        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);

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
    private JwtEncoder jwtEncoder;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Researcher researcher;
    private String researcherToken;

    @BeforeEach
    void setUp() {

        researcher = researcherRepository.saveAndFlush(
                new Researcher(
                        "study-query-test@example.com",
                        "test-password-hash"
                )
        );

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(researcher.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("userId", researcher.getId().toString())
                .claim("role", "RESEARCHER")
                .build();

        researcherToken = jwtEncoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
    }

    /** Verifies that a researcher with no studies receives an empty paginated list. */
    @Test
    void shouldReturnEmptyStudyList() throws Exception {

        mockMvc.perform(
                        get("/api/studies")
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(0))
                .andExpect(jsonPath("$.totalPages").value(0));
    }

    /** Verifies that a researcher can retrieve their own study details. */
    @Test
    void shouldReturnOwnedStudyDetails() throws Exception {

        // Create a study owned by the authenticated researcher.
        Study savedStudy = studyRepository.saveAndFlush(
                new Study(
                        researcher.getId(),
                        "Study Detail Test",
                        "Verify study detail response"
                )
        );

        mockMvc.perform(
                        get("/api/studies/{studyId}", savedStudy.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(savedStudy.getId().toString()))
                .andExpect(jsonPath("$.title")
                        .value("Study Detail Test"))
                .andExpect(jsonPath("$.description")
                        .value("Verify study detail response"))
                .andExpect(jsonPath("$.status")
                        .value("DRAFT"))
                .andExpect(jsonPath("$.version")
                        .value(savedStudy.getLockVersion().intValue()))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty())
                .andExpect(jsonPath("$.eyeTrackingEnabled")
                        .value(false))
                .andExpect(jsonPath("$.questionnaireEnabled")
                        .value(false));
    }

    /** Verifies that invalid study pagination parameters are rejected. */
    @Test
    void shouldRejectInvalidStudyPagination() throws Exception {

        String[][] invalidParameters = {
                {"-1", "20"},
                {"0", "0"},
                {"0", "101"}
        };

        for (String[] parameters : invalidParameters) {

            mockMvc.perform(
                            get("/api/studies")
                                    .header(
                                            "Authorization",
                                            "Bearer " + researcherToken
                                    )
                                    .param("page", parameters[0])
                                    .param("size", parameters[1])
                    )
                    .andExpect(status().isBadRequest());
        }
    }

    /** Verifies that study pagination returns the correct records ordered by latest update. */
    @Test
    void shouldReturnPaginatedStudiesOrderedByUpdatedAt() throws Exception {

        Study oldestStudy = studyRepository.saveAndFlush(
                new Study(researcher.getId(), "Oldest Study", "First study")
        );

        Study middleStudy = studyRepository.saveAndFlush(
                new Study(researcher.getId(), "Middle Study", "Second study")
        );

        Study newestStudy = studyRepository.saveAndFlush(
                new Study(researcher.getId(), "Newest Study", "Third study")
        );


        // Assign fixed timestamps to make the ordering deterministic.
        jdbcTemplate.update(
                "UPDATE studies SET updated_at = ? WHERE id = ?",
                Timestamp.from(Instant.parse("2026-01-01T10:00:00Z")),
                oldestStudy.getId()
        );

        jdbcTemplate.update(
                "UPDATE studies SET updated_at = ? WHERE id = ?",
                Timestamp.from(Instant.parse("2026-01-02T10:00:00Z")),
                middleStudy.getId()
        );

        jdbcTemplate.update(
                "UPDATE studies SET updated_at = ? WHERE id = ?",
                Timestamp.from(Instant.parse("2026-01-03T10:00:00Z")),
                newestStudy.getId()
        );


        // Page 0 should return the two most recently updated studies.
        mockMvc.perform(
                        get("/api/studies")
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .param("page", "0")
                                .param("size", "2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.content[0].id")
                        .value(newestStudy.getId().toString()))
                .andExpect(jsonPath("$.content[1].id")
                        .value(middleStudy.getId().toString()));

        // Page 1 should contain only the oldest study.
        mockMvc.perform(
                        get("/api/studies")
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .param("page", "1")
                                .param("size", "2")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalElements").value(3))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id")
                        .value(oldestStudy.getId().toString()));
    }

}