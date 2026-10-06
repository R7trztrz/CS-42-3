package com.cs_42_3.surveyplatformbackend.linkpreview;

import com.cs_42_3.surveyplatformbackend.linkpreview.exception.LinkPreviewException;
import com.cs_42_3.surveyplatformbackend.linkpreview.infrastructure.RemoteDocument;
import com.cs_42_3.surveyplatformbackend.linkpreview.infrastructure.SafeHttpFetcher;
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

import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class LinkPreviewIntegrationTest {

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

    @MockitoBean
    private SafeHttpFetcher safeHttpFetcher;

    private String researcherToken;

    @BeforeEach
    void setUp() {

        Researcher researcher = researcherRepository.saveAndFlush(
                new Researcher(
                        "link-preview-researcher-a@example.com",
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

    /** Verifies authentication and ownership checks on the link preview API. */
    @Test
    void shouldRejectUnauthorizedLinkPreviewRequests() throws Exception {

        // Researcher A creates an owned draft study.
        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Link Preview Authorization Test",
                                  "description": "FT-034",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        // This URL is deliberately blocked by the real SSRF policy.
        // It allows us to test authorization without contacting the network.
        String requestBody = """
                {
                  "url": "http://127.0.0.1/internal"
                }
                """;
        when(
                safeHttpFetcher.fetch(
                        "http://127.0.0.1/internal",
                        2 * 1024 * 1024
                )
        ).thenThrow(
                LinkPreviewException.invalidUrl()
        );

        // 1. A request without JWT must be rejected immediately.
        mockMvc.perform(
                        post("/api/studies/{studyId}/link-previews", studyId)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_UNAUTHORIZED"));

        // 2. Create an independent researcher B.
        Researcher researcherB = researcherRepository.saveAndFlush(
                new Researcher(
                        "link-preview-researcher-b@example.com",
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

        // B must not use A's Study ID to generate a link preview.
        mockMvc.perform(
                        post("/api/studies/{studyId}/link-previews", studyId)
                                .header("Authorization", "Bearer " + tokenB)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY_NOT_FOUND"));

        // 3. A owns the study and should pass the authorization checks.
        // The request then reaches the actual SSRF policy and is rejected
        // because 127.0.0.1 is a non-public destination.
        mockMvc.perform(
                        post("/api/studies/{studyId}/link-previews", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("PREVIEW_URL_INVALID"));
    }

    /** Verifies that link previews cannot be generated after a study is published. */
    @Test
    void shouldRejectLinkPreviewAfterStudyPublication() throws Exception {

        // Create a populated draft study to avoid the known blank-publication issue.
        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Published Link Preview Test",
                                  "description": "FT-035",
                                  "templateCode": "facebook"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        // Retrieve the current study version required by publication.
        String studyResponse = mockMvc.perform(
                        get("/api/studies/{studyId}", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number studyVersion = JsonPath.read(
                studyResponse,
                "$.version"
        );

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

        // Use a blocked URL deliberately.
        // STUDY_NOT_EDITABLE must occur BEFORE any URL/network processing.
        String requestBody = """
                {
                  "url": "http://127.0.0.1/internal"
                }
                """;

        mockMvc.perform(
                        post("/api/studies/{studyId}/link-previews", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("STUDY_NOT_EDITABLE"));

        // Confirm the study remains published.
        mockMvc.perform(
                        get("/api/studies/{studyId}", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COLLECTING"));
    }

    /** Verifies that incomplete preview metadata returns a warning without modifying the study feed. */
    @Test
    void shouldReturnMetadataWarningWithoutChangingFeed() throws Exception {

        // Create an owned draft study.
        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Incomplete Preview Metadata Test",
                                  "description": "FT-036",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        // Record the feed before generating the preview.
        String feedBefore = mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number versionBefore = JsonPath.read(
                feedBefore,
                "$.version"
        );

        // Simulate a valid HTML page that has a title,
        // but no description and no preview image.
        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <title>Preview Article Title</title>
                </head>
                <body>Example article content</body>
                </html>
                """;

        String sourceUrl = "https://example.org/article";

        RemoteDocument remotePage = new RemoteDocument(
                URI.create(sourceUrl),
                "text/html; charset=UTF-8",
                html.getBytes(StandardCharsets.UTF_8)
        );

        when(
                safeHttpFetcher.fetch(
                        sourceUrl,
                        2 * 1024 * 1024
                )
        ).thenReturn(remotePage);

        // Generate the link preview through the real API/service/parser flow.
        mockMvc.perform(
                        post("/api/studies/{studyId}/link-previews", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "url": "https://example.org/article"
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Cache-Control",
                        org.hamcrest.Matchers.containsString("no-store")
                ))
                .andExpect(jsonPath("$.sourceUrl")
                        .value(sourceUrl))
                .andExpect(jsonPath("$.title")
                        .value("Preview Article Title"))
                .andExpect(jsonPath("$.description")
                        .value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.image")
                        .value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.warnings.length()")
                        .value(1))
                .andExpect(jsonPath("$.warnings[0].code")
                        .value("PREVIEW_METADATA_INCOMPLETE"));

        // Link preview generation must not save or alter the study feed.
        mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content")
                        .value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.version")
                        .value(versionBefore.longValue()));
    }

    /** Verifies that unsupported remote content is mapped to HTTP 422 by the API. */
    @Test
    void shouldRejectUnsupportedContentThroughApi() throws Exception {

        // Create an owned draft study.
        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Unsupported Preview Content Test",
                                  "description": "FT-037",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        // Record the feed version before the failed preview request.
        String feedBefore = mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number versionBefore = JsonPath.read(feedBefore, "$.version");

        String sourceUrl = "https://example.org/data.json";

        // The mock returns a remote JSON document.
        // The real MetadataParser must reject its Content-Type.
        RemoteDocument remoteDocument = new RemoteDocument(
                URI.create(sourceUrl),
                "application/json; charset=UTF-8",
                """
                {"title": "This is JSON, not an HTML page"}
                """.getBytes(StandardCharsets.UTF_8)
        );

        when(
                safeHttpFetcher.fetch(
                        sourceUrl,
                        2 * 1024 * 1024
                )
        ).thenReturn(remoteDocument);

        mockMvc.perform(
                        post("/api/studies/{studyId}/link-previews", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "url": "https://example.org/data.json"
                                }
                                """)
                )
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code")
                        .value("PREVIEW_CONTENT_UNSUPPORTED"));

        // A rejected preview must leave the feed unchanged.
        mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content")
                        .value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.version")
                        .value(versionBefore.longValue()));
    }

    /** Verifies plain-text conversion and metadata length limits in API responses. */
    @Test
    void shouldReturnSanitizedAndLimitedMetadataThroughApi() throws Exception {

        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Preview Metadata Safety Test",
                                  "description": "FT-038",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");
        String sourceUrl = "https://example.org/metadata-safety";

        // Scenario 1: Encoded HTML markup must become plain text.
        String markupHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta property="og:title"
                          content="&lt;b&gt;Safe headline&lt;/b&gt;">
                    <meta property="og:description"
                          content="&lt;p&gt;Readable &amp;amp; useful&lt;/p&gt;">
                </head>
                <body></body>
                </html>
                """;

        when(
                safeHttpFetcher.fetch(sourceUrl, 2 * 1024 * 1024)
        ).thenReturn(
                new RemoteDocument(
                        URI.create(sourceUrl),
                        "text/html; charset=UTF-8",
                        markupHtml.getBytes(StandardCharsets.UTF_8)
                )
        );

        String requestBody = """
                {
                  "url": "https://example.org/metadata-safety"
                }
                """;

        mockMvc.perform(
                        post("/api/studies/{studyId}/link-previews", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title")
                        .value("Safe headline"))
                .andExpect(jsonPath("$.description")
                        .value("Readable & useful"))
                .andExpect(jsonPath("$.image")
                        .value(org.hamcrest.Matchers.nullValue()));

        // Scenario 2: Oversized metadata must be truncated.
        String oversizedHtml = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta property="og:title" content="%s">
                    <meta property="og:description" content="%s">
                </head>
                <body></body>
                </html>
                """.formatted(
                "T".repeat(501),
                "D".repeat(5001)
        );

        // Replace the response for the second request in this test.
        when(
                safeHttpFetcher.fetch(sourceUrl, 2 * 1024 * 1024)
        ).thenReturn(
                new RemoteDocument(
                        URI.create(sourceUrl),
                        "text/html; charset=UTF-8",
                        oversizedHtml.getBytes(StandardCharsets.UTF_8)
                )
        );

        mockMvc.perform(
                        post("/api/studies/{studyId}/link-previews", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title")
                        .value("T".repeat(500)))
                .andExpect(jsonPath("$.description")
                        .value("D".repeat(5000)))
                .andExpect(jsonPath("$.image")
                        .value(org.hamcrest.Matchers.nullValue()));
    }

    /** Verifies that image download failure preserves text metadata and leaves the feed unchanged. */
    @Test
    void shouldReturnImageWarningWhenImageDownloadFails() throws Exception {

        // Create an owned draft study.
        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Preview Image Failure Test",
                                  "description": "FT-039",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        String feedBefore = mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number versionBefore = JsonPath.read(feedBefore, "$.version");

        String sourceUrl = "https://example.org/image-failure";
        String imageUrl = "https://example.org/images/cover.png";

        // Complete text metadata prevents a metadata-incomplete warning.
        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta property="og:title"
                          content="Article With Unavailable Image">
                    <meta property="og:description"
                          content="The article text remains available.">
                    <meta property="og:image"
                          content="/images/cover.png">
                </head>
                <body></body>
                </html>
                """;

        when(
                safeHttpFetcher.fetch(sourceUrl, 2 * 1024 * 1024)
        ).thenReturn(
                new RemoteDocument(
                        URI.create(sourceUrl),
                        "text/html; charset=UTF-8",
                        html.getBytes(StandardCharsets.UTF_8)
                )
        );

        // Simulate failure of the separate image download.
        when(
                safeHttpFetcher.fetch(imageUrl, 5 * 1024 * 1024)
        ).thenThrow(
                LinkPreviewException.fetchFailed()
        );

        mockMvc.perform(
                        post("/api/studies/{studyId}/link-previews", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "url": "https://example.org/image-failure"
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Cache-Control",
                        org.hamcrest.Matchers.containsString("no-store")
                ))
                .andExpect(jsonPath("$.sourceUrl").value(sourceUrl))
                .andExpect(jsonPath("$.title")
                        .value("Article With Unavailable Image"))
                .andExpect(jsonPath("$.description")
                        .value("The article text remains available."))
                .andExpect(jsonPath("$.image")
                        .value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.warnings.length()").value(1))
                .andExpect(jsonPath("$.warnings[0].code")
                        .value("PREVIEW_IMAGE_UNAVAILABLE"));

        // Confirm both download paths were exercised with their actual limits.
        org.mockito.Mockito.verify(safeHttpFetcher)
                .fetch(sourceUrl, 2 * 1024 * 1024);

        org.mockito.Mockito.verify(safeHttpFetcher)
                .fetch(imageUrl, 5 * 1024 * 1024);

        // Generating the preview must not modify the feed.
        mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content")
                        .value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.version")
                        .value(versionBefore.longValue()));
    }

    /** Verifies that invalid image bytes produce a warning while preserving text metadata. */
    @Test
    void shouldReturnImageWarningForInvalidImageBytes() throws Exception {

        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Invalid Preview Image Test",
                                  "description": "FT-040",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        String feedBefore = mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number versionBefore = JsonPath.read(feedBefore, "$.version");

        String sourceUrl = "https://example.org/invalid-image";
        String imageUrl = "https://example.org/images/fake.png";

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta property="og:title"
                          content="Article With Invalid Image">
                    <meta property="og:description"
                          content="Text metadata should still be returned.">
                    <meta property="og:image"
                          content="/images/fake.png">
                </head>
                <body></body>
                </html>
                """;

        when(
                safeHttpFetcher.fetch(sourceUrl, 2 * 1024 * 1024)
        ).thenReturn(
                new RemoteDocument(
                        URI.create(sourceUrl),
                        "text/html; charset=UTF-8",
                        html.getBytes(StandardCharsets.UTF_8)
                )
        );

        // The URL and MIME header claim PNG, but the bytes are plain text.
        // The real image validator must reject them.
        when(
                safeHttpFetcher.fetch(imageUrl, 5 * 1024 * 1024)
        ).thenReturn(
                new RemoteDocument(
                        URI.create(imageUrl),
                        "image/png",
                        "This is not a PNG image."
                                .getBytes(StandardCharsets.UTF_8)
                )
        );

        mockMvc.perform(
                        post("/api/studies/{studyId}/link-previews", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "url": "https://example.org/invalid-image"
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceUrl").value(sourceUrl))
                .andExpect(jsonPath("$.title")
                        .value("Article With Invalid Image"))
                .andExpect(jsonPath("$.description")
                        .value("Text metadata should still be returned."))
                .andExpect(jsonPath("$.image")
                        .value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.warnings.length()").value(1))
                .andExpect(jsonPath("$.warnings[0].code")
                        .value("PREVIEW_IMAGE_UNAVAILABLE"));

        org.mockito.Mockito.verify(safeHttpFetcher)
                .fetch(sourceUrl, 2 * 1024 * 1024);

        org.mockito.Mockito.verify(safeHttpFetcher)
                .fetch(imageUrl, 5 * 1024 * 1024);

        // Rejected image data must not change the feed.
        mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content")
                        .value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.version")
                        .value(versionBefore.longValue()));
    }

    /** Verifies successful preview image import and retrieval without changing the feed. */
    @Test
    void shouldImportValidPreviewImageWithoutChangingFeed() throws Exception {

        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Valid Preview Image Test",
                                  "description": "FT-041",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        String feedBefore = mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number versionBefore = JsonPath.read(feedBefore, "$.version");

        // Generate a valid 2 × 3 PNG entirely in memory.
        var fixtureImage = new java.awt.image.BufferedImage(
                2,
                3,
                java.awt.image.BufferedImage.TYPE_INT_RGB
        );

        byte[] pngBytes;
        try (var output = new java.io.ByteArrayOutputStream()) {
            org.junit.jupiter.api.Assertions.assertTrue(
                    javax.imageio.ImageIO.write(
                            fixtureImage,
                            "png",
                            output
                    ),
                    "A PNG encoder must be available."
            );
            pngBytes = output.toByteArray();
        } finally {
            fixtureImage.flush();
        }

        String sourceUrl = "https://example.org/valid-image";
        String imageUrl = "https://example.org/images/cover.png";

        String html = """
                <!DOCTYPE html>
                <html>
                <head>
                    <meta property="og:title"
                          content="Article With Valid Image">
                    <meta property="og:description"
                          content="Complete preview metadata.">
                    <meta property="og:image"
                          content="/images/cover.png">
                </head>
                <body></body>
                </html>
                """;

        when(
                safeHttpFetcher.fetch(sourceUrl, 2 * 1024 * 1024)
        ).thenReturn(
                new RemoteDocument(
                        URI.create(sourceUrl),
                        "text/html; charset=UTF-8",
                        html.getBytes(StandardCharsets.UTF_8)
                )
        );

        when(
                safeHttpFetcher.fetch(imageUrl, 5 * 1024 * 1024)
        ).thenReturn(
                new RemoteDocument(
                        URI.create(imageUrl),
                        "image/png",
                        pngBytes
                )
        );

        String previewResponse = mockMvc.perform(
                        post("/api/studies/{studyId}/link-previews", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "url": "https://example.org/valid-image"
                                }
                                """)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sourceUrl").value(sourceUrl))
                .andExpect(jsonPath("$.title")
                        .value("Article With Valid Image"))
                .andExpect(jsonPath("$.description")
                        .value("Complete preview metadata."))
                .andExpect(jsonPath("$.image.assetId").isNotEmpty())
                .andExpect(jsonPath("$.image.filename")
                        .value("image.png"))
                .andExpect(jsonPath("$.image.contentType")
                        .value("image/png"))
                .andExpect(jsonPath("$.image.width").value(2))
                .andExpect(jsonPath("$.image.height").value(3))
                .andExpect(jsonPath("$.image.sizeBytes")
                        .value(pngBytes.length))
                .andExpect(jsonPath("$.warnings.length()").value(0))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String assetId = JsonPath.read(
                previewResponse,
                "$.image.assetId"
        );

        String contentUrl = JsonPath.read(
                previewResponse,
                "$.image.contentUrl"
        );

        org.junit.jupiter.api.Assertions.assertEquals(
                "/api/studies/" + studyId
                        + "/assets/" + assetId + "/content",
                contentUrl
        );

        // Read the actual stored image through the authenticated API.
        mockMvc.perform(
                        get(contentUrl)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(
                        MediaType.IMAGE_PNG
                ))
                .andExpect(content().bytes(pngBytes));

        org.mockito.Mockito.verify(safeHttpFetcher)
                .fetch(sourceUrl, 2 * 1024 * 1024);

        org.mockito.Mockito.verify(safeHttpFetcher)
                .fetch(imageUrl, 5 * 1024 * 1024);

        // Importing an Asset must not automatically insert it into the feed.
        mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content")
                        .value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.version")
                        .value(versionBefore.longValue()));
    }

    /** Verifies webpage fetch failure mapping and that the feed remains unchanged. */
    @Test
    void shouldReturnFetchFailureWithoutChangingFeed() throws Exception {

        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Webpage Fetch Failure Test",
                                  "description": "FT-042",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        String feedBefore = mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number versionBefore = JsonPath.read(feedBefore, "$.version");

        String sourceUrl = "https://example.org/unavailable-page";

        // Simulate the downloader's safe failure for the webpage itself.
        when(
                safeHttpFetcher.fetch(sourceUrl, 2 * 1024 * 1024)
        ).thenThrow(
                LinkPreviewException.fetchFailed()
        );

        mockMvc.perform(
                        post("/api/studies/{studyId}/link-previews", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "url": "https://example.org/unavailable-page"
                                }
                                """)
                )
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code")
                        .value("PREVIEW_FETCH_FAILED"));

        // Only the webpage download should have been attempted.
        org.mockito.Mockito.verify(safeHttpFetcher)
                .fetch(sourceUrl, 2 * 1024 * 1024);

        org.mockito.Mockito.verifyNoMoreInteractions(safeHttpFetcher);

        mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content")
                        .value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.version")
                        .value(versionBefore.longValue()));
    }
}
