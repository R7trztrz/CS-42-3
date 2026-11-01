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
@EnabledIfSystemProperty(named = "survey.performance.tests", matches = "true")
@SpringBootTest(properties = {
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=",
        "app.exports.scheduler-enabled=false"
})
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@Timeout(300)
class M7ExportScaleIntegrationTest {
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
    void exportsOneMillionEventsWithHeapObservationAndConcurrentStudyReads() throws Exception {
        UUID populated = newSession();
        for (int i = 0; i < 200; i++) newSession();
        Map<String, BitSet> expected = new HashMap<>();
        for (int batch = 1; batch <= 1000; batch++) {
            UUID id = addBatch(populated, batch, 1000);
            addEvents(populated, id, 1000);
            expected.put(id.toString(), new BitSet(1000));
        }
        UUID job = create("BEHAVIOR");
        assertTrue(worker.startNext());
        var memory = java.lang.management.ManagementFactory.getMemoryMXBean();
        var pools = java.lang.management.ManagementFactory.getMemoryPoolMXBeans().stream()
                .filter(pool -> pool.getType() == java.lang.management.MemoryType.HEAP).toList();
        for (var pool : pools) pool.resetPeakUsage();
        long baseline = memory.getHeapMemoryUsage().getUsed();
        var sampledPeak = new java.util.concurrent.atomic.AtomicLong(baseline);
        ScheduledExecutorService sampler = Executors.newSingleThreadScheduledExecutor();
        ExecutorService executor = Executors.newSingleThreadExecutor();
        var schedule = sampler.scheduleAtFixedRate(() -> sampledPeak.accumulateAndGet(
                memory.getHeapMemoryUsage().getUsed(), Math::max), 0, 20, TimeUnit.MILLISECONDS);
        long started = System.nanoTime();
        long maxApiNanos = 0;
        int concurrentReads = 0;
        // A latch proves the worker reached data generation, rather than probing only a queued task.
        var entered = new CountDownLatch(1);
        var proceed = new CountDownLatch(1);
        SessionExportWriter writerSpy = AopTestUtils.getUltimateTargetObject(sessionWriter);
        doAnswer(invocation -> {
            entered.countDown();
            assertTrue(proceed.await(10, TimeUnit.SECONDS));
            return invocation.callRealMethod();
        }).when(writerSpy).read(eq(studyId), any(UUID.class));
        Future<Boolean> generated = executor.submit(() -> worker.processNext());
        try {
            assertTrue(entered.await(15, TimeUnit.SECONDS), "Worker did not begin writing sessions");
            // Release before measuring API responsiveness; no measured call runs behind the artificial pause.
            proceed.countDown();
            while (!generated.isDone()) {
                long probeStart = System.nanoTime();
                mvc.perform(get("/api/studies/{id}", studyId).header("Authorization", bearer()))
                        .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(studyId.toString()));
                long duration = System.nanoTime() - probeStart;
                // Require overlap through the end of the request, not merely before it starts.
                if (!generated.isDone()) {
                    concurrentReads++;
                    maxApiNanos = Math.max(maxApiNanos, duration);
                }
                try { generated.get(50, TimeUnit.MILLISECONDS); }
                catch (TimeoutException stillRunning) { /* next measured probe */ }
            }
            assertTrue(generated.get(10, TimeUnit.SECONDS));
        } finally {
            proceed.countDown(); generated.cancel(true);
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(30, TimeUnit.SECONDS));
            schedule.cancel(false); sampler.shutdownNow();
            assertTrue(sampler.awaitTermination(10, TimeUnit.SECONDS));
        }
        long generationNanos = System.nanoTime() - started;
        long poolPeakUpperBound = pools.stream().mapToLong(pool -> pool.getPeakUsage().getUsed()).sum();
        long heapGrowth = Math.max(0, poolPeakUpperBound - baseline);
        mvc.perform(get(jobUrl(job)).header("Authorization", bearer()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("COMPLETED"));

        // Parse the artifact as a stream. No million-row List/Map or MockMvc byte[] download buffer.
        Set<String> entries = new HashSet<>();
        long eventCount = 0, batchCount = 0, sessionCount = 0, receiptCount = 0;
        try (var zip = new ZipInputStream(storage.open(job), StandardCharsets.UTF_8)) {
            for (var entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
                String name = entry.getName();
                assertTrue(entries.add(name), "Duplicate ZIP entry: " + name);
                assertFalse(name.startsWith("/") || name.contains("\\")
                        || Arrays.asList(name.split("/")).contains(".."), "Unsafe ZIP entry: " + name);
                if (!Set.of("behavior_events.csv", "behavior_batches.csv", "sessions.csv",
                        "data_availability.csv").contains(name)) continue;
                // Do not close this parser here: its Reader wraps the shared ZIP stream.
                var parser = CSVFormat.RFC4180.builder().setHeader().setSkipHeaderRecord(true).get()
                        .parse(new InputStreamReader(zip, StandardCharsets.UTF_8));
                for (var row : parser) {
                    switch (name) {
                        case "behavior_events.csv" -> {
                            assertEquals(populated.toString(), row.get("session_id"));
                            assertEquals(studyId.toString(), row.get("study_id"));
                            assertEquals("mousemove", row.get("event_type"));
                            BitSet seen = expected.get(row.get("batch_id"));
                            assertNotNull(seen, "Unknown exported batch");
                            int index = Integer.parseInt(row.get("event_index"));
                            assertTrue(index >= 0 && index < 1000);
                            assertEquals(index % 1000, Double.parseDouble(row.get("x")));
                            assertEquals(index % 800, Double.parseDouble(row.get("y")));
                            assertFalse(seen.get(index), "Duplicate event identity");
                            seen.set(index); eventCount++;
                        }
                        case "behavior_batches.csv" -> batchCount++;
                        case "sessions.csv" -> sessionCount++;
                        case "data_availability.csv" -> {
                            if ("behavior_events.csv".equals(row.get("file_name")))
                                receiptCount += Long.parseLong(row.get("record_count"));
                        }
                        default -> throw new AssertionError(name);
                    }
                }
            }
        }
        assertEquals(1_000_000L, eventCount);
        assertEquals(1000L, batchCount);
        assertEquals(201L, sessionCount);
        assertEquals(eventCount, receiptCount);
        assertTrue(expected.values().stream().allMatch(bits -> bits.cardinality() == 1000));
        assertTrue(concurrentReads > 0, "No study read overlapped active export generation");
        // Tentative, configurable local regression budgets, not universal production SLAs.
        long heapBudgetMiB = Long.getLong("m7.performance.maxHeapGrowthMiB", 512L);
        long apiBudgetMs = Long.getLong("m7.performance.maxApiMillis", 5000L);
        long generationBudgetMs = Long.getLong("m7.performance.maxGenerationMillis", 120000L);
        String metrics = String.format(Locale.ROOT,
                "events=%d sessions=%d batches=%d generation_ms=%.1f concurrent_reads=%d "
                        + "max_api_ms=%.1f heap_baseline_bytes=%d sampled_peak_bytes=%d "
                        + "sum_pool_peaks_bytes=%d heap_growth_upper_bound_bytes=%d "
                        + "heap_budget_mib=%d api_budget_ms=%d generation_budget_ms=%d%n",
                eventCount, sessionCount, batchCount, generationNanos / 1e6, concurrentReads,
                maxApiNanos / 1e6, baseline, sampledPeak.get(), poolPeakUpperBound, heapGrowth,
                heapBudgetMiB, apiBudgetMs, generationBudgetMs);
        Path evidence = Path.of("target", "m7-million-export-performance.txt");
        Files.createDirectories(evidence.getParent()); Files.writeString(evidence, metrics);
        System.out.print("M7_MILLION_PERFORMANCE " + metrics);
        assertTrue(heapGrowth <= heapBudgetMiB * 1024 * 1024, metrics);
        assertTrue(maxApiNanos <= apiBudgetMs * 1_000_000, metrics);
        assertTrue(generationNanos <= generationBudgetMs * 1_000_000, metrics);
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

    private String jobUrl(UUID id) { return "/api/studies/" + studyId + "/exports/" + id; }
    private String bearer() { return "Bearer " + token; }
}
