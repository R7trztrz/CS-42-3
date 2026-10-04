
package com.cs_42_3.surveyplatformbackend.study;

import com.cs_42_3.surveyplatformbackend.researcher.domain.Researcher;
import com.cs_42_3.surveyplatformbackend.researcher.repository.ResearcherRepository;
import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class StudyListPerformanceIntegrationTest {

    private static final int STUDY_COUNT = 1000;
    private static final int FOREIGN_STUDY_COUNT = 50;
    private static final int PAGE_SIZE = 100;

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
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ResearcherRepository researcherRepository;

    @Autowired
    private JwtEncoder jwtEncoder;

    private Researcher owner;
    private Researcher otherResearcher;
    private String ownerToken;

    @BeforeEach
    void setUp() {

        owner = researcherRepository.saveAndFlush(
                new Researcher(
                        "study-perf-owner-" + UUID.randomUUID() + "@example.com",
                        "test-password-hash"
                )
        );

        otherResearcher = researcherRepository.saveAndFlush(
                new Researcher(
                        "study-perf-other-" + UUID.randomUUID() + "@example.com",
                        "test-password-hash"
                )
        );

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(owner.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("userId", owner.getId().toString())
                .claim("role", "RESEARCHER")
                .build();

        ownerToken = jwtEncoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
    }

    @AfterEach
    void cleanUp() {

        // Remove only records generated for this performance test.
        if (owner != null) {
            jdbcTemplate.update(
                    "DELETE FROM studies WHERE owner_id = ?",
                    owner.getId()
            );

            researcherRepository.deleteById(owner.getId());
        }

        if (otherResearcher != null) {
            jdbcTemplate.update(
                    "DELETE FROM studies WHERE owner_id = ?",
                    otherResearcher.getId()
            );

            researcherRepository.deleteById(otherResearcher.getId());
        }
    }

    /** Verifies thousand-record pagination, ownership isolation and records a query performance baseline. */
    @Test
    void shouldPaginateThousandStudiesAndRecordPerformanceBaseline() throws Exception {

        // Prepare deterministic timestamps and expected Study IDs.
        Instant baseTime = Instant.parse("2026-01-01T00:00:00Z");

        List<UUID> expectedOwnerIds = new ArrayList<>();
        List<UUID> expectedForeignIds = new ArrayList<>();

        List<Object[]> batchRows = new ArrayList<>();

        // Seed 1,000 Studies belonging to researcher A.
        for (int i = 0; i < STUDY_COUNT; i++) {

            UUID studyId = UUID.randomUUID();
            expectedOwnerIds.add(studyId);

            Timestamp timestamp = Timestamp.from(
                    baseTime.plusSeconds(i)
            );

            batchRows.add(new Object[]{
                    studyId,
                    owner.getId(),
                    "Performance Study " + i,
                    "ST-049 pagination verification",
                    timestamp,
                    timestamp
            });
        }

        // Seed another 50 Studies belonging to researcher B.
        for (int i = 0; i < FOREIGN_STUDY_COUNT; i++) {

            UUID studyId = UUID.randomUUID();
            expectedForeignIds.add(studyId);

            Timestamp timestamp = Timestamp.from(
                    baseTime.plusSeconds(i)
            );

            batchRows.add(new Object[]{
                    studyId,
                    otherResearcher.getId(),
                    "Foreign Study " + i,
                    "Ownership isolation test data",
                    timestamp,
                    timestamp
            });
        }

        // Batch insert is used so data preparation is not part of the timing measurement.
        jdbcTemplate.batchUpdate("""
                INSERT INTO studies (
                    id,
                    owner_id,
                    title,
                    description,
                    status,
                    created_at,
                    updated_at,
                    lock_version
                )
                VALUES (?, ?, ?, ?, 'DRAFT', ?, ?, 0)
                """, batchRows);

        // Confirm all 1,050 test records have been inserted.
        Integer seededCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM studies
                WHERE owner_id IN (?, ?)
                """,
                Integer.class,
                owner.getId(),
                otherResearcher.getId()
        );

        assertEquals(1050, seededCount);

        // Warm up the application path before recording timing samples.
        mockMvc.perform(
                        get("/api/studies")
                                .header("Authorization", "Bearer " + ownerToken)
                                .param("page", "0")
                                .param("size", String.valueOf(PAGE_SIZE))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(STUDY_COUNT));

        Set<String> returnedIds = new HashSet<>();

        double totalMilliseconds = 0;
        double maximumMilliseconds = 0;

        // Query every page and verify the entire 1,000-record result set.
        for (int page = 0; page < 10; page++) {

            long start = System.nanoTime();

            MvcResult result = mockMvc.perform(
                            get("/api/studies")
                                    .header(
                                            "Authorization",
                                            "Bearer " + ownerToken
                                    )
                                    .param("page", String.valueOf(page))
                                    .param("size", String.valueOf(PAGE_SIZE))
                    )
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.page").value(page))
                    .andExpect(jsonPath("$.size").value(PAGE_SIZE))
                    .andExpect(jsonPath("$.totalElements").value(STUDY_COUNT))
                    .andExpect(jsonPath("$.totalPages").value(10))
                    .andExpect(jsonPath("$.content.length()").value(PAGE_SIZE))
                    .andReturn();

            long finish = System.nanoTime();

            double elapsedMilliseconds =
                    (finish - start) / 1_000_000.0;

            totalMilliseconds += elapsedMilliseconds;

            maximumMilliseconds = Math.max(
                    maximumMilliseconds,
                    elapsedMilliseconds
            );

            List<String> pageIds = JsonPath.read(
                    result.getResponse().getContentAsString(),
                    "$.content[*].id"
            );

            assertEquals(PAGE_SIZE, pageIds.size());

            for (int index = 0; index < PAGE_SIZE; index++) {

                String actualId = pageIds.get(index);

                // Higher timestamps must appear first.
                int expectedIndex =
                        STUDY_COUNT - 1 - (page * PAGE_SIZE + index);

                assertEquals(
                        expectedOwnerIds.get(expectedIndex).toString(),
                        actualId,
                        "Incorrect Study ordering on page " + page
                );

                // No Study may appear more than once across pages.
                assertTrue(
                        returnedIds.add(actualId),
                        "Duplicate Study found across pages: " + actualId
                );
            }
        }

        // Every one of the researcher's Studies must have been returned.
        assertEquals(STUDY_COUNT, returnedIds.size());

        Set<String> expectedIds = new HashSet<>();

        for (UUID id : expectedOwnerIds) {
            expectedIds.add(id.toString());
        }

        assertEquals(expectedIds, returnedIds);

        // The page immediately after the last valid page must be empty.
        mockMvc.perform(
                        get("/api/studies")
                                .header("Authorization", "Bearer " + ownerToken)
                                .param("page", "10")
                                .param("size", String.valueOf(PAGE_SIZE))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(STUDY_COUNT))
                .andExpect(jsonPath("$.totalPages").value(10));

        // Researcher B's 50 Studies must not appear in researcher A's results.
        for (UUID foreignId : expectedForeignIds) {
            assertFalse(
                    returnedIds.contains(foreignId.toString()),
                    "Foreign-owned Study leaked into the response"
            );
        }

        double averageMilliseconds = totalMilliseconds / 10.0;

        // Report observed values without imposing an unapproved performance threshold.
        System.out.printf(
                Locale.ROOT,
                """
                
                ========= ST-049 PERFORMANCE BASELINE =========
                Owned Studies:       %d
                Foreign Studies:     %d
                Page Size:           %d
                Measured Requests:   10
                Average Time:        %.2f ms
                Maximum Time:        %.2f ms
                Unique IDs Returned: %d
                Result:              Pagination and isolation verified
                ===============================================
                
                """,
                STUDY_COUNT,
                FOREIGN_STUDY_COUNT,
                PAGE_SIZE,
                averageMilliseconds,
                maximumMilliseconds,
                returnedIds.size()
        );
    }
}
