package com.cs_42_3.surveyplatformbackend.asset;

import com.cs_42_3.surveyplatformbackend.asset.repository.StudyAssetRepository;
import com.cs_42_3.surveyplatformbackend.researcher.domain.Researcher;
import com.cs_42_3.surveyplatformbackend.researcher.repository.ResearcherRepository;
import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class StudyAssetIntegrationTest {

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

        // Keep test uploads separate from normal application assets.
        registry.add(
                "app.assets.storage-directory",
                () -> Path.of("target", "test-assets")
                        .toAbsolutePath()
                        .toString()
        );
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResearcherRepository researcherRepository;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private StudyAssetRepository studyAssetRepository;

    private String researcherToken;

    @BeforeEach
    void setUp() {

        Researcher researcher = researcherRepository.saveAndFlush(
                new Researcher(
                        "asset-integration-test@example.com",
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

    /** Verifies that a researcher can upload and retrieve a valid PNG image. */
    @Test
    void shouldUploadAndRetrieveValidPngImage() throws Exception {

        // Create a study owned by the authenticated researcher.
        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Asset Upload Test",
                                  "description": "FT-011",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        // Generate a real 2 x 2 PNG instead of using fake image bytes.
        BufferedImage image =
                new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertTrue(ImageIO.write(image, "png", output));

        byte[] pngBytes = output.toByteArray();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "sample.png",
                "image/png",
                pngBytes
        );

        // Upload through the actual HTTP endpoint.
        String uploadResponse = mockMvc.perform(
                        multipart("/api/studies/{studyId}/assets", studyId)
                                .file(file)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetId").isNotEmpty())
                .andExpect(jsonPath("$.filename").value("sample.png"))
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.sizeBytes").value(pngBytes.length))
                .andExpect(jsonPath("$.width").value(2))
                .andExpect(jsonPath("$.height").value(2))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String assetId = JsonPath.read(uploadResponse, "$.assetId");

        // Confirm the returned identifier is a valid UUID.
        assertNotNull(UUID.fromString(assetId));

        String expectedContentUrl =
                "/api/studies/" + studyId
                        + "/assets/" + assetId
                        + "/content";

        assertEquals(
                expectedContentUrl,
                JsonPath.read(uploadResponse, "$.contentUrl")
        );

        // Retrieve the image and verify its original bytes.
        mockMvc.perform(
                        get(
                                "/api/studies/{studyId}/assets/{assetId}/content",
                                studyId,
                                assetId
                        )
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(pngBytes))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    /** Verifies that fake and empty images are rejected without creating asset records. */
    @Test
    void shouldRejectInvalidImageUploads() throws Exception {

        // Create a study owned by the authenticated researcher.
        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Invalid Image Upload Test",
                                  "description": "FT-012",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        // Record the asset count before attempting invalid uploads.
        long assetCountBefore = studyAssetRepository.count();

        // A fake PNG: filename and MIME type look valid, but bytes are plain text.
        MockMultipartFile fakePng = new MockMultipartFile(
                "file",
                "fake.png",
                "image/png",
                "This is not a real PNG image."
                        .getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );

        // An empty PNG file must also be rejected.
        MockMultipartFile emptyPng = new MockMultipartFile(
                "file",
                "empty.png",
                "image/png",
                new byte[0]
        );

        MockMultipartFile[] invalidFiles = {
                fakePng,
                emptyPng
        };

        // Both uploads must fail with the expected business error.
        for (MockMultipartFile invalidFile : invalidFiles) {

            mockMvc.perform(
                            multipart("/api/studies/{studyId}/assets", studyId)
                                    .file(invalidFile)
                                    .header(
                                            "Authorization",
                                            "Bearer " + researcherToken
                                    )
                    )
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code")
                            .value("ASSET_INVALID_IMAGE"));
        }

        // Failed uploads must not create database records.
        assertEquals(
                assetCountBefore,
                studyAssetRepository.count(),
                "Invalid uploads must not create asset records."
        );
    }

    /** Verifies that images exceeding the 5 MiB limit are rejected without persistence. */
    @Test
    void shouldRejectImageExceedingFiveMiB() throws Exception {

        // Create a draft study owned by the authenticated researcher.
        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Image Size Limit Test",
                                  "description": "FT-013",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        long assetCountBefore = studyAssetRepository.count();

        // Generate a file exactly one byte larger than the 5 MiB limit.
        byte[] oversizedBytes = new byte[5 * 1024 * 1024 + 1];

        MockMultipartFile oversizedFile = new MockMultipartFile(
                "file",
                "oversized.png",
                "image/png",
                oversizedBytes
        );

        // The server must reject the upload before attempting image decoding.
        mockMvc.perform(
                        multipart("/api/studies/{studyId}/assets", studyId)
                                .file(oversizedFile)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                )
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code")
                        .value("ASSET_TOO_LARGE"));

        // An oversized upload must not create an asset database record.
        assertEquals(
                assetCountBefore,
                studyAssetRepository.count(),
                "Oversized uploads must not create asset records."
        );
    }

    /** Verifies that image access is isolated across studies and researcher accounts. */
    @Test
    void shouldRejectCrossStudyAndCrossResearcherAssetAccess() throws Exception {

        // Researcher A creates Study A.
        String createResponseA = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Asset Isolation Study A",
                                  "description": "FT-014 source study",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyA = JsonPath.read(createResponseA, "$.id");

        // The same researcher creates an independent Study B.
        String createResponseB = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Asset Isolation Study B",
                                  "description": "FT-014 unrelated study",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyB = JsonPath.read(createResponseB, "$.id");

        // Generate a real PNG image.
        BufferedImage image =
                new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertTrue(ImageIO.write(image, "png", output));

        byte[] pngBytes = output.toByteArray();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "private-image.png",
                "image/png",
                pngBytes
        );

        // Upload the image exclusively to Study A.
        String uploadResponse = mockMvc.perform(
                        multipart("/api/studies/{studyId}/assets", studyA)
                                .file(file)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String assetId = JsonPath.read(uploadResponse, "$.assetId");

        // The owner can read the image through its correct Study A path.
        mockMvc.perform(
                        get(
                                "/api/studies/{studyId}/assets/{assetId}/content",
                                studyA,
                                assetId
                        )
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andExpect(content().bytes(pngBytes));

        // Even the same owner cannot read A's asset through Study B.
        mockMvc.perform(
                        get(
                                "/api/studies/{studyId}/assets/{assetId}/content",
                                studyB,
                                assetId
                        )
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("ASSET_NOT_FOUND"));

        // Create an independent researcher B.
        Researcher researcherB = researcherRepository.saveAndFlush(
                new Researcher(
                        "asset-isolation-researcher-b@example.com",
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

        // Researcher B cannot read A's asset through its correct study path.
        mockMvc.perform(
                        get(
                                "/api/studies/{studyId}/assets/{assetId}/content",
                                studyA,
                                assetId
                        )
                                .header("Authorization", "Bearer " + tokenB)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("STUDY_NOT_FOUND"));

        // Verify that the legitimate owner can still retrieve the image.
        mockMvc.perform(
                        get(
                                "/api/studies/{studyId}/assets/{assetId}/content",
                                studyA,
                                assetId
                        )
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andExpect(content().bytes(pngBytes));
    }

    /** Verifies that foreign and unauthenticated users cannot upload assets to a study. */
    @Test
    void shouldRejectUnauthorizedAssetUploads() throws Exception {

        // Researcher A creates an owned draft study.
        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Asset Upload Authorization Test",
                                  "description": "FT-015",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        // Generate a valid image so rejection cannot be attributed to invalid bytes.
        BufferedImage image =
                new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertTrue(ImageIO.write(image, "png", output));

        byte[] pngBytes = output.toByteArray();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "authorization-test.png",
                "image/png",
                pngBytes
        );

        long assetCountBefore = studyAssetRepository.count();

        // Create an independent researcher B.
        Researcher researcherB = researcherRepository.saveAndFlush(
                new Researcher(
                        "asset-upload-researcher-b@example.com",
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

        // Researcher B must not upload an image to A's study.
        mockMvc.perform(
                        multipart("/api/studies/{studyId}/assets", studyId)
                                .file(file)
                                .header("Authorization", "Bearer " + tokenB)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("STUDY_NOT_FOUND"));

        // An unauthenticated user must also be rejected.
        mockMvc.perform(
                        multipart("/api/studies/{studyId}/assets", studyId)
                                .file(file)
                )
                .andExpect(status().isUnauthorized());

        // Neither rejected request may create an asset record.
        assertEquals(
                assetCountBefore,
                studyAssetRepository.count(),
                "Unauthorized uploads must not create asset records."
        );

        // Researcher A must still be able to upload the same valid PNG.
        mockMvc.perform(
                        multipart("/api/studies/{studyId}/assets", studyId)
                                .file(file)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.assetId").isNotEmpty())
                .andExpect(jsonPath("$.contentType").value("image/png"));

        assertEquals(
                assetCountBefore + 1,
                studyAssetRepository.count(),
                "Only the authorized upload should create an asset record."
        );
    }

    /** Verifies that publishing a study prevents any further image uploads. */
    @Test
    void shouldRejectAssetUploadAfterStudyPublication() throws Exception {

        // Create a draft study using a populated Facebook template.
        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Published Asset Upload Test",
                                  "description": "FT-016",
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

        // Retrieve the current STUDY version for publication.
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

        // Generate a valid PNG to ensure the rejection is caused by study status.
        BufferedImage image =
                new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertTrue(ImageIO.write(image, "png", output));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "after-publication.png",
                "image/png",
                output.toByteArray()
        );

        long assetCountBefore = studyAssetRepository.count();

        // Uploading to a published study must be rejected.
        mockMvc.perform(
                        multipart("/api/studies/{studyId}/assets", studyId)
                                .file(file)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("STUDY_NOT_EDITABLE"));

        // The rejected upload must not create an asset record.
        assertEquals(
                assetCountBefore,
                studyAssetRepository.count(),
                "Published studies must not accept new image assets."
        );

        // Verify that the study remains published.
        mockMvc.perform(
                        get("/api/studies/{studyId}", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COLLECTING"));
    }

    /** Verifies that a feed cannot reference an image belonging to another study. */
    @Test
    void shouldRejectCrossStudyImageAssetReference() throws Exception {

        // Create two independent studies owned by the same researcher.
        String responseA = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Asset Reference Study A",
                                  "description": "FT-017 source",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyA = JsonPath.read(responseA, "$.id");

        String responseB = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Asset Reference Study B",
                                  "description": "FT-017 destination",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyB = JsonPath.read(responseB, "$.id");

        // Generate and upload a real PNG exclusively to Study A.
        BufferedImage image =
                new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertTrue(ImageIO.write(image, "png", output));

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "study-a-image.png",
                "image/png",
                output.toByteArray()
        );

        String uploadResponse = mockMvc.perform(
                        multipart("/api/studies/{studyId}/assets", studyA)
                                .file(file)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String assetId = JsonPath.read(uploadResponse, "$.assetId");

        // Read Study B's independent feed version.
        String originalFeedB = mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyB)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number originalVersion = JsonPath.read(originalFeedB, "$.version");

        // Attempt to insert Study A's assetId into Study B's document.
        String invalidFeed = """
                {
                  "content": {
                    "ROOT": {
                      "type": {
                        "resolvedName": "ResearcherEditCanvas"
                      },
                      "isCanvas": true,
                      "props": {},
                      "nodes": ["foreignImage"],
                      "linkedNodes": {}
                    },
                    "foreignImage": {
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
                """.formatted(assetId, originalVersion.longValue());

        // The asset exists, but it belongs to the wrong study.
        mockMvc.perform(
                        put("/api/studies/{studyId}/feed", studyB)
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(invalidFeed)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("ASSET_REFERENCE_INVALID"));

        // Confirm that Study B's feed remains unchanged.
        mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyB)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content")
                        .value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.version")
                        .value(originalVersion.longValue()));

        // The original image must remain accessible through Study A.
        mockMvc.perform(
                        get(
                                "/api/studies/{studyId}/assets/{assetId}/content",
                                studyA,
                                assetId
                        )
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(output.toByteArray()));
    }

    /** Verifies that a feed can persist and retrieve a valid image belonging to its own study. */
    @Test
    void shouldSaveFeedWithOwnedImageAssetReference() throws Exception {

        // Create a draft study.
        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Valid Image Reference Test",
                                  "description": "FT-018",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        // Generate and upload a valid PNG to this study.
        BufferedImage image =
                new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertTrue(ImageIO.write(image, "png", output));

        byte[] pngBytes = output.toByteArray();

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "owned-image.png",
                "image/png",
                pngBytes
        );

        String uploadResponse = mockMvc.perform(
                        multipart("/api/studies/{studyId}/assets", studyId)
                                .file(file)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String assetId = JsonPath.read(uploadResponse, "$.assetId");

        // Retrieve the current feed version.
        String initialFeed = mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number initialVersion = JsonPath.read(initialFeed, "$.version");

        // Reference the uploaded image using its assetId.
        String saveRequest = """
                {
                  "content": {
                    "ROOT": {
                      "type": {
                        "resolvedName": "ResearcherEditCanvas"
                      },
                      "isCanvas": true,
                      "props": {},
                      "nodes": ["ownedImage"],
                      "linkedNodes": {}
                    },
                    "ownedImage": {
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
                """.formatted(assetId, initialVersion.longValue());

        // A valid same-study image reference must be accepted.
        String savedFeed = mockMvc.perform(
                        put("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(saveRequest)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.ownedImage.props.assetId")
                        .value(assetId))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number savedVersion = JsonPath.read(savedFeed, "$.version");

        assertTrue(
                savedVersion.longValue() > initialVersion.longValue(),
                "Saving the feed must advance its version."
        );

        // Retrieve the feed again to verify that the reference was persisted.
        mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.ROOT.nodes[0]")
                        .value("ownedImage"))
                .andExpect(jsonPath("$.content.ownedImage.props.assetId")
                        .value(assetId))
                .andExpect(jsonPath("$.version")
                        .value(savedVersion.longValue()));

        // Verify that the referenced image remains accessible to its owner.
        mockMvc.perform(
                        get(
                                "/api/studies/{studyId}/assets/{assetId}/content",
                                studyId,
                                assetId
                        )
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(pngBytes));
    }

    /** Verifies that image validation uses decoded bytes rather than trusting the MIME header. */
    @Test
    void shouldValidateActualImageFormatAndFilename() throws Exception {

        // Create an owned draft study.
        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Image Format Validation Test",
                                  "description": "FT-019",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        // Generate a genuine PNG image.
        BufferedImage image =
                new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertTrue(ImageIO.write(image, "png", output));

        byte[] pngBytes = output.toByteArray();

        long assetCountBefore = studyAssetRepository.count();

        // The actual PNG format conflicts with the .jpg filename extension.
        MockMultipartFile wrongExtension = new MockMultipartFile(
                "file",
                "wrong.jpg",
                "image/jpeg",
                pngBytes
        );

        mockMvc.perform(
                        multipart("/api/studies/{studyId}/assets", studyId)
                                .file(wrongExtension)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("ASSET_INVALID_IMAGE"));

        assertEquals(
                assetCountBefore,
                studyAssetRepository.count(),
                "An extension mismatch must not create an asset record."
        );

        // The filename and actual bytes are PNG, but the client lies about MIME.
        MockMultipartFile misleadingMime = new MockMultipartFile(
                "file",
                "actual.png",
                "image/jpeg",
                pngBytes
        );

        // MIME is advisory: the server should detect and report the actual PNG type.
        String uploadResponse = mockMvc.perform(
                        multipart("/api/studies/{studyId}/assets", studyId)
                                .file(misleadingMime)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.contentType").value("image/png"))
                .andExpect(jsonPath("$.filename").value("actual.png"))
                .andExpect(jsonPath("$.width").value(2))
                .andExpect(jsonPath("$.height").value(2))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String assetId = JsonPath.read(uploadResponse, "$.assetId");

        // The retrieved content must retain the server-detected image type.
        mockMvc.perform(
                        get(
                                "/api/studies/{studyId}/assets/{assetId}/content",
                                studyId,
                                assetId
                        )
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(pngBytes));

        // Only the valid image upload should create an asset record.
        assertEquals(
                assetCountBefore + 1,
                studyAssetRepository.count()
        );
    }

    /** Verifies that uploaded image filenames are sanitized against path traversal. */
    @Test
    void shouldSanitizeUploadedImageFilename() throws Exception {

        // Create an owned draft study.
        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Asset Filename Security Test",
                                  "description": "FT-020",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        // Generate a valid PNG.
        BufferedImage image =
                new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertTrue(ImageIO.write(image, "png", output));

        byte[] pngBytes = output.toByteArray();

        // Test both Unix-style and Windows-style traversal paths.
        String[] unsafeFilenames = {
                "../../outside.png",
                "..\\..\\outside.png"
        };

        long assetCountBefore = studyAssetRepository.count();

        for (String unsafeFilename : unsafeFilenames) {

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    unsafeFilename,
                    "image/png",
                    pngBytes
            );

            // The service should strip path components from the filename.
            String uploadResponse = mockMvc.perform(
                            multipart("/api/studies/{studyId}/assets", studyId)
                                    .file(file)
                                    .header(
                                            "Authorization",
                                            "Bearer " + researcherToken
                                    )
                    )
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.filename").value("outside.png"))
                    .andExpect(jsonPath("$.contentType").value("image/png"))
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            String assetId = JsonPath.read(uploadResponse, "$.assetId");

            // The response must expose only the generated asset URL.
            String expectedUrl =
                    "/api/studies/" + studyId
                            + "/assets/" + assetId
                            + "/content";

            assertEquals(
                    expectedUrl,
                    JsonPath.read(uploadResponse, "$.contentUrl")
            );

            // Verify that the image remains accessible through its asset ID.
            mockMvc.perform(
                            get(
                                    "/api/studies/{studyId}/assets/{assetId}/content",
                                    studyId,
                                    assetId
                            )
                                    .header(
                                            "Authorization",
                                            "Bearer " + researcherToken
                                    )
                    )
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.IMAGE_PNG))
                    .andExpect(content().bytes(pngBytes));
        }

        // Both sanitized uploads should create independent asset records.
        assertEquals(
                assetCountBefore + 2,
                studyAssetRepository.count(),
                "Both images should be stored under independent asset IDs."
        );
    }

    /** Verifies that an image exceeding the configured dimension limit is rejected. */
    @Test
    void shouldRejectImageExceedingDimensionLimit() throws Exception {

        // Create an owned draft study.
        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Image Dimension Limit Test",
                                  "description": "FT-021",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        long assetCountBefore = studyAssetRepository.count();

        // Generate a genuine PNG whose width exceeds the 10,000-pixel limit.
        BufferedImage image =
                new BufferedImage(10001, 1, BufferedImage.TYPE_INT_RGB);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertTrue(ImageIO.write(image, "png", output));

        byte[] pngBytes = output.toByteArray();

        image.flush();

        // Ensure the file itself is below 5 MiB.
        // This isolates dimension validation from the file-size limit.
        assertTrue(
                pngBytes.length <= 5 * 1024 * 1024,
                "The test image must remain below the upload size limit."
        );

        MockMultipartFile oversizedDimensions = new MockMultipartFile(
                "file",
                "oversized-dimensions.png",
                "image/png",
                pngBytes
        );

        // The server must reject the decoded image dimensions.
        mockMvc.perform(
                        multipart("/api/studies/{studyId}/assets", studyId)
                                .file(oversizedDimensions)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("ASSET_INVALID_IMAGE"));

        // Rejected images must not create database records.
        assertEquals(
                assetCountBefore,
                studyAssetRepository.count(),
                "An image exceeding dimension limits must not be persisted."
        );
    }

    /** Verifies that anonymous participants can only read images referenced by a published feed. */
    @Test
    void shouldRestrictParticipantImageAccessToPublishedFeedReferences() throws Exception {

        // Create a draft study.
        String createResponse = mockMvc.perform(
                        post("/api/studies")
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Participant Image Access Test",
                                  "description": "FT-022",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String studyId = JsonPath.read(createResponse, "$.id");

        // Generate a real PNG.
        BufferedImage image =
                new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);

        ByteArrayOutputStream output = new ByteArrayOutputStream();

        assertTrue(ImageIO.write(image, "png", output));

        byte[] pngBytes = output.toByteArray();

        image.flush();

        // Upload two independent assets to the same study.
        String[] assetIds = new String[2];

        for (int i = 0; i < 2; i++) {

            MockMultipartFile file = new MockMultipartFile(
                    "file",
                    "participant-image-" + i + ".png",
                    "image/png",
                    pngBytes
            );

            String uploadResponse = mockMvc.perform(
                            multipart("/api/studies/{studyId}/assets", studyId)
                                    .file(file)
                                    .header(
                                            "Authorization",
                                            "Bearer " + researcherToken
                                    )
                    )
                    .andExpect(status().isCreated())
                    .andReturn()
                    .getResponse()
                    .getContentAsString();

            assetIds[i] = JsonPath.read(uploadResponse, "$.assetId");
        }

        String referencedAssetId = assetIds[0];
        String unreferencedAssetId = assetIds[1];

        assertNotEquals(referencedAssetId, unreferencedAssetId);

        // Read the current feed version.
        String initialFeed = mockMvc.perform(
                        get("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number feedVersion = JsonPath.read(initialFeed, "$.version");

        // Save a feed referencing ONLY the first image.
        String saveRequest = """
                {
                  "content": {
                    "ROOT": {
                      "type": {
                        "resolvedName": "ResearcherEditCanvas"
                      },
                      "isCanvas": true,
                      "props": {},
                      "nodes": ["visibleImage"],
                      "linkedNodes": {}
                    },
                    "visibleImage": {
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
                referencedAssetId,
                feedVersion.longValue()
        );

        mockMvc.perform(
                        put("/api/studies/{studyId}/feed", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(saveRequest)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.visibleImage.props.assetId")
                        .value(referencedAssetId));

        // Retrieve the STUDY version for publication.
        String studyResponse = mockMvc.perform(
                        get("/api/studies/{studyId}", studyId)
                                .header("Authorization", "Bearer " + researcherToken)
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Number studyVersion = JsonPath.read(studyResponse, "$.version");

        // Publish the study and obtain its participation link.
        String publicationResponse = mockMvc.perform(
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
                .andExpect(jsonPath("$.status").value("COLLECTING"))
                .andExpect(jsonPath("$.participationUrl").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String participationUrl =
                JsonPath.read(publicationResponse, "$.participationUrl");

        String token = participationUrl.substring(
                participationUrl.lastIndexOf('/') + 1
        );

        assertTrue(token.matches("[A-Za-z0-9_-]{43}"));

        // A participant with the valid token can retrieve the referenced image.
        // No researcher JWT is supplied.
        mockMvc.perform(
                        get(
                                "/api/participation/{token}/assets/{assetId}/content",
                                token,
                                referencedAssetId
                        )
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(pngBytes))
                .andExpect(header().string(
                        "X-Content-Type-Options",
                        "nosniff"
                ));

        // The same token must not expose an uploaded but unreferenced asset.
        mockMvc.perform(
                        get(
                                "/api/participation/{token}/assets/{assetId}/content",
                                token,
                                unreferencedAssetId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("ASSET_NOT_FOUND"));

        // An unknown participation token must not expose the referenced image.
        mockMvc.perform(
                        get(
                                "/api/participation/{token}/assets/{assetId}/content",
                                "A".repeat(43),
                                referencedAssetId
                        )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("PARTICIPATION_NOT_FOUND"));
    }
}
