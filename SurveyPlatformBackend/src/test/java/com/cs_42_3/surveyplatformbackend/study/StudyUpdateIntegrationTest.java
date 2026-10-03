package com.cs_42_3.surveyplatformbackend.study;

import com.cs_42_3.surveyplatformbackend.researcher.domain.Researcher;
import com.cs_42_3.surveyplatformbackend.researcher.repository.ResearcherRepository;
import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StudyUpdateIntegrationTest {

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
    private JwtEncoder jwtEncoder;

    @Autowired
    private EntityManager entityManager;

    private Researcher researcher;
    private String researcherToken;

    @BeforeEach
    void setUp() {

        researcher = researcherRepository.saveAndFlush(
                new Researcher(
                        "study-update-test@example.com",
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

    /** Verifies that updating an owned draft persists changes and increments its version. */
    @Test
    void shouldUpdateDraftStudySuccessfully() throws Exception {

        Study study = studyRepository.saveAndFlush(
                new Study(
                        researcher.getId(),
                        "Original Title",
                        "Original Description"
                )
        );

        UUID studyId = study.getId();
        long originalVersion = study.getLockVersion();

        String requestBody = """
                {
                  "version": %d,
                  "title": "Updated Title",
                  "description": "Updated Description",
                  "eyeTrackingEnabled": true,
                  "questionnaireEnabled": true
                }
                """.formatted(originalVersion);

        mockMvc.perform(
                        patch("/api/studies/{studyId}", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.description")
                        .value("Updated Description"))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.eyeTrackingEnabled").value(true))
                .andExpect(jsonPath("$.questionnaireEnabled").value(true))
                .andExpect(jsonPath("$.version")
                        .value(originalVersion + 1));

        // Clear the persistence context and reload the saved database state.
        entityManager.clear();

        Study updatedStudy = studyRepository.findById(studyId)
                .orElseThrow();

        assertEquals("Updated Title", updatedStudy.getTitle());
        assertEquals(
                "Updated Description",
                updatedStudy.getDescription()
        );

        assertTrue(updatedStudy.isEyeTrackingEnabled());
        assertTrue(updatedStudy.isQuestionnaireEnabled());

        assertEquals(StudyStatus.DRAFT, updatedStudy.getStatus());
        assertEquals(researcher.getId(), updatedStudy.getOwnerId());

        assertEquals(
                originalVersion + 1,
                updatedStudy.getLockVersion()
        );
    }

    /** Verifies that a partial update preserves all fields omitted from the request. */
    @Test
    void shouldPreserveUnchangedFieldsDuringPartialUpdate() throws Exception {

        Study study = studyRepository.saveAndFlush(
                new Study(
                        researcher.getId(),
                        "Original Title",
                        "Original Description"
                )
        );

        UUID studyId = study.getId();
        long originalVersion = study.getLockVersion();

        // Update only the title.
        String requestBody = """
            {
              "version": %d,
              "title": "Updated Title"
            }
            """.formatted(originalVersion);

        mockMvc.perform(
                        patch("/api/studies/{studyId}", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated Title"))
                .andExpect(jsonPath("$.description")
                        .value("Original Description"))
                .andExpect(jsonPath("$.eyeTrackingEnabled").value(false))
                .andExpect(jsonPath("$.questionnaireEnabled").value(false))
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.version")
                        .value(originalVersion + 1));

        // Reload from PostgreSQL instead of relying on the cached entity.
        entityManager.clear();

        Study updatedStudy = studyRepository.findById(studyId)
                .orElseThrow();

        assertEquals("Updated Title", updatedStudy.getTitle());

        assertEquals(
                "Original Description",
                updatedStudy.getDescription()
        );

        assertFalse(updatedStudy.isEyeTrackingEnabled());
        assertFalse(updatedStudy.isQuestionnaireEnabled());

        assertEquals(StudyStatus.DRAFT, updatedStudy.getStatus());
        assertEquals(researcher.getId(), updatedStudy.getOwnerId());

        assertEquals(
                originalVersion + 1,
                updatedStudy.getLockVersion()
        );
    }

    /** Verifies that a stale study version is rejected without overwriting saved changes. */
    @Test
    void shouldRejectStudyUpdateWithStaleVersion() throws Exception {

        Study study = studyRepository.saveAndFlush(
                new Study(
                        researcher.getId(),
                        "Original Title",
                        "Original Description"
                )
        );

        UUID studyId = study.getId();
        long originalVersion = study.getLockVersion();

        // First request successfully saves a newer version.
        String firstRequest = """
            {
              "version": %d,
              "title": "First Saved Title"
            }
            """.formatted(originalVersion);

        mockMvc.perform(
                        patch("/api/studies/{studyId}", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(firstRequest)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("First Saved Title"))
                .andExpect(jsonPath("$.version").value(originalVersion + 1));

        // Second request deliberately uses the outdated version.
        String staleRequest = """
            {
              "version": %d,
              "title": "Stale Overwrite Attempt"
            }
            """.formatted(originalVersion);

        mockMvc.perform(
                        patch("/api/studies/{studyId}", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(staleRequest)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("STUDY_VERSION_CONFLICT"));

        // Verify that the rejected request did not overwrite the first update.
        mockMvc.perform(
                        get("/api/studies/{studyId}", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("First Saved Title"))
                .andExpect(jsonPath("$.description")
                        .value("Original Description"))
                .andExpect(jsonPath("$.version").value(originalVersion + 1));
    }

    /** Verifies that a published study cannot be edited even with the correct version. */
    @Test
    void shouldRejectUpdateWhenStudyIsPublished() throws Exception {

        Study study = studyRepository.saveAndFlush(
                new Study(
                        researcher.getId(),
                        "Published Study",
                        "Original Description"
                )
        );

        UUID studyId = study.getId();

        // Prepare a COLLECTING study to isolate the editing restriction.
        study.publish(UUID.randomUUID().toString(), Instant.now());
        studyRepository.flush();

        long publishedVersion = study.getLockVersion();

        entityManager.clear();

        // Attempt to modify the published study using its current version.
        String requestBody = """
            {
              "version": %d,
              "title": "Unauthorized Updated Title"
            }
            """.formatted(publishedVersion);

        mockMvc.perform(
                        patch("/api/studies/{studyId}", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("STUDY_NOT_EDITABLE"));

        // Reload the database state and verify that nothing was modified.
        entityManager.clear();

        Study unchangedStudy = studyRepository.findById(studyId)
                .orElseThrow();

        assertEquals("Published Study", unchangedStudy.getTitle());

        assertEquals(
                "Original Description",
                unchangedStudy.getDescription()
        );

        assertEquals(
                StudyStatus.COLLECTING,
                unchangedStudy.getStatus()
        );

        assertEquals(
                publishedVersion,
                unchangedStudy.getLockVersion()
        );
    }

    /** Verifies that clients cannot modify server-managed study status through PATCH. */
    @Test
    void shouldRejectUpdateWithUnexpectedStatusField() throws Exception {

        Study study = studyRepository.saveAndFlush(
                new Study(
                        researcher.getId(),
                        "Original Title",
                        "Original Description"
                )
        );

        UUID studyId = study.getId();
        long originalVersion = study.getLockVersion();

        String requestBody = """
            {
              "version": %d,
              "title": "Attempted Update",
              "status": "COLLECTING"
            }
            """.formatted(originalVersion);

        mockMvc.perform(
                        patch("/api/studies/{studyId}", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());

        // Reload the database state to verify that the request changed nothing.
        entityManager.clear();

        Study unchangedStudy = studyRepository.findById(studyId)
                .orElseThrow();

        assertEquals("Original Title", unchangedStudy.getTitle());

        assertEquals(
                "Original Description",
                unchangedStudy.getDescription()
        );

        assertEquals(StudyStatus.DRAFT, unchangedStudy.getStatus());

        assertEquals(
                researcher.getId(),
                unchangedStudy.getOwnerId()
        );

        assertEquals(
                originalVersion,
                unchangedStudy.getLockVersion()
        );
    }

    /** Verifies that explicitly setting description to null clears the saved description. */
    @Test
    void shouldClearStudyDescriptionWhenExplicitlyNull() throws Exception {

        Study study = studyRepository.saveAndFlush(
                new Study(
                        researcher.getId(),
                        "Original Title",
                        "Original Description"
                )
        );

        UUID studyId = study.getId();
        long originalVersion = study.getLockVersion();

        String requestBody = """
            {
              "version": %d,
              "description": null
            }
            """.formatted(originalVersion);

        mockMvc.perform(
                        patch("/api/studies/{studyId}", studyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title")
                        .value("Original Title"))
                .andExpect(jsonPath("$.description")
                        .value(org.hamcrest.Matchers.nullValue()))
                .andExpect(jsonPath("$.status")
                        .value("DRAFT"))
                .andExpect(jsonPath("$.version")
                        .value(originalVersion + 1));

        // Reload the study from PostgreSQL.
        entityManager.clear();

        Study updatedStudy = studyRepository.findById(studyId)
                .orElseThrow();

        assertEquals("Original Title", updatedStudy.getTitle());

        // An explicit JSON null should clear the description.
        assertNull(updatedStudy.getDescription());

        assertFalse(updatedStudy.isEyeTrackingEnabled());
        assertFalse(updatedStudy.isQuestionnaireEnabled());

        assertEquals(StudyStatus.DRAFT, updatedStudy.getStatus());
        assertEquals(researcher.getId(), updatedStudy.getOwnerId());

        assertEquals(
                originalVersion + 1,
                updatedStudy.getLockVersion()
        );
    }

}
