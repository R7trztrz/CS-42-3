package com.cs_42_3.surveyplatformbackend.study;

import com.cs_42_3.surveyplatformbackend.feed.domain.StudyFeed;
import com.cs_42_3.surveyplatformbackend.feed.repository.FeedTemplateRepository;
import com.cs_42_3.surveyplatformbackend.feed.repository.StudyFeedRepository;
import com.cs_42_3.surveyplatformbackend.researcher.domain.Researcher;
import com.cs_42_3.surveyplatformbackend.researcher.repository.ResearcherRepository;
import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
// Roll back each test fixture; performance timing and after-commit cleanup are outside this suite.
@Transactional
class StudyDeletionIntegrationTest {

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
    private StudyRepository studyRepository;

    @Autowired
    private StudyFeedRepository studyFeedRepository;

    @Autowired
    private FeedTemplateRepository feedTemplateRepository;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID ownerId;
    private String ownerToken;
    private String otherToken;

    @BeforeEach
    void setUp() {

        Researcher owner = researcherRepository.saveAndFlush(
                new Researcher(
                        "deletion-owner-" + UUID.randomUUID() + "@example.com",
                        "test-password-hash"
                )
        );

        Researcher otherResearcher = researcherRepository.saveAndFlush(
                new Researcher(
                        "deletion-other-" + UUID.randomUUID() + "@example.com",
                        "test-password-hash"
                )
        );

        ownerId = owner.getId();
        ownerToken = generateResearcherToken(owner.getId());
        otherToken = generateResearcherToken(otherResearcher.getId());
    }

    /** Verifies deletion of DRAFT, COLLECTING and CLOSED Studies while retaining other Studies and shared records. */
    @ParameterizedTest
    @EnumSource(StudyStatus.class)
    void shouldDeleteOwnedStudyInEachStateAndPreserveOtherData(StudyStatus state)
            throws Exception {

        // Prepare the deletion target and a separate Study that must be retained.
        Study target = createStudyWithFeed(state);
        Study retained = createStudyWithFeed(StudyStatus.DRAFT);
        String retainedStudyRow = getPersistedStudyRow(retained.getId());
        String retainedFeedRow = getPersistedFeedRow(retained.getId());

        long researcherCount = researcherRepository.count();
        long templateCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM feed_templates",
                Long.class
        );

        // Exercise the real deletion endpoint.
        deleteStudy(target, ownerToken, target.getLockVersion().toString())
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));

        // Clear cached entities before checking deletion and ownership isolation.
        entityManager.clear();

        assertFalse(studyRepository.existsById(target.getId()));
        assertFalse(studyFeedRepository.existsById(target.getId()));
        assertEquals(retainedStudyRow, getPersistedStudyRow(retained.getId()));
        assertEquals(retainedFeedRow, getPersistedFeedRow(retained.getId()));
        assertEquals(researcherCount, researcherRepository.count());
        Long remainingTemplateCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM feed_templates",
                Long.class
        );

        assertNotNull(remainingTemplateCount);
        assertEquals(templateCount, remainingTemplateCount.longValue());

        // The owner must no longer be able to retrieve deleted Study details.
        mockMvc.perform(
                        get("/api/studies/{studyId}", target.getId())
                                .header("Authorization", "Bearer " + ownerToken)
                )
                .andExpect(status().isNotFound());

        // Deleted publication links must no longer resolve the Study.
        if (target.getParticipationToken() != null) {
            mockMvc.perform(
                            get("/api/participation/{token}", target.getParticipationToken())
                    )
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("PARTICIPATION_NOT_FOUND"));
        }
    }

    /** Verifies that a deletion version conflict preserves the original Study and feed. */
    @Test
    void shouldRejectDeletingStudyWithIncorrectVersion() throws Exception {

        // Prepare an owned Study and capture its persisted state.
        Study study = createStudyWithFeed(StudyStatus.COLLECTING);
        String originalStudy = getPersistedStudyRow(study.getId());
        String originalFeed = getPersistedFeedRow(study.getId());

        // Exercise the real deletion endpoint.
        deleteStudy(study, ownerToken, Long.toString(study.getLockVersion() + 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STUDY_VERSION_CONFLICT"));

        // Recheck PostgreSQL after the rejected request.
        assertEquals(originalStudy, getPersistedStudyRow(study.getId()));

        assertEquals(originalFeed, getPersistedFeedRow(study.getId()));
    }

    /** Verifies that an unauthenticated request cannot delete a Study or its feed. */
    @Test
    void shouldRejectDeletingStudyWithoutAuthentication() throws Exception {

        // Prepare an owned Study and capture its persisted state.
        Study study = createStudyWithFeed(StudyStatus.DRAFT);
        String originalStudy = getPersistedStudyRow(study.getId());

        // Omit researcher authentication when calling the deletion endpoint.
        mockMvc.perform(
                        delete("/api/studies/{studyId}", study.getId())
                                .param("version", study.getLockVersion().toString())
                )
                .andExpect(status().isUnauthorized());

        // Recheck PostgreSQL after the rejected request.
        assertEquals(originalStudy, getPersistedStudyRow(study.getId()));
        assertTrue(studyFeedRepository.existsById(study.getId()));
    }

    /** Verifies that another researcher cannot delete the owner's Study or its feed. */
    @Test
    void shouldRejectDeletingAnotherResearchersStudy() throws Exception {

        // Prepare an owned Study and capture its persisted state.
        Study study = createStudyWithFeed(StudyStatus.DRAFT);
        String originalStudy = getPersistedStudyRow(study.getId());
        String originalFeed = getPersistedFeedRow(study.getId());

        // Exercise the real deletion endpoint.
        deleteStudy(study, otherToken, study.getLockVersion().toString())
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY_NOT_FOUND"));

        // Recheck PostgreSQL after the rejected request.
        assertEquals(originalStudy, getPersistedStudyRow(study.getId()));

        assertEquals(originalFeed, getPersistedFeedRow(study.getId()));
    }

    /** Verifies that deleting a nonexistent Study returns the standard not-found response. */
    @Test
    void shouldReturnNotFoundWhenDeletingMissingStudy() throws Exception {

        // Use a valid researcher token with a nonexistent Study ID.
        mockMvc.perform(
                        delete("/api/studies/{studyId}", UUID.randomUUID())
                                .header("Authorization", "Bearer " + ownerToken)
                                .param("version", "0")
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY_NOT_FOUND"));
    }

    /** Verifies that missing, negative, fractional, nonnumeric and overflowing deletion versions are rejected. */
    @ParameterizedTest
    @ValueSource(strings = {"", "-1", "0.5", "abc", "9223372036854775808"})
    void shouldRejectInvalidDeletionVersionsWithoutChangingData(String version) throws Exception {

        // Prepare an owned Study and capture its persisted state.
        Study study = createStudyWithFeed(StudyStatus.DRAFT);
        String originalStudy = getPersistedStudyRow(study.getId());
        String originalFeed = getPersistedFeedRow(study.getId());

        // Exercise the real deletion endpoint.
        deleteStudy(study, ownerToken, version)
                .andExpect(status().isBadRequest());

        // Recheck PostgreSQL after the rejected request.
        assertEquals(originalStudy, getPersistedStudyRow(study.getId()));

        assertEquals(originalFeed, getPersistedFeedRow(study.getId()));
    }

    /** Verifies that repeating a successful deletion returns not found and does not recreate data. */
    @Test
    void shouldReturnNotFoundWhenDeletingStudyAgain() throws Exception {

        // Prepare an owned Study and capture its persisted state.
        Study study = createStudyWithFeed(StudyStatus.DRAFT);
        String version = study.getLockVersion().toString();

        // Exercise the real deletion endpoint.
        deleteStudy(study, ownerToken, version)
                .andExpect(status().isNoContent());

        // A second deletion must not report success or recreate the deleted records.
        deleteStudy(study, ownerToken, version)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY_NOT_FOUND"));

        // Clear cached entities before checking deletion and ownership isolation.
        entityManager.clear();

        assertFalse(studyRepository.existsById(study.getId()));
        assertFalse(studyFeedRepository.existsById(study.getId()));
    }

    /** Creates persisted lifecycle fixtures without testing the publication endpoint again. */
    private Study createStudyWithFeed(StudyStatus state) {

        Study study = new Study(
                ownerId,
                "Lifecycle " + state,
                "Closure and deletion regression fixture"
        );

        // Seed publication metadata for collecting and closed fixtures.
        if (state != StudyStatus.DRAFT) {

            String participationToken =
                    UUID.randomUUID().toString().replace("-", "") + "abcdefghijk";

            study.publish(
                    participationToken,
                    Instant.now(),
                    "platform-default-v1"
            );
        }

        if (state == StudyStatus.CLOSED) {
            study.close(Instant.now());
        }

        study = studyRepository.saveAndFlush(study);

        // Create an independent feed belonging only to this Study.
        var template = feedTemplateRepository.findById("blank")
                .orElseThrow();

        StudyFeed feed = new StudyFeed(study.getId(), template);

        feed.replaceContent("""
                {
                  "ROOT": {
                    "type": "div"
                  }
                }
                """);

        studyFeedRepository.saveAndFlush(feed);

        // Return a detached database snapshot so later API calls cannot alter the baseline object.
        UUID studyId = study.getId();
        entityManager.clear();

        Study persistedStudy = studyRepository.findById(studyId)
                .orElseThrow();

        entityManager.detach(persistedStudy);

        return persistedStudy;
    }

    /** Captures all persisted Study columns for unchanged-state assertions. */
    private String getPersistedStudyRow(UUID studyId) {

        entityManager.flush();

        return jdbcTemplate.queryForObject(
                "SELECT row_to_json(s)::text FROM studies s WHERE id = ?",
                String.class,
                studyId
        );
    }

    /** Captures all persisted feed columns, including content, timestamps and version. */
    private String getPersistedFeedRow(UUID studyId) {

        entityManager.flush();

        return jdbcTemplate.queryForObject(
                "SELECT row_to_json(f)::text FROM study_feeds f WHERE study_id = ?",
                String.class,
                studyId
        );
    }

    /** Omits the version parameter when the supplied value is empty. */
    private ResultActions deleteStudy(Study study, String researcherToken, String version)
            throws Exception {

        var request = delete("/api/studies/{studyId}", study.getId())
                .header("Authorization", "Bearer " + researcherToken);

        if (!version.isEmpty()) {
            request.param("version", version);
        }

        return mockMvc.perform(request);
    }

    /** Generates researcher authentication in the same way as the existing integration tests. */
    private String generateResearcherToken(UUID researcherId) {

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(researcherId.toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("userId", researcherId.toString())
                .claim("role", "RESEARCHER")
                .build();

        return jwtEncoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
    }}
