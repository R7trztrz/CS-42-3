package com.cs_42_3.surveyplatformbackend.study;

import com.cs_42_3.surveyplatformbackend.feed.domain.StudyFeed;
import com.cs_42_3.surveyplatformbackend.feed.repository.FeedTemplateRepository;
import com.cs_42_3.surveyplatformbackend.feed.repository.StudyFeedRepository;
import com.cs_42_3.surveyplatformbackend.researcher.domain.Researcher;
import com.cs_42_3.surveyplatformbackend.researcher.repository.ResearcherRepository;
import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
class StudyPublicationConcurrencyIntegrationTest {

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
    private PlatformTransactionManager transactionManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /** Verifies that concurrent publication and feed saving preserve a consistent published snapshot. */
    @Test
    void shouldSerializeConcurrentPublicationAndFeedUpdate() throws Exception {

        // Create a researcher with a unique email.
        Researcher researcher = researcherRepository.saveAndFlush(
                new Researcher(
                        "study-concurrency-" + UUID.randomUUID() + "@example.com",
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

        String researcherToken = jwtEncoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();

        // Create and commit the draft study.
        Study study = studyRepository.saveAndFlush(
                new Study(
                        researcher.getId(),
                        "Concurrent Publication Study",
                        "Verify publication and feed update consistency"
                )
        );

        UUID studyId = study.getId();
        long originalStudyVersion = study.getLockVersion();

        // Create and commit the initial feed.
        var template = feedTemplateRepository.findById("blank")
                .orElseThrow();

        StudyFeed feed = new StudyFeed(studyId, template);

        feed.replaceContent("""
                {
                  "ROOT": {
                    "type": "ORIGINAL"
                  }
                }
                """);

        studyFeedRepository.saveAndFlush(feed);

        long originalFeedVersion = feed.getLockVersion();

        // Prepare two valid requests against the same study.
        String publishRequest = """
                {
                  "version": %d
                }
                """.formatted(originalStudyVersion);

        String updateRequest = """
                {
                  "version": %d,
                  "content": {
                    "ROOT": {
                      "type": "UPDATED"
                    }
                  }
                }
                """.formatted(originalFeedVersion);

        // Both threads wait for a shared release signal.
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {

            // Thread A: publish the study.
            Future<MvcResult> publicationFuture = executor.submit(() -> {

                ready.countDown();

                assertTrue(
                        start.await(10, TimeUnit.SECONDS),
                        "Publication thread did not receive the start signal"
                );

                return mockMvc.perform(
                                post("/api/studies/{studyId}/publish", studyId)
                                        .header(
                                                "Authorization",
                                                "Bearer " + researcherToken
                                        )
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(publishRequest)
                        )
                        .andReturn();
            });

            // Thread B: save a modified feed.
            Future<MvcResult> updateFuture = executor.submit(() -> {

                ready.countDown();

                assertTrue(
                        start.await(10, TimeUnit.SECONDS),
                        "Feed update thread did not receive the start signal"
                );

                return mockMvc.perform(
                                put("/api/studies/{studyId}/feed", studyId)
                                        .header(
                                                "Authorization",
                                                "Bearer " + researcherToken
                                        )
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(updateRequest)
                        )
                        .andReturn();
            });

            // Wait until both workers are ready, then release them together.
            assertTrue(
                    ready.await(10, TimeUnit.SECONDS),
                    "Both request threads should become ready"
            );

            start.countDown();

            MvcResult publicationResult =
                    publicationFuture.get(30, TimeUnit.SECONDS);

            MvcResult updateResult =
                    updateFuture.get(30, TimeUnit.SECONDS);

            int publicationStatus =
                    publicationResult.getResponse().getStatus();

            int updateStatus =
                    updateResult.getResponse().getStatus();

            // Publication must succeed regardless of which request acquires the lock first.
            assertEquals(
                    200,
                    publicationStatus,
                    publicationResult.getResponse().getContentAsString()
            );

            // Feed saving may finish before publication or be rejected after it.
            assertTrue(
                    updateStatus == 200 || updateStatus == 409,
                    "Unexpected feed update response: "
                            + updateResult.getResponse().getContentAsString()
            );

            // Reload both entities after the requests have completed.
            Study publishedStudy = studyRepository.findById(studyId)
                    .orElseThrow();

            StudyFeed persistedFeed = studyFeedRepository.findById(studyId)
                    .orElseThrow();

            // The study must have been published exactly once.
            assertEquals(
                    StudyStatus.COLLECTING,
                    publishedStudy.getStatus()
            );

            assertNotNull(publishedStudy.getParticipationToken());
            assertNotNull(publishedStudy.getPublishedAt());

            assertEquals(
                    originalStudyVersion + 1,
                    publishedStudy.getLockVersion()
            );

            String persistedFeedType =
                    JsonPath.read(persistedFeed.getContent(), "$.ROOT.type");

            if (updateStatus == 200) {

                // Feed update won the lock: publication must use the updated feed.
                assertEquals("UPDATED", persistedFeedType);

                assertEquals(
                        originalFeedVersion + 1,
                        persistedFeed.getLockVersion()
                );

            } else {

                // Publication won the lock: subsequent feed editing must be rejected.
                String errorCode = JsonPath.read(
                        updateResult.getResponse().getContentAsString(),
                        "$.code"
                );

                assertEquals("STUDY_NOT_EDITABLE", errorCode);

                assertEquals("ORIGINAL", persistedFeedType);

                assertEquals(
                        originalFeedVersion,
                        persistedFeed.getLockVersion()
                );
            }

        } finally {

            // Ensure waiting threads are always released, even after an assertion failure.
            start.countDown();

            executor.shutdownNow();
        }
    }

    /** Verifies that a feed save blocked by publication cannot modify the published feed. */
    @Test
    void shouldRejectFeedSaveWaitingBehindPublicationLock() throws Exception {

        // Create committed test data without a class-level transaction.
        Researcher researcher = researcherRepository.saveAndFlush(
                new Researcher(
                        "study-lock-" + UUID.randomUUID() + "@example.com",
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

        String token = jwtEncoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();

        Study study = studyRepository.saveAndFlush(
                new Study(
                        researcher.getId(),
                        "Controlled Publication Lock Study",
                        "Verify database lock ordering"
                )
        );

        UUID studyId = study.getId();
        long studyVersion = study.getLockVersion();

        var template = feedTemplateRepository.findById("blank")
                .orElseThrow();

        StudyFeed feed = new StudyFeed(studyId, template);

        feed.replaceContent("""
            {
              "ROOT": {
                "type": "ORIGINAL"
              }
            }
            """);

        studyFeedRepository.saveAndFlush(feed);

        long feedVersion = feed.getLockVersion();

        String publishRequest = """
            {
              "version": %d
            }
            """.formatted(studyVersion);

        String saveRequest = """
            {
              "version": %d,
              "content": {
                "ROOT": {
                  "type": "UPDATED"
                }
              }
            }
            """.formatted(feedVersion);

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        ExecutorService executor = Executors.newSingleThreadExecutor();

        CountDownLatch workerStarted = new CountDownLatch(1);

        AtomicReference<Future<MvcResult>> saveFuture =
                new AtomicReference<>();

        try {

            // The outer transaction holds the Study lock until publication commits.
            transaction.executeWithoutResult(tx -> {

                studyRepository.findOwnedByIdForUpdate(
                        studyId,
                        researcher.getId()
                ).orElseThrow();

                // A separate thread attempts a real HTTP Feed save.
                Future<MvcResult> future = executor.submit(() -> {

                    workerStarted.countDown();

                    return mockMvc.perform(
                                    put("/api/studies/{studyId}/feed", studyId)
                                            .header(
                                                    "Authorization",
                                                    "Bearer " + token
                                            )
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(saveRequest)
                            )
                            .andReturn();
                });

                saveFuture.set(future);

                try {
                    assertTrue(
                            workerStarted.await(5, TimeUnit.SECONDS),
                            "Feed save worker did not start"
                    );
                } catch (InterruptedException exception) {
                    Thread.currentThread().interrupt();
                    throw new IllegalStateException(exception);
                }

                // Observe PostgreSQL until the Feed request is waiting for a lock.
                boolean lockWaitObserved = false;


                for (int attempt = 0; attempt < 200; attempt++) {

                    // Refresh PostgreSQL activity statistics inside the long-running transaction.
                    jdbcTemplate.execute("SELECT pg_stat_clear_snapshot()");

                    // Check whether this transaction is blocking another database session.
                    Boolean waiting = jdbcTemplate.queryForObject("""
            SELECT EXISTS (
                SELECT 1
                FROM pg_stat_activity a
                WHERE a.datname = current_database()
                  AND a.pid <> pg_backend_pid()
                  AND pg_backend_pid() = ANY(pg_blocking_pids(a.pid))
            )
            """, Boolean.class);

                    if (Boolean.TRUE.equals(waiting)) {
                        lockWaitObserved = true;
                        break;
                    }

                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        throw new IllegalStateException(exception);
                    }
                }


                assertTrue(
                        lockWaitObserved,
                        "The Feed save request should wait for the Study database lock"
                );

                assertFalse(
                        future.isDone(),
                        "Feed saving must not finish while the Study lock is held"
                );

                // Publish on the thread that already owns the transaction and lock.
                try {
                    mockMvc.perform(
                                    post("/api/studies/{studyId}/publish", studyId)
                                            .header(
                                                    "Authorization",
                                                    "Bearer " + token
                                            )
                                            .contentType(MediaType.APPLICATION_JSON)
                                            .content(publishRequest)
                            )
                            .andExpect(status().isOk());

                } catch (Exception exception) {
                    throw new IllegalStateException(exception);
                }

                // PostgreSQL releases the lock when this callback's transaction commits.
            });

            // The waiting request resumes only after publication commits.
            MvcResult saveResult = saveFuture.get()
                    .get(15, TimeUnit.SECONDS);

            assertEquals(
                    409,
                    saveResult.getResponse().getStatus(),
                    saveResult.getResponse().getContentAsString()
            );

            String errorCode = JsonPath.read(
                    saveResult.getResponse().getContentAsString(),
                    "$.code"
            );

            assertEquals("STUDY_NOT_EDITABLE", errorCode);

            // Verify the final state using fresh repository reads.
            Study publishedStudy = studyRepository.findById(studyId)
                    .orElseThrow();

            StudyFeed unchangedFeed = studyFeedRepository.findById(studyId)
                    .orElseThrow();

            assertEquals(
                    StudyStatus.COLLECTING,
                    publishedStudy.getStatus()
            );

            assertNotNull(publishedStudy.getParticipationToken());
            assertNotNull(publishedStudy.getPublishedAt());

            assertEquals(
                    studyVersion + 1,
                    publishedStudy.getLockVersion()
            );

            String persistedType = JsonPath.read(
                    unchangedFeed.getContent(),
                    "$.ROOT.type"
            );

            // The waiting Feed save must not overwrite published content.
            assertEquals("ORIGINAL", persistedType);

            assertEquals(
                    feedVersion,
                    unchangedFeed.getLockVersion()
            );

        } finally {

            executor.shutdownNow();
        }
    }

}
