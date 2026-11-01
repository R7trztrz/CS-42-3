package com.cs_42_3.surveyplatformbackend.asset;

import com.cs_42_3.surveyplatformbackend.TestcontainersConfiguration;
import com.cs_42_3.surveyplatformbackend.researcher.domain.Researcher;
import com.cs_42_3.surveyplatformbackend.researcher.repository.ResearcherRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.zip.CRC32;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Valid image bytes at the real endpoint; isolated PostgreSQL and file storage. */
@Testcontainers
@SpringBootTest(properties = {
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.assets.cleanup.scheduler-enabled=false"
})
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class StudyAssetBoundaryIntegrationTest {
    private static final int LIMIT = 5 * 1024 * 1024;
    private static final Path ROOT = temporaryRoot();
    @DynamicPropertySource
    static void files(DynamicPropertyRegistry properties) {
        properties.add("app.assets.storage-directory", ROOT::toString);
    }
    static Path temporaryRoot() {
        try { return Files.createTempDirectory("m3-asset-boundary-"); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
    @Autowired MockMvc mvc;
    @Autowired ResearcherRepository researchers;
    @Autowired JwtEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    private String token;
    private UUID study;

    @BeforeEach
    void setUp() throws Exception {
        var owner = researchers.saveAndFlush(new Researcher(
                "m3-boundary-" + UUID.randomUUID() + "@example.com", "test-password-hash"));
        Instant now = Instant.now();
        token = encoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder()
                .subject(owner.getId().toString()).issuedAt(now).expiresAt(now.plusSeconds(3600))
                .claim("role", "RESEARCHER").build())).getTokenValue();
        String response = mvc.perform(post("/api/studies").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"M3 boundary\",\"templateCode\":\"blank\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        study = UUID.fromString(JsonPath.read(response, "$.id"));
    }

    @AfterAll
    static void cleanFiles() throws Exception {
        try (var paths = Files.walk(ROOT)) {
            for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
        }
    }

    @ParameterizedTest
    @ValueSource(ints = {LIMIT - 1, LIMIT})
    void acceptsValidPngAtAndImmediatelyBelowFiveMiB(int size) throws Exception {
        byte[] bytes = pngWithTextChunk(size);
        assertNotNull(ImageIO.read(new ByteArrayInputStream(bytes)), "Fixture must be decodable");
        long before = assets();
        String response = mvc.perform(multipart("/api/studies/{id}/assets", study)
                        .header("Authorization", bearer())
                        .file(new MockMultipartFile("file", "boundary.png", "image/png", bytes)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.sizeBytes").value(size))
                .andExpect(jsonPath("$.width").value(2)).andExpect(jsonPath("$.height").value(2))
                .andReturn().getResponse().getContentAsString();
        assertEquals(before + 1, assets());
        String assetId = JsonPath.read(response, "$.assetId");
        mvc.perform(get("/api/studies/{id}/assets/{asset}/content", study, assetId)
                        .header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(content().bytes(bytes));
    }

    @Test
    void rejectsARealPngOneByteOverTheLimitWithoutDatabaseOrFileSideEffects() throws Exception {
        byte[] bytes = pngWithTextChunk(LIMIT + 1);
        assertNotNull(ImageIO.read(new ByteArrayInputStream(bytes)));
        long before = assets();
        Set<String> files = storedFiles();
        mvc.perform(multipart("/api/studies/{id}/assets", study).header("Authorization", bearer())
                        .file(new MockMultipartFile("file", "over.png", "image/png", bytes)))
                .andExpect(status().isPayloadTooLarge()).andExpect(jsonPath("$.code").value("ASSET_TOO_LARGE"));
        assertEquals(before, assets());
        assertEquals(files, storedFiles());
    }

    @Test
    void acceptsRealWebpAndReturnsItsOriginalBytes() throws Exception {
        byte[] bytes = Base64.getDecoder().decode("UklGRh4AAABXRUJQVlA4TBEAAAAvAUAAAAdQslKUtv+BiOh/AAA=");
        assertNotNull(ImageIO.read(new ByteArrayInputStream(bytes)), "The app must have its WebP decoder installed");
        String response = mvc.perform(multipart("/api/studies/{id}/assets", study)
                        .header("Authorization", bearer())
                        .file(new MockMultipartFile("file", "sample.webp", "image/webp", bytes)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.contentType").value("image/webp"))
                .andExpect(jsonPath("$.width").value(2)).andExpect(jsonPath("$.height").value(2))
                .andReturn().getResponse().getContentAsString();
        String assetId = JsonPath.read(response, "$.assetId");
        mvc.perform(get("/api/studies/{id}/assets/{asset}/content", study, assetId)
                        .header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(content().contentType("image/webp"))
                .andExpect(content().bytes(bytes));
    }

    @Test
    void reusesAnAssetAcrossWidgetsAndRemovingOneReferencePreservesTheOther() throws Exception {
        var output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", output));
        byte[] bytes = output.toByteArray();
        String response = mvc.perform(multipart("/api/studies/{id}/assets", study)
                        .header("Authorization", bearer())
                        .file(new MockMultipartFile("file", "shared.png", "image/png", bytes)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String assetId = JsonPath.read(response, "$.assetId");
        saveReferences(assetId, true);
        mvc.perform(get("/api/studies/{id}/feed", study).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.image1.props.assetId").value(assetId))
                .andExpect(jsonPath("$.content.image2.props.assetId").value(assetId));
        saveReferences(assetId, false);
        mvc.perform(get("/api/studies/{id}/feed", study).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.image1").doesNotExist())
                .andExpect(jsonPath("$.content.ROOT.nodes[0]").value("image2"))
                .andExpect(jsonPath("$.content.image2.props.assetId").value(assetId));
        assertEquals(1, assets());
        mvc.perform(get("/api/studies/{id}/assets/{asset}/content", study, assetId)
                        .header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(content().bytes(bytes));
    }

    private void saveReferences(String assetId, boolean includeFirst) throws Exception {
        String current = mvc.perform(get("/api/studies/{id}/feed", study).header("Authorization", bearer()))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Number version = JsonPath.read(current, "$.version");
        String first = includeFirst ? "\"image1\":" + imageNode(assetId) + "," : "";
        String nodes = includeFirst ? "\"image1\",\"image2\"" : "\"image2\"";
        String body = "{\"version\":" + version.longValue() + ",\"content\":{\"ROOT\":{"
                + "\"type\":{\"resolvedName\":\"ResearcherEditCanvas\"},\"isCanvas\":true,"
                + "\"props\":{},\"nodes\":[" + nodes + "],\"linkedNodes\":{}},"
                + first + "\"image2\":" + imageNode(assetId) + "}}";
        mvc.perform(put("/api/studies/{id}/feed", study).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
    }

    private String imageNode(String assetId) {
        return "{\"type\":{\"resolvedName\":\"ImageWidget\"},\"isCanvas\":false,"
                + "\"props\":{\"assetId\":\"" + assetId + "\",\"styleId\":\"facebook\"},"
                + "\"parent\":\"ROOT\",\"nodes\":[],\"linkedNodes\":{}}";
    }

    private long assets() {
        return jdbc.queryForObject("SELECT count(*) FROM study_assets WHERE study_id = ?", Long.class, study);
    }
    private Set<String> storedFiles() throws IOException {
        try (var paths = Files.walk(ROOT)) {
            return paths.filter(Files::isRegularFile).map(p -> ROOT.relativize(p).toString())
                    .collect(java.util.stream.Collectors.toSet());
        }
    }
    private String bearer() { return "Bearer " + token; }

    /** Add a valid ancillary tEXt chunk before IEND, with correct size and CRC. */
    private byte[] pngWithTextChunk(int totalBytes) throws Exception {
        var base = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", base));
        byte[] png = base.toByteArray();
        byte[] text = new byte[totalBytes - png.length - 12];
        Arrays.fill(text, (byte) 'a');
        text[0] = 'k'; text[1] = 0;
        byte[] type = "tEXt".getBytes(StandardCharsets.US_ASCII);
        var crc = new CRC32(); crc.update(type); crc.update(text);
        var result = new ByteArrayOutputStream(totalBytes);
        var data = new DataOutputStream(result);
        data.write(png, 0, png.length - 12);
        data.writeInt(text.length); data.write(type); data.write(text); data.writeInt((int) crc.getValue());
        data.write(png, png.length - 12, 12);
        assertEquals(totalBytes, result.size());
        return result.toByteArray();
    }
}
