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
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
// Roll back each test fixture; performance timing and after-commit cleanup are outside this suite.
@Transactional
class StudyClosureIntegrationTest {

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
                        "closure-owner-" + UUID.randomUUID() + "@example.com",
                        "test-password-hash"
                )
        );

        Researcher otherResearcher = researcherRepository.saveAndFlush(
                new Researcher(
                        "closure-other-" + UUID.randomUUID() + "@example.com",
                        "test-password-hash"
                )
        );

        ownerId = owner.getId();
        ownerToken = generateResearcherToken(owner.getId());
        otherToken = generateResearcherToken(otherResearcher.getId());
    }

    /** Verifies that closing a collecting Study persists closure and retains its feed and publication metadata. */
    @Test
    void shouldCloseCollectingStudyAndRetainFeedAndPublicationMetadata() throws Exception {

        // Prepare an owned Study and capture its persisted state.
        Study study = createStudyWithFeed(StudyStatus.COLLECTING);
        String originalFeed = getPersistedFeedRow(study.getId());

        // Exercise the real closure endpoint.
        closeStudy(study.getId(), ownerToken, createVersionRequest(study.getLockVersion()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"))
                .andExpect(jsonPath("$.version").value(study.getLockVersion() + 1))
                .andExpect(jsonPath("$.closedAt").isNotEmpty());

        // Reload the Study and verify its saved lifecycle and retained content.
        Study closedStudy = reloadStudy(study.getId());
        assertEquals(StudyStatus.CLOSED, closedStudy.getStatus());
        assertNotNull(closedStudy.getClosedAt());
        assertEquals(study.getParticipationToken(), closedStudy.getParticipationToken());
        assertEquals(study.getPublishedAt(), closedStudy.getPublishedAt());

        assertEquals(originalFeed, getPersistedFeedRow(study.getId()));
        // A retained publication link must now report permanent closure.
        mockMvc.perform(
                        get("/api/participation/{token}", closedStudy.getParticipationToken())
                )
                .andExpect(status().isGone())
                .andExpect(jsonPath("$.code").value("STUDY_CLOSED"));
    }

    /** Verifies that repeated closure accepts a stale version without changing the saved Study or feed. */
    @Test
    void shouldKeepRepeatedClosureIdempotentWithStaleVersion() throws Exception {

        // Prepare an owned Study and capture its persisted state.
        Study study = createStudyWithFeed(StudyStatus.COLLECTING);
        String originalVersionRequest = createVersionRequest(study.getLockVersion());

        // Close the Study once and capture its persisted result.
        closeStudy(study.getId(), ownerToken, originalVersionRequest)
                .andExpect(status().isOk());

        String closedRow = getPersistedStudyRow(study.getId());
        String originalFeed = getPersistedFeedRow(study.getId());

        // Retry with the original version and verify that closure remains idempotent.
        closeStudy(study.getId(), ownerToken, originalVersionRequest)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
        assertEquals(closedRow, getPersistedStudyRow(study.getId()));

        assertEquals(originalFeed, getPersistedFeedRow(study.getId()));
    }

    /** Verifies that a draft Study cannot be closedStudy and its persisted data remains unchanged. */
    @Test
    void shouldRejectClosingDraftStudyWithoutChangingData() throws Exception {

        // Prepare an owned Study and capture its persisted state.
        Study study = createStudyWithFeed(StudyStatus.DRAFT);
        String originalStudy = getPersistedStudyRow(study.getId());

        // Exercise the real closure endpoint.
        closeStudy(study.getId(), ownerToken, createVersionRequest(study.getLockVersion()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STUDY_NOT_CLOSABLE"));

        // Recheck PostgreSQL after the rejected request.
        assertEquals(originalStudy, getPersistedStudyRow(study.getId()));
    }

    /** Verifies that a closure version conflict preserves the original Study and feed. */
    @Test
    void shouldRejectClosingStudyWithIncorrectVersion() throws Exception {

        // Prepare an owned Study and capture its persisted state.
        Study study = createStudyWithFeed(StudyStatus.COLLECTING);
        String originalStudy = getPersistedStudyRow(study.getId());
        String originalFeed = getPersistedFeedRow(study.getId());

        // Exercise the real closure endpoint.
        closeStudy(study.getId(), ownerToken, createVersionRequest(study.getLockVersion() + 1))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STUDY_VERSION_CONFLICT"));

        // Recheck PostgreSQL after the rejected request.
        assertEquals(originalStudy, getPersistedStudyRow(study.getId()));

        assertEquals(originalFeed, getPersistedFeedRow(study.getId()));
    }

    /** Verifies that an unauthenticated request cannot close a Study. */
    @Test
    void shouldRejectClosingStudyWithoutAuthentication() throws Exception {

        // Prepare an owned Study and capture its persisted state.
        Study study = createStudyWithFeed(StudyStatus.COLLECTING);
        String originalStudy = getPersistedStudyRow(study.getId());

        // Omit researcher authentication when calling the closure endpoint.
        mockMvc.perform(
                        post("/api/studies/{studyId}/close", study.getId())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(createVersionRequest(study.getLockVersion()))
                )
                .andExpect(status().isUnauthorized());

        // Recheck PostgreSQL after the rejected request.
        assertEquals(originalStudy, getPersistedStudyRow(study.getId()));
    }

    /** Verifies that another researcher cannot close the owner's Study. */
    @Test
    void shouldRejectClosingAnotherResearchersStudy() throws Exception {

        // Prepare an owned Study and capture its persisted state.
        Study study = createStudyWithFeed(StudyStatus.COLLECTING);
        String originalStudy = getPersistedStudyRow(study.getId());

        // Exercise the real closure endpoint.
        closeStudy(study.getId(), otherToken, createVersionRequest(study.getLockVersion()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY_NOT_FOUND"));

        // Recheck PostgreSQL after the rejected request.
        assertEquals(originalStudy, getPersistedStudyRow(study.getId()));
    }

    /** Verifies that closing a nonexistent Study returns the standard not-found response. */
    @Test
    void shouldReturnNotFoundWhenClosingMissingStudy() throws Exception {

        // Exercise the real closure endpoint.
        closeStudy(UUID.randomUUID(), ownerToken, "{\"version\":0}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY_NOT_FOUND"));
    }

    /** Verifies that missing, null, negative, string, fractional and overflowing closure versions are rejected. */
    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"version\":null}",
            "{\"version\":-1}",
            "{\"version\":\"0\"}",
            "{\"version\":0.5}",
            "{\"version\":9223372036854775808}"
    })
    void shouldRejectInvalidClosureVersionsWithoutChangingData(String body) throws Exception {

        // Prepare an owned Study and capture its persisted state.
        Study study = createStudyWithFeed(StudyStatus.COLLECTING);
        String originalStudy = getPersistedStudyRow(study.getId());

        // Exercise the real closure endpoint.
        closeStudy(study.getId(), ownerToken, body)
                .andExpect(status().isBadRequest());

        // Recheck PostgreSQL after the rejected request.
        assertEquals(originalStudy, getPersistedStudyRow(study.getId()));
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

    /** Reloads the Study after flushing pending persistence changes. */
    private Study reloadStudy(UUID studyId) {

        entityManager.flush();
        entityManager.clear();

        return studyRepository.findById(studyId)
                .orElseThrow();
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

    /** Sends an authenticated closure request through the real security and controller layers. */
    private ResultActions closeStudy(UUID studyId, String researcherToken, String requestBody)
            throws Exception {

        return mockMvc.perform(
                post("/api/studies/{studyId}/close", studyId)
                        .header("Authorization", "Bearer " + researcherToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
        );
    }

    /** Builds the numeric Study version body required by the closure API. */
    private String createVersionRequest(long version) {

        return "{\"version\":" + version + "}";
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
