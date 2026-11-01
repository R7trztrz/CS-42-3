package com.cs_42_3.surveyplatformbackend.export;

import com.cs_42_3.surveyplatformbackend.TestcontainersConfiguration;
import com.cs_42_3.surveyplatformbackend.export.service.*;
import com.cs_42_3.surveyplatformbackend.export.storage.ExportStorage;
import com.cs_42_3.surveyplatformbackend.participation.domain.ParticipantSession;
import com.cs_42_3.surveyplatformbackend.participation.repository.ParticipantSessionRepository;
import com.cs_42_3.surveyplatformbackend.researcher.domain.Researcher;
import com.cs_42_3.surveyplatformbackend.researcher.repository.ResearcherRepository;
import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import org.apache.commons.csv.CSVFormat;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.AopTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;
import tools.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * M7 controlled backend regression tests. Uses Testcontainers, never the local app database.
 * The failure case corrupts only its own disposable fixture. No business code changes.
 * Performance is opt-in: -Dsurvey.performance.tests=true (or the existing -Pperformance).
 */
@Testcontainers
@SpringBootTest(properties = {
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.exports.scheduler-enabled=false"
})
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@Timeout(180)
class M7ExportIntegrationTest {
    private static final Path ARTIFACT_ROOT = temporaryRoot();

    @DynamicPropertySource
    static void isolatedFiles(DynamicPropertyRegistry properties) {
        properties.add("app.exports.storage-directory", ARTIFACT_ROOT::toString);
    }

    private static Path temporaryRoot() {
        try { return Files.createTempDirectory("m7-export-integration-"); }
        catch (IOException failure) { throw new UncheckedIOException(failure); }
    }

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired ResearcherRepository researchers;
    @Autowired StudyRepository studies;
    @Autowired ParticipantSessionRepository sessions;
    @Autowired JwtEncoder encoder;
    @Autowired JdbcTemplate jdbc;
    @Autowired ExportWorker worker;
    @Autowired ExportCleanupService cleanup;
    @Autowired ExportStorage storage;
    @MockitoSpyBean Clock clock;
    @MockitoSpyBean SessionExportWriter sessionWriter;

    private UUID studyId;
    private String token;
    private final List<UUID> jobs = new ArrayList<>();

    @BeforeEach
    void setUp() {
        var owner = researchers.saveAndFlush(new Researcher(
                "m7-" + UUID.randomUUID() + "@example.com", "test-password-hash"));
        var study = new Study(owner.getId(), "M7 isolated export test", "Disposable test fixture");
        study.publish(UUID.randomUUID().toString().replace("-", "") + "abcdefghijk",
                Instant.now(), "m7-test-consent");
        studyId = studies.saveAndFlush(study).getId();
        Instant now = Instant.now();
        token = encoder.encode(JwtEncoderParameters.from(JwtClaimsSet.builder()
                .subject(owner.getId().toString()).issuedAt(now).expiresAt(now.plusSeconds(3600))
                .claim("role", "RESEARCHER").build())).getTokenValue();
    }

    @AfterEach
    void removePrivateArtifacts() throws Exception {
        // Do not leave PENDING/RUNNING jobs to affect the next test's queue selection.
        for (UUID id : jobs) storage.delete(id);
        jdbc.update("DELETE FROM export_file_cleanup_tasks WHERE export_id IN "
                + "(SELECT id FROM export_jobs WHERE study_id = ?)", studyId);
        jdbc.update("DELETE FROM export_jobs WHERE study_id = ?", studyId);
        jobs.clear();
    }

    @Test
    void invalidPersistedBatchFailsWithSessionIdAndNewExportWorksAfterRepair() throws Exception {
        UUID session = newSession();
        UUID batch = addBatch(session, 1, 1);
        UUID failed = create("BEHAVIOR");
        process();
        mvc.perform(get(jobUrl(failed)).header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.failedSessionId").value(session.toString()));
        mvc.perform(get(jobUrl(failed) + "/download").header("Authorization", bearer()))
                .andExpect(status().isConflict());
        assertEquals(1L, jdbc.queryForObject(
                "SELECT count(*) FROM export_file_cleanup_tasks WHERE export_id = ?", Long.class, failed));
        assertTrue(cleanup.processNext());
        addEvents(session, batch, 1);
        UUID replacement = create("BEHAVIOR");
        assertNotEquals(failed, replacement);
        process();
        assertEquals(1, csv(download(replacement), "behavior_events.csv").size());
        mvc.perform(get(jobUrl(failed)).header("Authorization", bearer()))
                .andExpect(jsonPath("$.status").value("FAILED"));
    }

    @Test
    void expiredArchiveIsRejectedCleanedAndCanBeRegenerated() throws Exception {
        UUID id = create("QUESTIONNAIRE");
        process();
        byte[] original = download(id);
        assertTrue(original.length > 0);
        String metadata = mvc.perform(get(jobUrl(id)).header("Authorization", bearer()))
                .andReturn().getResponse().getContentAsString();
        Instant expiry = Instant.parse(mapper.readTree(metadata).get("expiresAt").asString());
        doReturn(expiry.plusSeconds(1)).when(clock).instant();
        try {
            mvc.perform(get(jobUrl(id)).header("Authorization", bearer()))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EXPIRED"));
            mvc.perform(get(jobUrl(id) + "/download").header("Authorization", bearer()))
                    .andExpect(status().isGone());
            assertTrue(worker.expireNext());
            assertTrue(cleanup.processNext());
            assertThrows(IOException.class, () -> { try (var input = storage.open(id)) { input.read(); } });
        } finally {
            doCallRealMethod().when(clock).instant();
        }
        UUID replacement = create("QUESTIONNAIRE");
        process();
        assertNotEquals(id, replacement);
        assertTrue(download(replacement).length > 0);
    }

    @Test
    void missingArchiveReturnsGoneAndReplacementDownloads() throws Exception {
        UUID id = create("QUESTIONNAIRE");
        process();
        storage.delete(id);
        mvc.perform(get(jobUrl(id) + "/download").header("Authorization", bearer()))
                .andExpect(status().isGone());
        UUID replacement = create("QUESTIONNAIRE");
        process();
        assertTrue(download(replacement).length > 0);
    }

    @Test
    void pendingAndRunningJobsCannotBeDownloaded() throws Exception {
        UUID id = create("QUESTIONNAIRE");
        mvc.perform(get(jobUrl(id) + "/download").header("Authorization", bearer()))
                .andExpect(status().isConflict());
        assertTrue(worker.startNext());
        mvc.perform(get(jobUrl(id)).header("Authorization", bearer()))
                .andExpect(jsonPath("$.status").value("RUNNING"));
        mvc.perform(get(jobUrl(id) + "/download").header("Authorization", bearer()))
                .andExpect(status().isConflict());
        assertTrue(worker.processNext());
        assertTrue(download(id).length > 0);
    }

    @Test
    void concurrentCommitIsExcludedFromCurrentSnapshotButIncludedInNextExport() throws Exception {
        newSession();
        newSession();
        List<UUID> ordered = jdbc.query("SELECT id FROM participant_sessions WHERE study_id = ? ORDER BY id",
                (rs, row) -> rs.getObject(1, UUID.class), studyId);
        UUID later = ordered.get(1);
        var readStarted = new CountDownLatch(1);
        var writerCommitted = new CountDownLatch(1);
        var armed = new AtomicBoolean(true);
        // Stub the actual Mockito spy, bypassing only the proxy during setup.
        // Real export calls still enter through the Spring transaction proxy.
        SessionExportWriter writerSpy = AopTestUtils.getUltimateTargetObject(sessionWriter);
        doAnswer(call -> {
            Object value = call.callRealMethod();
            if (armed.compareAndSet(true, false)) {
                readStarted.countDown();
                assertTrue(writerCommitted.await(30, TimeUnit.SECONDS), "Concurrent writer did not commit");
            }
            return value;
        }).when(writerSpy).read(eq(studyId), any(UUID.class));

        UUID id = create("QUESTIONNAIRE");
        assertTrue(worker.startNext());
        var executor = Executors.newSingleThreadExecutor();
        Future<Boolean> generated = executor.submit(() -> worker.processNext());
        try {
            assertTrue(readStarted.await(30, TimeUnit.SECONDS), "Export did not establish its snapshot");
            // Independent committed transaction while the export snapshot is still open.
            assertEquals(1, jdbc.update("UPDATE participant_sessions SET status = 'ABANDONED', "
                    + "phase = 'FINISHED', abandonment_reason = 'PARTICIPANT_EXIT', abandoned_at = now(), "
                    + "updated_at = now(), lock_version = lock_version + 1 WHERE id = ?", later));
            writerCommitted.countDown();
            assertTrue(generated.get(30, TimeUnit.SECONDS));
        } finally {
            writerCommitted.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
        }
        assertEquals("IN_PROGRESS", sessionStatus(download(id), later));
        assertEquals("ABANDONED", jdbc.queryForObject(
                "SELECT status FROM participant_sessions WHERE id = ?", String.class, later));
        UUID next = create("QUESTIONNAIRE");
        process();
        assertEquals("ABANDONED", sessionStatus(download(next), later));
    }

    @Test
    @EnabledIfSystemProperty(named = "survey.performance.tests", matches = "true")
    void exportsTwoHundredOneSessionsAndOneHundredThousandEvents() throws Exception {
        UUID populated = newSession();
        for (int i = 0; i < 200; i++) newSession();
        for (int batch = 1; batch <= 100; batch++) {
            UUID id = addBatch(populated, batch, 1000);
            addEvents(populated, id, 1000);
        }
        UUID id = create("BEHAVIOR");
        long start = System.nanoTime();
        process();
        long generated = System.nanoTime();
        byte[] zip = download(id);
        long downloaded = System.nanoTime();
        assertEquals(201, csv(zip, "sessions.csv").size());
        assertEquals(100, csv(zip, "behavior_batches.csv").size());
        var events = csv(zip, "behavior_events.csv");
        assertEquals(100000, events.size());
        assertTrue(events.stream().allMatch(row -> populated.toString().equals(row.get("session_id"))));
        assertEquals(100000, events.stream().map(row -> row.get("batch_id") + ":" + row.get("event_index"))
                .distinct().count());
        long receiptCount = csv(zip, "data_availability.csv").stream()
                .filter(row -> "behavior_events.csv".equals(row.get("file_name")))
                .mapToLong(row -> Long.parseLong(row.get("record_count"))).sum();
        assertEquals(100000, receiptCount);
        String metrics = String.format(Locale.ROOT,
                "sessions=201 events=100000 batches=100 generation_ms=%.1f mockmvc_download_ms=%.1f zip_bytes=%d%n",
                (generated - start) / 1e6, (downloaded - generated) / 1e6, zip.length);
        System.out.print("M7_PERFORMANCE " + metrics);
        Path evidence = Path.of("target", "m7-export-performance.txt");
        Files.createDirectories(evidence.getParent());
        Files.writeString(evidence, metrics);
    }

    private UUID newSession() {
        Instant now = Instant.now();
        var session = ParticipantSession.create(studyId, UUID.randomUUID(),
                UUID.randomUUID().toString().replace("-", "").repeat(2), null, now);
        session.decideConsent(true, false, now);
        return sessions.saveAndFlush(session).getId();
    }

    private UUID addBatch(UUID session, int sequence, int count) {
        UUID batch = UUID.randomUUID();
        // One deterministic stream per fixture session, with increasing sequences.
        jdbc.update("INSERT INTO behavior_batches(session_id,batch_id,stream_id,sequence,schema_version,"
                        + "event_count,request_hash,received_at) VALUES (?,?,?,?,1,?,?,now())",
                session, batch, session, sequence, count, "0".repeat(64));
        return batch;
    }

    private void addEvents(UUID session, UUID batch, int count) {
        jdbc.update("INSERT INTO behavior_events(session_id,batch_id,event_index,event_type,occurred_at,"
                        + "session_elapsed_ms,page_id,payload) SELECT ?,?,i,'mousemove',now(),i,'m7-page',"
                        + "jsonb_build_object('x',i % 1000,'y',i % 800) FROM generate_series(0,?) AS i",
                session, batch, count - 1);
    }

    private UUID create(String category) throws Exception {
        String body = mvc.perform(post("/api/studies/{id}/exports", studyId)
                        .header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"dataTypes\":[\"" + category + "\"]}"))
                .andExpect(status().isAccepted()).andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn().getResponse().getContentAsString();
        UUID id = UUID.fromString(mapper.readTree(body).get("id").asString());
        jobs.add(id);
        return id;
    }

    private void process() {
        assertTrue(worker.startNext(), "Expected one queued export");
        assertTrue(worker.processNext(), "Expected one running export");
    }

    private byte[] download(UUID id) throws Exception {
        return mvc.perform(get(jobUrl(id) + "/download").header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(content().contentType("application/zip"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andReturn().getResponse().getContentAsByteArray();
    }

    private List<Map<String, String>> csv(byte[] zip, String name) throws Exception {
        try (var input = new ZipInputStream(new ByteArrayInputStream(zip), StandardCharsets.UTF_8)) {
            for (var entry = input.getNextEntry(); entry != null; entry = input.getNextEntry()) {
                if (name.equals(entry.getName())) {
                    String text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
                    try (var parser = CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true)
                            .get().parse(new StringReader(text))) {
                        return parser.stream().map(row -> row.toMap()).toList();
                    }
                }
            }
        }
        fail("Missing ZIP entry: " + name);
        return List.of();
    }

    private String sessionStatus(byte[] zip, UUID session) throws Exception {
        return csv(zip, "sessions.csv").stream().filter(row -> session.toString().equals(row.get("session_id")))
                .findFirst().orElseThrow().get("status");
    }

    private String jobUrl(UUID id) { return "/api/studies/" + studyId + "/exports/" + id; }
    private String bearer() { return "Bearer " + token; }
}
