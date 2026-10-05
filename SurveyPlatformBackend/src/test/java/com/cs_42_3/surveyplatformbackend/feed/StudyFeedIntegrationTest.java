package com.cs_42_3.surveyplatformbackend.feed;

import com.cs_42_3.surveyplatformbackend.researcher.domain.Researcher;
import com.cs_42_3.surveyplatformbackend.researcher.repository.ResearcherRepository;
import com.jayway.jsonpath.JsonPath;

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

import jakarta.persistence.EntityManager;
import java.time.Instant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StudyFeedIntegrationTest {

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
    private EntityManager entityManager;

    private String researcherToken;

    @BeforeEach
    void setUp() {
        Researcher researcher = researcherRepository.saveAndFlush(
                new Researcher(
                        "feed-integration-test@example.com",
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

    /** Verifies that creating a draft study initializes a feed retrievable by its owner. */
    @Test
    void shouldRetrieveInitializedStudyFeed() throws Exception {

        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Feed Integration Test",
                                  "description": "FT-001",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studyId").value(studyId))
                .andExpect(jsonPath("$.templateCode").value("blank"))
                .andExpect(jsonPath("$.version").isNumber())
                .andExpect(jsonPath("$.updatedAt").isNotEmpty());
    }

    /** Verifies that a researcher cannot access another researcher's study feed. */
    @Test
    void shouldRejectAccessToAnotherResearchersFeed() throws Exception {

        // Researcher A creates a study.
        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Private Feed Test",
                                  "description": "FT-002 ownership verification",
                                  "templateCode": "facebook"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        // Create an independent researcher B.
        Researcher researcherB = researcherRepository.saveAndFlush(
                new Researcher(
                        "feed-researcher-b@example.com",
                        "test-password-hash"
                )
        );

        Instant now = Instant.now();

        JwtClaimsSet claimsB = JwtClaimsSet.builder()
                .subject(researcherB.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("userId", researcherB.getId().toString())
                .claim("role", "RESEARCHER")
                .build();

        String tokenB = jwtEncoder
                .encode(JwtEncoderParameters.from(claimsB))
                .getTokenValue();

        // Researcher A can access their own feed.
        mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studyId").value(studyId));

        // Researcher B must not be able to retrieve A's feed.
        mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + tokenB)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY_NOT_FOUND"));
    }

    /** Verifies that a foreign researcher cannot overwrite another researcher's feed. */
    @Test
    void shouldRejectUnauthorizedFeedModification() throws Exception {

        // Researcher A creates a study.
        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Feed Write Isolation Test",
                                  "description": "FT-003",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        // Record A's original feed response and version.
        String originalFeed = mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number originalVersion = JsonPath.read(originalFeed, "$.version");

        // Create researcher B with a separate JWT.
        Researcher researcherB = researcherRepository.saveAndFlush(
                new Researcher(
                        "feed-write-researcher-b@example.com",
                        "test-password-hash"
                )
        );

        Instant now = Instant.now();

        JwtClaimsSet claimsB = JwtClaimsSet.builder()
                .subject(researcherB.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("userId", researcherB.getId().toString())
                .claim("role", "RESEARCHER")
                .build();

        String tokenB = jwtEncoder
                .encode(JwtEncoderParameters.from(claimsB))
                .getTokenValue();

        // B attempts to overwrite A's feed.
        String unauthorizedUpdate = """
                {
                  "content": {
                    "ROOT": {
                      "type": "div",
                      "props": {
                        "caption": "Unauthorized modification"
                      }
                    }
                  },
                  "version": %d
                }
                """.formatted(originalVersion.longValue());

        mockMvc.perform(
                        put("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + tokenB)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(unauthorizedUpdate)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY_NOT_FOUND"));

        // Verify that A's original feed remains unchanged.
        String feedAfterAttempt = mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Compare content without relying on timestamp precision.
        Object originalContent =
                JsonPath.read(originalFeed, "$.content");

        Object contentAfterAttempt =
                JsonPath.read(feedAfterAttempt, "$.content");

        org.junit.jupiter.api.Assertions.assertEquals(
                originalContent,
                contentAfterAttempt,
                "Rejected requests must not modify the stored feed content."
        );

        // Verify that the feed version remains unchanged.
        Number versionAfterAttempt =
                JsonPath.read(feedAfterAttempt, "$.version");

        org.junit.jupiter.api.Assertions.assertEquals(
                originalVersion.longValue(),
                versionAfterAttempt.longValue(),
                "Rejected requests must not change the feed version."
        );
    }

    /** Verifies that saving a draft feed persists its content and updated version. */
    @Test
    void shouldPersistAndRetrieveUpdatedFeed() throws Exception {

        // Create a blank study.
        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Feed Persistence Test",
                                  "description": "FT-004",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        // Retrieve the initial feed version.
        String initialFeed = mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number initialVersion = JsonPath.read(initialFeed, "$.version");

        // Save a Craft.js-style document containing one text widget.
        String saveRequest = """
                {
                  "content": {
                    "ROOT": {
                      "type": {
                        "resolvedName": "ResearcherEditCanvas"
                      },
                      "isCanvas": true,
                      "props": {},
                      "displayName": "Researcher Edit Canvas",
                      "custom": {},
                      "hidden": false,
                      "nodes": ["ft004Text"],
                      "linkedNodes": {}
                    },
                    "ft004Text": {
                      "type": {
                        "resolvedName": "TextWidget"
                      },
                      "isCanvas": false,
                      "props": {
                        "text": "FT-004 saved text",
                        "styleId": "facebook"
                      },
                      "displayName": "Text Widget",
                      "custom": {},
                      "hidden": false,
                      "parent": "ROOT",
                      "nodes": [],
                      "linkedNodes": {}
                    }
                  },
                  "version": %d
                }
                """.formatted(initialVersion.longValue());

        String savedFeed = mockMvc.perform(
                        put("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(saveRequest)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.ft004Text.props.text")
                        .value("FT-004 saved text"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number savedVersion = JsonPath.read(savedFeed, "$.version");

        org.junit.jupiter.api.Assertions.assertTrue(
                savedVersion.longValue() > initialVersion.longValue(),
                "Saving modified feed content should increase its version."
        );

        // Clear JPA's first-level cache before reading again.
        entityManager.clear();

        // Fetch the feed again and verify the persisted content.
        mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studyId").value(studyId))
                .andExpect(jsonPath("$.templateCode").value("blank"))
                .andExpect(jsonPath("$.content.ROOT.nodes[0]")
                        .value("ft004Text"))
                .andExpect(jsonPath("$.content.ft004Text.props.text")
                        .value("FT-004 saved text"))
                .andExpect(jsonPath("$.content.ft004Text.props.styleId")
                        .value("facebook"))
                .andExpect(jsonPath("$.version")
                        .value(savedVersion.longValue()));
    }

    /** Verifies that an outdated feed version cannot overwrite newer saved content. */
    @Test
    void shouldRejectStaleFeedVersionWithoutOverwritingContent() throws Exception {

        // Create a draft study.
        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Feed Version Conflict Test",
                                  "description": "FT-005",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        // Both editor sessions initially have the same feed version.
        String initialResponse = mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number initialVersion = JsonPath.read(initialResponse, "$.version");

        // Editor A saves a new document.
        String firstUpdate = """
                {
                  "content": {
                    "ROOT": {
                      "type": {
                        "resolvedName": "ResearcherEditCanvas"
                      },
                      "isCanvas": true,
                      "props": {
                        "testMarker": "Editor A"
                      },
                      "nodes": [],
                      "linkedNodes": {}
                    }
                  },
                  "version": %d
                }
                """.formatted(initialVersion.longValue());

        String firstSaveResponse = mockMvc.perform(
                        put("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(firstUpdate)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.ROOT.props.testMarker")
                        .value("Editor A"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number updatedVersion = JsonPath.read(firstSaveResponse, "$.version");

        org.junit.jupiter.api.Assertions.assertTrue(
                updatedVersion.longValue() > initialVersion.longValue()
        );

        // Editor B attempts to overwrite A's changes using the stale version.
        String staleUpdate = """
                {
                  "content": {
                    "ROOT": {
                      "type": {
                        "resolvedName": "ResearcherEditCanvas"
                      },
                      "isCanvas": true,
                      "props": {
                        "testMarker": "Editor B"
                      },
                      "nodes": [],
                      "linkedNodes": {}
                    }
                  },
                  "version": %d
                }
                """.formatted(initialVersion.longValue());

        mockMvc.perform(
                        put("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(staleUpdate)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FEED_VERSION_CONFLICT"));

        // Clear the persistence context and retrieve the saved feed again.
        entityManager.clear();

        // Editor A's content must remain unchanged.
        mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.ROOT.props.testMarker")
                        .value("Editor A"))
                .andExpect(jsonPath("$.version")
                        .value(updatedVersion.longValue()));
    }

    /** Verifies that unauthenticated users cannot read or modify a study feed. */
    @Test
    void shouldRejectUnauthenticatedFeedAccess() throws Exception {

        // Create a study using the authenticated researcher.
        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Feed Authentication Test",
                                  "description": "FT-006",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        // Record the original feed before attempting unauthorized access.
        String originalFeed = mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number originalVersion = JsonPath.read(originalFeed, "$.version");

        // An unauthenticated user must not be able to read the feed.
        mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                )
                .andExpect(status().isUnauthorized());

        // An unauthenticated user must not be able to modify the feed.
        String updateRequest = """
                {
                  "content": {
                    "ROOT": {
                      "type": {
                        "resolvedName": "ResearcherEditCanvas"
                      },
                      "props": {
                        "testMarker": "Unauthorized change"
                      },
                      "nodes": [],
                      "linkedNodes": {}
                    }
                  },
                  "version": %d
                }
                """.formatted(originalVersion.longValue());

        mockMvc.perform(
                        put("/api/studies/{studyId}/feed", studyId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(updateRequest)
                )
                .andExpect(status().isUnauthorized());

        // Verify that the rejected request did not change the stored feed.
        entityManager.clear();

        String feedAfterAttempt = mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Compare content without relying on timestamp precision.
        Object originalContent =
                JsonPath.read(originalFeed, "$.content");

        Object contentAfterAttempt =
                JsonPath.read(feedAfterAttempt, "$.content");

        org.junit.jupiter.api.Assertions.assertEquals(
                originalContent,
                contentAfterAttempt,
                "Rejected requests must not modify the stored feed content."
        );

        // Verify that the feed version remains unchanged.
        Number versionAfterAttempt =
                JsonPath.read(feedAfterAttempt, "$.version");

        org.junit.jupiter.api.Assertions.assertEquals(
                originalVersion.longValue(),
                versionAfterAttempt.longValue(),
                "Rejected requests must not change the feed version."
        );
    }

    /** Verifies that a published study rejects feed edits without changing saved content. */
    @Test
    void shouldRejectFeedModificationAfterPublication() throws Exception {

        // Create a blank draft study.
        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Published Feed Read-Only Test",
                                  "description": "FT-007",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        // Read the initial feed version.
        String initialFeed = mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number initialFeedVersion = JsonPath.read(initialFeed, "$.version");

        // Save content before publication.
        String draftContent = """
                {
                  "content": {
                    "ROOT": {
                      "type": {
                        "resolvedName": "ResearcherEditCanvas"
                      },
                      "isCanvas": true,
                      "props": {},
                      "nodes": ["ft007Text"],
                      "linkedNodes": {}
                    },
                    "ft007Text": {
                      "type": {
                        "resolvedName": "TextWidget"
                      },
                      "isCanvas": false,
                      "props": {
                        "text": "Original published content",
                        "styleId": "facebook"
                      },
                      "parent": "ROOT",
                      "nodes": [],
                      "linkedNodes": {}
                    }
                  },
                  "version": %d
                }
                """.formatted(initialFeedVersion.longValue());

        String savedFeed = mockMvc.perform(
                        put("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(draftContent)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number savedFeedVersion = JsonPath.read(savedFeed, "$.version");

        // Get the current STUDY version, which is separate from the feed version.
        String studyResponse = mockMvc.perform(
                        get("/api/studies/{studyId}", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number studyVersion = JsonPath.read(studyResponse, "$.version");

        // Publish the study.
        mockMvc.perform(
                        post("/api/studies/{studyId}/publish", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "version": %d
                                }
                                """.formatted(studyVersion.longValue()))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COLLECTING"));

        // Confirm that the published feed remains readable.
        String publishedFeed = mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.ft007Text.props.text")
                        .value("Original published content"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Attempt to modify the published feed using its CURRENT version.
        String forbiddenUpdate = """
                {
                  "content": {
                    "ROOT": {
                      "type": {
                        "resolvedName": "ResearcherEditCanvas"
                      },
                      "props": {
                        "testMarker": "Illegal modification"
                      },
                      "nodes": [],
                      "linkedNodes": {}
                    }
                  },
                  "version": %d
                }
                """.formatted(savedFeedVersion.longValue());

        mockMvc.perform(
                        put("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(forbiddenUpdate)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STUDY_NOT_EDITABLE"));

        // Verify that the rejected request did not change persisted content.
        entityManager.clear();

        String feedAfterAttempt = mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        // Compare the saved JSON documents, not the entire HTTP responses.
        Object publishedContent =
                JsonPath.read(publishedFeed, "$.content");

        Object contentAfterAttempt =
                JsonPath.read(feedAfterAttempt, "$.content");

        org.junit.jupiter.api.Assertions.assertEquals(
                publishedContent,
                contentAfterAttempt,
                "Published feed content must remain unchanged."
        );


        // A rejected edit must not increase the feed version.
        Number versionAfterAttempt =
                JsonPath.read(feedAfterAttempt, "$.version");

        org.junit.jupiter.api.Assertions.assertEquals(
                savedFeedVersion.longValue(),
                versionAfterAttempt.longValue(),
                "Rejected feed modification must not change the version."
        );

    }

    /** Verifies that invalid feed save requests are rejected without changing stored data. */
    @Test
    void shouldRejectInvalidFeedSaveRequests() throws Exception {

        // Create an owned draft study.
        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Feed Validation Test",
                                  "description": "FT-008",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        // Record the initial feed version.
        String originalFeed = mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number originalVersion = JsonPath.read(originalFeed, "$.version");

        // Each request violates the SaveFeedRequest validation rules.
        String[] invalidRequests = {
                """
                {
                  "version": %d
                }
                """.formatted(originalVersion.longValue()),

                """
                {
                  "content": null,
                  "version": %d
                }
                """.formatted(originalVersion.longValue()),

                """
                {
                  "content": {"ROOT": {}},
                  "version": -1
                }
                """,

                """
                {
                  "content": {"ROOT": {}},
                  "version": 1.5
                }
                """,

                """
                {
                  "content": {"ROOT": {}},
                  "version": "0"
                }
                """
        };

        // All malformed requests must be rejected.
        for (String invalidRequest : invalidRequests) {
            mockMvc.perform(
                            put("/api/studies/{studyId}/feed", studyId)
                                    .header(
                                            "Authorization",
                                            "Bearer " + researcherToken
                                    )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(invalidRequest)
                    )
                    .andExpect(status().isBadRequest());
        }

        // Reload the feed to verify that none of the requests changed it.
        entityManager.clear();

        mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version")
                        .value(originalVersion.longValue()))
                .andExpect(jsonPath("$.content")
                        .value(org.hamcrest.Matchers.nullValue()));
    }

    /** Verifies that a feed cannot reference a nonexistent image asset. */
    @Test
    void shouldRejectNonexistentImageAssetReference() throws Exception {

        // Create an owned draft study.
        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Invalid Image Reference Test",
                                  "description": "FT-009",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        // Read the initial feed version.
        String originalFeed = mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number originalVersion = JsonPath.read(originalFeed, "$.version");

        // Generate an asset ID that has not been uploaded to this test database.
        String nonexistentAssetId = java.util.UUID.randomUUID().toString();

        String invalidFeed = """
                {
                  "content": {
                    "ROOT": {
                      "type": {
                        "resolvedName": "ResearcherEditCanvas"
                      },
                      "isCanvas": true,
                      "props": {},
                      "nodes": ["invalidImage"],
                      "linkedNodes": {}
                    },
                    "invalidImage": {
                      "type": {
                        "resolvedName": "ImageWidget"
                      },
                      "isCanvas": false,
                      "props": {
                        "assetId": "%s",
                        "styleId": "facebook"
                      },
                      "parent": "ROOT",
                      "nodes": [],
                      "linkedNodes": {}
                    }
                  },
                  "version": %d
                }
                """.formatted(
                nonexistentAssetId,
                originalVersion.longValue()
        );

        // The server must reject a reference to an image that does not exist.
        mockMvc.perform(
                        put("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidFeed)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("ASSET_REFERENCE_INVALID"));

        // Clear JPA cache and verify that the rejected request changed nothing.
        entityManager.clear();

        mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content")
                        .value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.version")
                        .value(originalVersion.longValue()));
    }

    /** Verifies that temporary or protected image URLs cannot be persisted in feed content. */
    @Test
    void shouldRejectUnsafeImageUrlsInFeedContent() throws Exception {

        // Create an owned draft study.
        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Unsafe Image URL Test",
                                  "description": "FT-010",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        // Record the initial feed version.
        String originalFeed = mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number originalVersion = JsonPath.read(originalFeed, "$.version");

        // These URLs must never be stored directly in the feed document.
        String[] invalidSources = {
                "blob:http://localhost:5173/temporary-image",
                "/api/studies/" + studyId
                        + "/assets/" + java.util.UUID.randomUUID()
                        + "/content"
        };

        for (String invalidSource : invalidSources) {

            String invalidRequest = """
                    {
                      "content": {
                        "ROOT": {
                          "type": {
                            "resolvedName": "ResearcherEditCanvas"
                          },
                          "isCanvas": true,
                          "props": {},
                          "nodes": ["unsafeImage"],
                          "linkedNodes": {}
                        },
                        "unsafeImage": {
                          "type": {
                            "resolvedName": "ImageWidget"
                          },
                          "isCanvas": false,
                          "props": {
                            "src": "%s",
                            "styleId": "facebook"
                          },
                          "parent": "ROOT",
                          "nodes": [],
                          "linkedNodes": {}
                        }
                      },
                      "version": %d
                    }
                    """.formatted(
                    invalidSource,
                    originalVersion.longValue()
            );

            // Both temporary and protected URLs must be rejected.
            mockMvc.perform(
                            put("/api/studies/{studyId}/feed", studyId)
                                    .header(
                                            "Authorization",
                                            "Bearer " + researcherToken
                                    )
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(invalidRequest)
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code")
                            .value("ASSET_REFERENCE_INVALID"));
        }

        // Confirm that rejected requests did not modify persisted data.
        entityManager.clear();

        mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content")
                        .value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.version")
                        .value(originalVersion.longValue()));
    }

}
