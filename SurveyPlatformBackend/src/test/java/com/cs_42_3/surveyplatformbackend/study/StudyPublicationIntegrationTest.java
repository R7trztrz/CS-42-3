package com.cs_42_3.surveyplatformbackend.study;

import com.cs_42_3.surveyplatformbackend.asset.repository.StudyAssetRepository;
import com.cs_42_3.surveyplatformbackend.feed.domain.StudyFeed;
import com.cs_42_3.surveyplatformbackend.feed.repository.FeedTemplateRepository;
import com.cs_42_3.surveyplatformbackend.feed.repository.StudyFeedRepository;
import com.cs_42_3.surveyplatformbackend.researcher.domain.Researcher;
import com.cs_42_3.surveyplatformbackend.researcher.repository.ResearcherRepository;
import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.jayway.jsonpath.JsonPath;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StudyPublicationIntegrationTest {

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
    private StudyAssetRepository studyAssetRepository;

    private Researcher researcher;
    private String researcherToken;

    @BeforeEach
    void setUp() {

        researcher = researcherRepository.saveAndFlush(
                new Researcher(
                        "study-publication-test@example.com",
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

    /** Verifies that publishing a ready draft persists its collecting state and participation link. */
    @Test
    void shouldPublishDraftStudySuccessfully() throws Exception {

        // Create an owned draft study.
        Study study = studyRepository.saveAndFlush(
                new Study(
                        researcher.getId(),
                        "Publication Test Study",
                        "Study publication integration test"
                )
        );

        UUID studyId = study.getId();
        long originalVersion = study.getLockVersion();

        // Initialize an independent feed from the blank system template.
        var template = feedTemplateRepository.findById("blank")
                .orElseThrow();

        StudyFeed feed = new StudyFeed(studyId, template);

        // Current publication logic requires a nonempty JSON object.
        feed.replaceContent("""
                {
                  "ROOT": {
                    "type": "div"
                  }
                }
                """);

        studyFeedRepository.saveAndFlush(feed);

        // Clear cached entities before exercising the HTTP endpoint.
        entityManager.clear();

        String requestBody = """
                {
                  "version": %d
                }
                """.formatted(originalVersion);

        // Publish the study through the real HTTP endpoint.
        String responseBody = mockMvc.perform(
                        post("/api/studies/{studyId}/publish", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id")
                        .value(studyId.toString()))
                .andExpect(jsonPath("$.title")
                        .value("Publication Test Study"))
                .andExpect(jsonPath("$.status")
                        .value("COLLECTING"))
                .andExpect(jsonPath("$.publishedAt").isNotEmpty())
                .andExpect(jsonPath("$.participationUrl").isNotEmpty())
                .andExpect(jsonPath("$.version")
                        .value(originalVersion + 1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Reload the study from PostgreSQL.
        entityManager.clear();

        Study publishedStudy = studyRepository.findById(studyId)
                .orElseThrow();

        assertEquals(
                StudyStatus.COLLECTING,
                publishedStudy.getStatus()
        );

        assertEquals(
                researcher.getId(),
                publishedStudy.getOwnerId()
        );

        assertEquals(
                "Publication Test Study",
                publishedStudy.getTitle()
        );

        assertNotNull(publishedStudy.getPublishedAt());
        assertNotNull(publishedStudy.getParticipationToken());

        assertEquals(
                43,
                publishedStudy.getParticipationToken().length()
        );

        assertEquals(
                originalVersion + 1,
                publishedStudy.getLockVersion()
        );

        // Verify that the returned link contains the persisted token.
        String participationUrl =
                JsonPath.read(responseBody, "$.participationUrl");

        assertTrue(
                participationUrl.endsWith(
                        "/participate/" + publishedStudy.getParticipationToken()
                )
        );

        // Publishing must not remove the study's feed.
        assertTrue(studyFeedRepository.existsById(studyId));
    }


    /** Verifies that publishing a study with an empty feed is rejected without changing its state. */
    @Test
    void shouldRejectPublicationWhenFeedContentIsNull() throws Exception {

        // Create an owned draft study.
        Study study = studyRepository.saveAndFlush(
                new Study(
                        researcher.getId(),
                        "Empty Feed Study",
                        "Publication should fail without feed content"
                )
        );

        UUID studyId = study.getId();
        long originalVersion = study.getLockVersion();

        // Initialize a feed from the blank template without adding content.
        var template = feedTemplateRepository.findById("blank")
                .orElseThrow();

        StudyFeed feed = new StudyFeed(studyId, template);

        studyFeedRepository.saveAndFlush(feed);

        // Verify that the feed has no content before publication.
        assertNull(feed.getContent());

        entityManager.clear();

        String requestBody = """
            {
              "version": %d
            }
            """.formatted(originalVersion);

        // Attempt to publish the study.
        mockMvc.perform(
                        post("/api/studies/{studyId}/publish", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("FEED_NOT_READY"));

        // Reload from PostgreSQL and verify that publication did not occur.
        entityManager.clear();

        Study unchangedStudy = studyRepository.findById(studyId)
                .orElseThrow();

        assertEquals(
                StudyStatus.DRAFT,
                unchangedStudy.getStatus()
        );

        assertNull(unchangedStudy.getPublishedAt());
        assertNull(unchangedStudy.getParticipationToken());

        assertEquals(
                originalVersion,
                unchangedStudy.getLockVersion()
        );

        // The feed should still exist, with its content unchanged.
        StudyFeed unchangedFeed = studyFeedRepository.findById(studyId)
                .orElseThrow();

        assertNull(unchangedFeed.getContent());
    }

    /** Verifies that publishing with a stale version is rejected without changing the study. */
    @Test
    void shouldRejectPublicationWithStaleVersion() throws Exception {

        // Create an owned draft study.
        Study study = studyRepository.saveAndFlush(
                new Study(
                        researcher.getId(),
                        "Original Study",
                        "Publication version conflict test"
                )
        );

        UUID studyId = study.getId();
        long originalVersion = study.getLockVersion();

        // Prepare a valid nonempty feed.
        var template = feedTemplateRepository.findById("blank")
                .orElseThrow();

        StudyFeed feed = new StudyFeed(studyId, template);

        feed.replaceContent("""
            {
              "ROOT": {
                "type": "div"
              }
            }
            """);

        studyFeedRepository.saveAndFlush(feed);

        entityManager.clear();

        // Successfully update the study, incrementing its version.
        String updateRequest = """
            {
              "version": %d,
              "title": "Updated Study"
            }
            """.formatted(originalVersion);

        mockMvc.perform(
                        patch("/api/studies/{studyId}", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateRequest)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Study"))
                .andExpect(jsonPath("$.version")
                        .value(originalVersion + 1));

        entityManager.clear();

        // Attempt publication using the outdated version.
        String publishRequest = """
            {
              "version": %d
            }
            """.formatted(originalVersion);

        mockMvc.perform(
                        post("/api/studies/{studyId}/publish", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(publishRequest)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("STUDY_VERSION_CONFLICT"));

        // Verify that the rejected publication changed nothing.
        entityManager.clear();

        Study unchangedStudy = studyRepository.findById(studyId)
                .orElseThrow();

        assertEquals("Updated Study", unchangedStudy.getTitle());

        assertEquals(
                StudyStatus.DRAFT,
                unchangedStudy.getStatus()
        );

        assertNull(unchangedStudy.getPublishedAt());
        assertNull(unchangedStudy.getParticipationToken());

        assertEquals(
                originalVersion + 1,
                unchangedStudy.getLockVersion()
        );

        assertTrue(studyFeedRepository.existsById(studyId));
    }

    /** Verifies that an already published study cannot be published again. */
    @Test
    void shouldRejectDuplicateStudyPublication() throws Exception {

        // Create an owned draft study.
        Study study = studyRepository.saveAndFlush(
                new Study(
                        researcher.getId(),
                        "Duplicate Publication Study",
                        "Verify that publication is allowed only once"
                )
        );

        UUID studyId = study.getId();
        long originalVersion = study.getLockVersion();

        // Prepare a valid nonempty feed.
        var template = feedTemplateRepository.findById("blank")
                .orElseThrow();

        StudyFeed feed = new StudyFeed(studyId, template);

        feed.replaceContent("""
            {
              "ROOT": {
                "type": "div"
              }
            }
            """);

        studyFeedRepository.saveAndFlush(feed);

        entityManager.clear();

        // First publication should succeed.
        String firstRequest = """
            {
              "version": %d
            }
            """.formatted(originalVersion);

        mockMvc.perform(
                        post("/api/studies/{studyId}/publish", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(firstRequest)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COLLECTING"))
                .andExpect(jsonPath("$.version")
                        .value(originalVersion + 1));

        // Capture the published state from PostgreSQL.
        entityManager.clear();

        Study publishedStudy = studyRepository.findById(studyId)
                .orElseThrow();

        long publishedVersion = publishedStudy.getLockVersion();

        String originalToken = publishedStudy.getParticipationToken();
        Instant originalPublishedAt = publishedStudy.getPublishedAt();

        assertNotNull(originalToken);
        assertNotNull(originalPublishedAt);

        // Second publication uses the CURRENT version, not a stale one.
        String secondRequest = """
            {
              "version": %d
            }
            """.formatted(publishedVersion);

        mockMvc.perform(
                        post("/api/studies/{studyId}/publish", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(secondRequest)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("STUDY_NOT_PUBLISHABLE"));

        // Verify that the duplicate request did not change publication data.
        entityManager.clear();

        Study unchangedStudy = studyRepository.findById(studyId)
                .orElseThrow();

        assertEquals(
                StudyStatus.COLLECTING,
                unchangedStudy.getStatus()
        );

        assertEquals(
                originalToken,
                unchangedStudy.getParticipationToken()
        );

        assertEquals(
                originalPublishedAt,
                unchangedStudy.getPublishedAt()
        );

        assertEquals(
                publishedVersion,
                unchangedStudy.getLockVersion()
        );
    }

    /** Verifies that a researcher cannot publish another owner's study or discover its existence. */
    @Test
    void shouldRejectPublicationOfAnotherResearchersStudy() throws Exception {

        // Create another researcher (B).
        Researcher otherResearcher = researcherRepository.saveAndFlush(
                new Researcher(
                        "other-publication-test@example.com",
                        "test-password-hash"
                )
        );

        // Create a study owned by B, not the authenticated researcher A.
        Study otherStudy = studyRepository.saveAndFlush(
                new Study(
                        otherResearcher.getId(),
                        "Other Researcher's Study",
                        "This study belongs to another researcher"
                )
        );

        UUID studyId = otherStudy.getId();
        long originalVersion = otherStudy.getLockVersion();

        entityManager.clear();

        String requestBody = """
            {
              "version": %d
            }
            """.formatted(originalVersion);

        // Researcher A attempts to publish B's study.
        var foreignResponse = mockMvc.perform(
                        post("/api/studies/{studyId}/publish", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("STUDY_NOT_FOUND"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Compare with a study ID that genuinely does not exist.
        UUID nonexistentStudyId = UUID.randomUUID();

        assertFalse(studyRepository.existsById(nonexistentStudyId));

        var missingResponse = mockMvc.perform(
                        post("/api/studies/{studyId}/publish", nonexistentStudyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("STUDY_NOT_FOUND"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // The API must not disclose whether the foreign study exists.
        assertEquals(foreignResponse, missingResponse);

        // Verify that B's study was not modified.
        entityManager.clear();

        Study unchangedStudy = studyRepository.findById(studyId)
                .orElseThrow();

        assertEquals(
                otherResearcher.getId(),
                unchangedStudy.getOwnerId()
        );

        assertEquals(
                StudyStatus.DRAFT,
                unchangedStudy.getStatus()
        );

        assertNull(unchangedStudy.getPublishedAt());
        assertNull(unchangedStudy.getParticipationToken());

        assertEquals(
                originalVersion,
                unchangedStudy.getLockVersion()
        );
    }

    /** Verifies that invalid publication versions are rejected without changing the study. */
    @Test
    void shouldRejectPublicationWithInvalidVersion() throws Exception {

        Study study = studyRepository.saveAndFlush(
                new Study(
                        researcher.getId(),
                        "Invalid Publication Version Study",
                        "Publication request validation test"
                )
        );

        UUID studyId = study.getId();
        long originalVersion = study.getLockVersion();

        entityManager.clear();

        String[] invalidRequests = {
                "{}",
                "{\"version\": -1}",
                "{\"version\": 1.5}",
                "{\"version\": \"0\"}"
        };

        // Every invalid version must be rejected before publication.
        for (String requestBody : invalidRequests) {

            mockMvc.perform(
                            post("/api/studies/{studyId}/publish", studyId)
                                    .header(
                                            "Authorization",
                                            "Bearer " + researcherToken
                                    )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(requestBody)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code")
                            .value("REQUEST_VALIDATION_FAILED"));
        }

        // Verify that none of the invalid requests changed the database.
        entityManager.clear();

        Study unchangedStudy = studyRepository.findById(studyId)
                .orElseThrow();

        assertEquals(
                StudyStatus.DRAFT,
                unchangedStudy.getStatus()
        );

        assertNull(unchangedStudy.getPublishedAt());
        assertNull(unchangedStudy.getParticipationToken());

        assertEquals(
                originalVersion,
                unchangedStudy.getLockVersion()
        );
    }

    /** Verifies that an empty JSON feed object cannot be published. */
    @Test
    void shouldRejectPublicationWithEmptyJsonObject() throws Exception {

        // Create an owned draft study.
        Study study = studyRepository.saveAndFlush(
                new Study(
                        researcher.getId(),
                        "Empty JSON Feed Study",
                        "Publication should fail with an empty JSON object"
                )
        );

        UUID studyId = study.getId();
        long originalVersion = study.getLockVersion();

        // Initialize a feed containing an empty JSON object.
        var template = feedTemplateRepository.findById("blank")
                .orElseThrow();

        StudyFeed feed = new StudyFeed(studyId, template);

        feed.replaceContent("{}");

        studyFeedRepository.saveAndFlush(feed);

        entityManager.clear();

        String requestBody = """
            {
              "version": %d
            }
            """.formatted(originalVersion);

        // Attempt to publish the study.
        mockMvc.perform(
                        post("/api/studies/{studyId}/publish", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("FEED_NOT_READY"));

        // Verify that the failed publication changed nothing.
        entityManager.clear();

        Study unchangedStudy = studyRepository.findById(studyId)
                .orElseThrow();

        assertEquals(
                StudyStatus.DRAFT,
                unchangedStudy.getStatus()
        );

        assertNull(unchangedStudy.getPublishedAt());
        assertNull(unchangedStudy.getParticipationToken());

        assertEquals(
                originalVersion,
                unchangedStudy.getLockVersion()
        );

        // The feed should remain unchanged.
        StudyFeed unchangedFeed = studyFeedRepository.findById(studyId)
                .orElseThrow();

        assertEquals("{}", unchangedFeed.getContent());
    }

    /** Verifies that a nonempty JSON array cannot be published as a feed document. */
    @Test
    void shouldRejectPublicationWithNonObjectJson() throws Exception {

        // Create an owned draft study.
        Study study = studyRepository.saveAndFlush(
                new Study(
                        researcher.getId(),
                        "JSON Array Feed Study",
                        "Publication should reject non-object JSON"
                )
        );

        UUID studyId = study.getId();
        long originalVersion = study.getLockVersion();

        // Initialize a feed with valid, nonempty JSON that is not an object.
        var template = feedTemplateRepository.findById("blank")
                .orElseThrow();

        StudyFeed feed = new StudyFeed(studyId, template);

        feed.replaceContent("""
            [
              {
                "type": "div"
              }
            ]
            """);

        studyFeedRepository.saveAndFlush(feed);

        entityManager.clear();

        String requestBody = """
            {
              "version": %d
            }
            """.formatted(originalVersion);

        // Publication must reject JSON arrays.
        mockMvc.perform(
                        post("/api/studies/{studyId}/publish", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("FEED_NOT_READY"));

        // Reload from PostgreSQL and verify that publication did not occur.
        entityManager.clear();

        Study unchangedStudy = studyRepository.findById(studyId)
                .orElseThrow();

        assertEquals(
                StudyStatus.DRAFT,
                unchangedStudy.getStatus()
        );

        assertNull(unchangedStudy.getPublishedAt());
        assertNull(unchangedStudy.getParticipationToken());

        assertEquals(
                originalVersion,
                unchangedStudy.getLockVersion()
        );

        // The original feed must still exist.
        StudyFeed unchangedFeed = studyFeedRepository.findById(studyId)
                .orElseThrow();

        assertNotNull(unchangedFeed.getContent());
        assertTrue(unchangedFeed.getContent().trim().startsWith("["));
    }

    /** Verifies that publication fails safely when the study has no associated feed record. */
    @Test
    void shouldRejectPublicationWhenFeedRecordIsMissing() throws Exception {

        // Create an owned draft study without initializing a feed.
        Study study = studyRepository.saveAndFlush(
                new Study(
                        researcher.getId(),
                        "Missing Feed Study",
                        "Publication should fail when the feed record is missing"
                )
        );

        UUID studyId = study.getId();
        long originalVersion = study.getLockVersion();

        // Confirm that no corresponding study feed exists.
        assertFalse(studyFeedRepository.existsById(studyId));

        entityManager.clear();

        String requestBody = """
            {
              "version": %d
            }
            """.formatted(originalVersion);

        // Attempt to publish a study without a feed record.
        mockMvc.perform(
                        post("/api/studies/{studyId}/publish", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("FEED_NOT_READY"));

        // Reload from PostgreSQL and verify that publication did not occur.
        entityManager.clear();

        Study unchangedStudy = studyRepository.findById(studyId)
                .orElseThrow();

        assertEquals(
                StudyStatus.DRAFT,
                unchangedStudy.getStatus()
        );

        assertNull(unchangedStudy.getPublishedAt());
        assertNull(unchangedStudy.getParticipationToken());

        assertEquals(
                originalVersion,
                unchangedStudy.getLockVersion()
        );

        // A rejected publication must not create a feed automatically.
        assertFalse(studyFeedRepository.existsById(studyId));
    }

    /** Verifies that publishing a study freezes its feed and prevents subsequent changes. */
    @Test
    void shouldRejectFeedUpdateAfterStudyPublication() throws Exception {

        // Create an owned draft study.
        Study study = studyRepository.saveAndFlush(
                new Study(
                        researcher.getId(),
                        "Feed Freeze Test Study",
                        "Verify feed immutability after publication"
                )
        );

        UUID studyId = study.getId();
        long originalStudyVersion = study.getLockVersion();

        // Prepare valid feed content.
        var template = feedTemplateRepository.findById("blank")
                .orElseThrow();

        StudyFeed feed = new StudyFeed(studyId, template);

        feed.replaceContent("""
            {
              "ROOT": {
                "type": "div"
              }
            }
            """);

        studyFeedRepository.saveAndFlush(feed);

        entityManager.clear();

        // Publish the study successfully.
        String publishRequest = """
            {
              "version": %d
            }
            """.formatted(originalStudyVersion);

        mockMvc.perform(
                        post("/api/studies/{studyId}/publish", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(publishRequest)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COLLECTING"));

        // Capture the persisted publication and feed state.
        entityManager.clear();

        Study publishedStudy = studyRepository.findById(studyId)
                .orElseThrow();

        StudyFeed publishedFeed = studyFeedRepository.findById(studyId)
                .orElseThrow();

        long publishedStudyVersion = publishedStudy.getLockVersion();
        long currentFeedVersion = publishedFeed.getLockVersion();

        String originalContent = publishedFeed.getContent();
        String originalToken = publishedStudy.getParticipationToken();
        Instant originalPublishedAt = publishedStudy.getPublishedAt();

        assertEquals(StudyStatus.COLLECTING, publishedStudy.getStatus());

        // Attempt to replace the feed using its CURRENT version.
        String updateRequest = """
            {
              "version": %d,
              "content": {
                "ROOT": {
                  "type": "ModifiedAfterPublication"
                }
              }
            }
            """.formatted(currentFeedVersion);

        mockMvc.perform(
                        put("/api/studies/{studyId}/feed", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateRequest)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("STUDY_NOT_EDITABLE"));

        // Reload the database state after the rejected update.
        entityManager.clear();

        Study unchangedStudy = studyRepository.findById(studyId)
                .orElseThrow();

        StudyFeed unchangedFeed = studyFeedRepository.findById(studyId)
                .orElseThrow();

        // The original feed must remain unchanged.
        assertEquals(originalContent, unchangedFeed.getContent());

        assertEquals(
                currentFeedVersion,
                unchangedFeed.getLockVersion()
        );

        // Publication metadata must also remain unchanged.
        assertEquals(StudyStatus.COLLECTING, unchangedStudy.getStatus());

        assertEquals(
                originalToken,
                unchangedStudy.getParticipationToken()
        );

        assertEquals(
                originalPublishedAt,
                unchangedStudy.getPublishedAt()
        );

        assertEquals(
                publishedStudyVersion,
                unchangedStudy.getLockVersion()
        );
    }

    /** Verifies that a published participation link exposes public configuration without researcher credentials. */
    @Test
    void shouldAllowPublicParticipationAccessAfterPublication() throws Exception {

        // Create an owned draft study.
        Study study = studyRepository.saveAndFlush(
                new Study(
                        researcher.getId(),
                        "Public Participation Study",
                        "Verify public access after publication"
                )
        );

        UUID studyId = study.getId();
        long originalVersion = study.getLockVersion();

        // Prepare valid feed content.
        var template = feedTemplateRepository.findById("blank")
                .orElseThrow();

        StudyFeed feed = new StudyFeed(studyId, template);

        feed.replaceContent("""
            {
              "ROOT": {
                "type": "div"
              }
            }
            """);

        studyFeedRepository.saveAndFlush(feed);

        entityManager.clear();

        // Publish the study through the authenticated endpoint.
        String publishRequest = """
            {
              "version": %d
            }
            """.formatted(originalVersion);

        mockMvc.perform(
                        post("/api/studies/{studyId}/publish", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(publishRequest)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COLLECTING"));

        // Retrieve the generated participation token from PostgreSQL.
        entityManager.clear();

        Study publishedStudy = studyRepository.findById(studyId)
                .orElseThrow();

        String participationToken = publishedStudy.getParticipationToken();

        assertNotNull(participationToken);
        assertEquals(43, participationToken.length());

        // Access the public participation API WITHOUT a researcher JWT.
        mockMvc.perform(
                        get("/api/participation/{token}", participationToken)
                )
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Cache-Control",
                        containsString("no-store")
                ))
                .andExpect(jsonPath("$.title")
                        .value("Public Participation Study"))
                .andExpect(jsonPath("$.description")
                        .value("Verify public access after publication"))
                .andExpect(jsonPath("$.eyeTrackingEnabled").value(false))
                .andExpect(jsonPath("$.questionnaireEnabled").value(false))
                .andExpect(jsonPath("$.content.ROOT.type").value("div"))

                // Researcher management metadata must not be exposed.
                .andExpect(jsonPath("$.id").doesNotExist())
                .andExpect(jsonPath("$.ownerId").doesNotExist())
                .andExpect(jsonPath("$.participationToken").doesNotExist())
                .andExpect(jsonPath("$.version").doesNotExist());
    }

    /** Verifies that malformed and unknown participation tokens return the same safe 404 response. */
    @Test
    void shouldRejectInvalidAndUnknownParticipationTokens() throws Exception {

        // A malformed token does not satisfy the required 43-character format.
        String malformedToken = "invalid-token";

        // This token has the correct format but does not exist in the database.
        String unknownToken = "A".repeat(43);

        assertTrue(
                studyRepository.findByParticipationToken(unknownToken).isEmpty()
        );

        // Reject the malformed token without exposing study information.
        String malformedResponse = mockMvc.perform(
                        get("/api/participation/{token}", malformedToken)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("PARTICIPATION_NOT_FOUND"))
                .andExpect(jsonPath("$.title").doesNotExist())
                .andExpect(jsonPath("$.content").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Reject a correctly formatted token that has no matching study.
        String unknownResponse = mockMvc.perform(
                        get("/api/participation/{token}", unknownToken)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("PARTICIPATION_NOT_FOUND"))
                .andExpect(jsonPath("$.title").doesNotExist())
                .andExpect(jsonPath("$.content").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Both failure cases must expose the same response.
        assertEquals(malformedResponse, unknownResponse);
    }

    /** Verifies that a published participation link fails safely if its feed record is missing. */
    @Test
    void shouldRejectPublicParticipationWhenPublishedFeedIsMissing() throws Exception {

        // Create a draft study with valid feed content.
        Study study = studyRepository.saveAndFlush(
                new Study(
                        researcher.getId(),
                        "Missing Published Feed Study",
                        "Verify safe public access when published content is unavailable"
                )
        );

        UUID studyId = study.getId();
        long originalVersion = study.getLockVersion();

        var template = feedTemplateRepository.findById("blank")
                .orElseThrow();

        StudyFeed feed = new StudyFeed(studyId, template);

        feed.replaceContent("""
            {
              "ROOT": {
                "type": "div"
              }
            }
            """);

        studyFeedRepository.saveAndFlush(feed);

        entityManager.clear();

        // Publish successfully before simulating the missing-feed condition.
        String publishRequest = """
            {
              "version": %d
            }
            """.formatted(originalVersion);

        mockMvc.perform(
                        post("/api/studies/{studyId}/publish", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(publishRequest)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COLLECTING"));

        entityManager.clear();

        Study publishedStudy = studyRepository.findById(studyId)
                .orElseThrow();

        String participationToken = publishedStudy.getParticipationToken();

        assertNotNull(participationToken);

        // Simulate an abnormal loss of the feed record after publication.
        studyFeedRepository.deleteById(studyId);
        studyFeedRepository.flush();

        entityManager.clear();

        assertFalse(studyFeedRepository.existsById(studyId));

        // A valid token must not expose missing or inconsistent content.
        mockMvc.perform(
                        get("/api/participation/{token}", participationToken)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FEED_NOT_READY"))
                .andExpect(jsonPath("$.content").doesNotExist())
                .andExpect(jsonPath("$.title").doesNotExist());

        // Verify that the study itself remains published.
        entityManager.clear();

        Study unchangedStudy = studyRepository.findById(studyId)
                .orElseThrow();

        assertEquals(StudyStatus.COLLECTING, unchangedStudy.getStatus());

        assertEquals(
                participationToken,
                unchangedStudy.getParticipationToken()
        );
    }

    /** Verifies that invalid image references cannot cause a partially published study. */
    @Test
    void shouldRejectPublicationWithMissingAssetWithoutPartialChanges() throws Exception {

        // Create an owned draft study.
        Study study = studyRepository.saveAndFlush(
                new Study(
                        researcher.getId(),
                        "Invalid Asset Publication Study",
                        "Verify publication atomicity when an image asset is missing"
                )
        );

        UUID studyId = study.getId();
        long originalStudyVersion = study.getLockVersion();

        // Generate an asset ID that does not exist in the database.
        UUID missingAssetId = UUID.randomUUID();

        assertFalse(studyAssetRepository.existsById(missingAssetId));

        // Prepare a feed containing a validly formatted but nonexistent assetId.
        var template = feedTemplateRepository.findById("blank")
                .orElseThrow();

        StudyFeed feed = new StudyFeed(studyId, template);

        feed.replaceContent("""
            {
              "ROOT": {
                "type": {
                  "resolvedName": "ImageWidget"
                },
                "props": {
                  "assetId": "%s"
                }
              }
            }
            """.formatted(missingAssetId));

        // Seed the invalid feed directly to exercise publication-time validation.
        studyFeedRepository.saveAndFlush(feed);

        entityManager.clear();

        // Capture the persisted state before publication.
        Study originalStudy = studyRepository.findById(studyId)
                .orElseThrow();

        StudyFeed originalFeed = studyFeedRepository.findById(studyId)
                .orElseThrow();

        String originalContent = originalFeed.getContent();
        long originalFeedVersion = originalFeed.getLockVersion();
        Instant originalUpdatedAt = originalStudy.getUpdatedAt();

        assertEquals(StudyStatus.DRAFT, originalStudy.getStatus());
        assertNull(originalStudy.getParticipationToken());
        assertNull(originalStudy.getPublishedAt());

        entityManager.clear();

        String publishRequest = """
            {
              "version": %d
            }
            """.formatted(originalStudyVersion);

        // Publication must reject the missing asset reference.
        mockMvc.perform(
                        post("/api/studies/{studyId}/publish", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(publishRequest)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("ASSET_REFERENCE_INVALID"))
                .andExpect(jsonPath("$.participationUrl").doesNotExist());

        // Clear the persistence context before inspecting database state.
        entityManager.clear();

        Study unchangedStudy = studyRepository.findById(studyId)
                .orElseThrow();

        StudyFeed unchangedFeed = studyFeedRepository.findById(studyId)
                .orElseThrow();

        // Publication failure must not change the study lifecycle.
        assertEquals(StudyStatus.DRAFT, unchangedStudy.getStatus());

        // No partial publication metadata may remain.
        assertNull(unchangedStudy.getParticipationToken());
        assertNull(unchangedStudy.getPublishedAt());

        // The rejected publication must not increment the study version.
        assertEquals(
                originalStudyVersion,
                unchangedStudy.getLockVersion()
        );

        assertEquals(
                originalUpdatedAt,
                unchangedStudy.getUpdatedAt()
        );

        // The original feed and its version must remain intact.
        assertEquals(originalContent, unchangedFeed.getContent());

        assertEquals(
                originalFeedVersion,
                unchangedFeed.getLockVersion()
        );

        // A failed publication must not create the missing asset.
        assertFalse(studyAssetRepository.existsById(missingAssetId));
    }

}

