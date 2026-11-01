package com.cs_42_3.surveyplatformbackend.study;

import com.cs_42_3.surveyplatformbackend.TestcontainersConfiguration;
import com.cs_42_3.surveyplatformbackend.researcher.domain.Researcher;
import com.cs_42_3.surveyplatformbackend.researcher.repository.ResearcherRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.*;
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
import java.nio.file.*;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real commits: an outer test transaction would conceal rollback and locking defects. */
@Testcontainers
@SpringBootTest(properties = {
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.assets.cleanup.scheduler-enabled=false"
})
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Timeout(60)
@org.junit.jupiter.api.parallel.Execution(org.junit.jupiter.api.parallel.ExecutionMode.SAME_THREAD)
class StudyLifecycleAtomicityIntegrationTest {
    static final Path ROOT = temporaryRoot();
    @DynamicPropertySource
    static void storage(DynamicPropertyRegistry properties) {
        properties.add("app.assets.storage-directory", ROOT::toString);
    }
    static Path temporaryRoot() {
        try { return Files.createTempDirectory("m2-deletion-atomicity-"); }
        catch (IOException e) { throw new UncheckedIOException(e); }
    }
    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired ResearcherRepository researchers;
    @Autowired JwtEncoder encoder;
    @Autowired com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository studies;
    @Autowired com.cs_42_3.surveyplatformbackend.participation.repository.ParticipantSessionRepository sessions;
    private UUID study;
    private UUID asset;
    private String token;
    private long version;

    @BeforeEach
    void setUp() throws Exception {
        var owner = researchers.saveAndFlush(new Researcher(
                "m2-atomic-" + UUID.randomUUID() + "@example.com", "test-password-hash"));
        Instant now = Instant.now();
        token = encoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder()
                .subject(owner.getId().toString()).issuedAt(now).expiresAt(now.plusSeconds(3600))
                .claim("role", "RESEARCHER").build())).getTokenValue();
        String body = mvc.perform(post("/api/studies").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"M2 atomicity\",\"templateCode\":\"blank\"}"))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        study = UUID.fromString(JsonPath.read(body, "$.id"));
        var png = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", png));
        String uploaded = mvc.perform(multipart("/api/studies/{id}/assets", study)
                        .header("Authorization", bearer())
                        .file(new MockMultipartFile("file", "retained.png", "image/png", png.toByteArray())))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        asset = UUID.fromString(JsonPath.read(uploaded, "$.assetId"));
        version = jdbc.queryForObject("SELECT lock_version FROM studies WHERE id = ?", Long.class, study);
    }

    @AfterEach
    void removeFault() {
        jdbc.execute("DROP TRIGGER IF EXISTS m2_test_delete_failure ON studies");
        jdbc.execute("DROP FUNCTION IF EXISTS m2_test_delete_failure()");
        jdbc.execute("DROP TRIGGER IF EXISTS m2_test_close_failure ON studies");
        jdbc.execute("DROP FUNCTION IF EXISTS m2_test_close_failure()");
    }

    @AfterAll
    static void cleanFiles() throws Exception {
        try (var paths = Files.walk(ROOT)) {
            for (var p : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(p);
        }
    }

    @Test
    void aLateDatabaseFailureRestoresStudyFeedAssetsAndCleanupQueue() throws Exception {
        Map<String, String> before = rows();
        String storageKey = jdbc.queryForObject("SELECT storage_key FROM study_assets WHERE id = ?", String.class, asset);
        byte[] image = Files.readAllBytes(ROOT.resolve(storageKey));
        // Fail only this fixture, after removeDependencies has already executed its DELETEs/INSERTs.
        jdbc.execute("CREATE FUNCTION m2_test_delete_failure() RETURNS trigger LANGUAGE plpgsql AS $$ "
                + "BEGIN IF OLD.id = '" + study + "'::uuid THEN "
                + "RAISE EXCEPTION 'M2_TEST_FORCED_LATE_DELETE_FAILURE'; END IF; RETURN OLD; END $$");
        jdbc.execute("CREATE TRIGGER m2_test_delete_failure BEFORE DELETE ON studies "
                + "FOR EACH ROW EXECUTE FUNCTION m2_test_delete_failure()");
        boolean failed = false;
        try {
            int response = deletion().getResponse().getStatus();
            failed = response >= 500;
        } catch (jakarta.servlet.ServletException failure) {
            assertTrue(causes(failure).contains("M2_TEST_FORCED_LATE_DELETE_FAILURE"),
                    "Failure must come from the injected database fault");
            failed = true;
        }
        assertTrue(failed, "Injected failure must not report deletion success");
        assertEquals(before, rows(), "Partial dependency cleanup must roll back");
        assertArrayEquals(image, Files.readAllBytes(ROOT.resolve(storageKey)));
        mvc.perform(get("/api/studies/{id}/assets/{asset}/content", study, asset)
                        .header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(content().bytes(image));
        removeFault();
        assertEquals(204, deletion().getResponse().getStatus(), "Retry works after removing the fault");
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM studies WHERE id = ?", Long.class, study));
        assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM study_assets WHERE id = ?", Long.class, asset));
    }

    @Test
    void simultaneousDeletesCommitOnceAndDoNotDuplicateAssetCleanup() throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        Callable<Integer> attempt = () -> {
            ready.countDown();
            assertTrue(start.await(10, TimeUnit.SECONDS));
            return deletion().getResponse().getStatus();
        };
        try {
            var first = executor.submit(attempt);
            var second = executor.submit(attempt);
            assertTrue(ready.await(10, TimeUnit.SECONDS));
            start.countDown();
            var results = new ArrayList<>(List.of(first.get(20, TimeUnit.SECONDS), second.get(20, TimeUnit.SECONDS)));
            Collections.sort(results);
            assertEquals(List.of(204, 404), results);
            assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM study_feeds WHERE study_id = ?", Long.class, study));
            assertEquals(0L, jdbc.queryForObject("SELECT count(*) FROM study_assets WHERE id = ?", Long.class, asset));
            assertEquals(1L, jdbc.queryForObject("SELECT count(*) FROM asset_file_cleanup_tasks WHERE id = ?", Long.class, asset));
        } finally {
            start.countDown(); executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    @Test
    void closureFailureRestoresBothStudyAndActiveSession() throws Exception {
        UUID session = publishWithActiveSession();
        Map<String, String> before = rows();
        String sessionBefore = sessionRow(session);
        jdbc.execute("CREATE FUNCTION m2_test_close_failure() RETURNS trigger LANGUAGE plpgsql AS $$ "
                + "BEGIN IF NEW.id = '" + study + "'::uuid AND NEW.status = 'CLOSED' THEN "
                + "RAISE EXCEPTION 'M2_TEST_FORCED_CLOSE_FAILURE'; END IF; RETURN NEW; END $$");
        jdbc.execute("CREATE TRIGGER m2_test_close_failure BEFORE UPDATE ON studies "
                + "FOR EACH ROW EXECUTE FUNCTION m2_test_close_failure()");
        boolean failed = false;
        try { failed = closeOnce().getResponse().getStatus() >= 500; }
        catch (jakarta.servlet.ServletException failure) {
            assertTrue(causes(failure).contains("M2_TEST_FORCED_CLOSE_FAILURE"));
            failed = true;
        }
        assertTrue(failed);
        assertEquals(before, rows());
        assertEquals(sessionBefore, sessionRow(session));
        removeFault();
        assertEquals(200, closeOnce().getResponse().getStatus());
        assertEquals("CLOSED", jdbc.queryForObject("SELECT status FROM studies WHERE id = ?", String.class, study));
        assertEquals("ABANDONED", jdbc.queryForObject("SELECT status FROM participant_sessions WHERE id = ?", String.class, session));
    }

    @Test
    void concurrentClosuresAreIdempotentAndTerminateTheSessionOnce() throws Exception {
        UUID session = publishWithActiveSession();
        ExecutorService executor = Executors.newFixedThreadPool(2);
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        Callable<Integer> attempt = () -> {
            ready.countDown();
            assertTrue(start.await(10, TimeUnit.SECONDS));
            return closeOnce().getResponse().getStatus();
        };
        try {
            var first = executor.submit(attempt);
            var second = executor.submit(attempt);
            assertTrue(ready.await(10, TimeUnit.SECONDS)); start.countDown();
            assertEquals(200, first.get(20, TimeUnit.SECONDS));
            assertEquals(200, second.get(20, TimeUnit.SECONDS));
            assertEquals(version + 1, jdbc.queryForObject("SELECT lock_version FROM studies WHERE id = ?", Long.class, study));
            assertEquals("ABANDONED", jdbc.queryForObject("SELECT status FROM participant_sessions WHERE id = ?", String.class, session));
            Map<String, String> closed = rows();
            String terminated = sessionRow(session);
            assertEquals(200, closeOnce().getResponse().getStatus());
            assertEquals(closed, rows());
            assertEquals(terminated, sessionRow(session));
        } finally {
            start.countDown(); executor.shutdownNow();
            assertTrue(executor.awaitTermination(10, TimeUnit.SECONDS));
        }
    }

    private UUID publishWithActiveSession() {
        var entity = studies.findById(study).orElseThrow();
        entity.publish(UUID.randomUUID().toString().replace("-", "") + "abcdefghijk", Instant.now(), "test-consent-v1");
        entity = studies.saveAndFlush(entity);
        version = entity.getLockVersion();
        Instant now = Instant.now();
        var session = com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSession.create(
                study, UUID.randomUUID(), UUID.randomUUID().toString().replace("-", "").repeat(2), null, now);
        session.decideConsent(true, false, now);
        return sessions.saveAndFlush(session).getId();
    }

    private String sessionRow(UUID id) {
        return jdbc.queryForObject("SELECT row_to_json(s)::text FROM participant_sessions s WHERE id = ?", String.class, id);
    }

    private org.springframework.test.web.servlet.MvcResult closeOnce() throws Exception {
        return mvc.perform(post("/api/studies/{id}/close", study).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"version\":" + version + "}"))
                .andReturn();
    }

    private Map<String, String> rows() {
        var result = new LinkedHashMap<String, String>();
        result.put("study", jdbc.queryForObject("SELECT row_to_json(s)::text FROM studies s WHERE id = ?", String.class, study));
        result.put("feed", jdbc.queryForObject("SELECT row_to_json(f)::text FROM study_feeds f WHERE study_id = ?", String.class, study));
        result.put("asset", jdbc.queryForObject("SELECT row_to_json(a)::text FROM study_assets a WHERE id = ?", String.class, asset));
        result.put("cleanup", jdbc.queryForObject("SELECT count(*)::text FROM asset_file_cleanup_tasks WHERE id = ?", String.class, asset));
        return result;
    }
    private String causes(Throwable failure) {
        var result = new StringBuilder();
        for (var t = failure; t != null; t = t.getCause()) result.append(t.getMessage()).append('\n');
        return result.toString();
    }
    private org.springframework.test.web.servlet.MvcResult deletion() throws Exception {
        return mvc.perform(delete("/api/studies/{id}", study).param("version", Long.toString(version))
                .header("Authorization", bearer())).andReturn();
    }
    private String bearer() { return "Bearer " + token; }
}
