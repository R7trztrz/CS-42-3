
package com.cs_42_3.surveyplatformbackend.study;

import com.cs_42_3.surveyplatformbackend.feed.repository.StudyFeedRepository;
import com.cs_42_3.surveyplatformbackend.researcher.domain.Researcher;
import com.cs_42_3.surveyplatformbackend.researcher.repository.ResearcherRepository;
import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.domain.StudyStatus;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class StudyCreationIntegrationTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtEncoder jwtEncoder;

    private Researcher researcher;
    private String researcherToken;

    @BeforeEach
    void setUp() {

        researcher = researcherRepository.saveAndFlush(
                new Researcher(
                        "study-create-test@example.com",
                        passwordEncoder.encode("TestPassword123")
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

    /** Verifies that creating a valid study persists an owned draft and initializes its feed. */
    @Test
    void shouldCreateDraftStudySuccessfully() throws Exception {

        String responseBody =
                mockMvc.perform(
                                post("/api/studies")
                                        .header(
                                                "Authorization",
                                                "Bearer " + researcherToken
                                        )
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content("""
                                        {
                                          "title": "Eye Tracking Study",
                                          "description": "Study creation integration test",
                                          "templateCode": "blank"
                                        }
                                        """)
                        )
                        .andExpect(status().isCreated())
                        .andExpect(jsonPath("$.id").isNotEmpty())
                        .andExpect(jsonPath("$.title")
                                .value("Eye Tracking Study"))
                        .andExpect(jsonPath("$.description")
                                .value("Study creation integration test"))
                        .andExpect(jsonPath("$.status")
                                .value("DRAFT"))
                        .andExpect(jsonPath("$.eyeTrackingEnabled")
                                .value(false))
                        .andExpect(jsonPath("$.questionnaireEnabled")
                                .value(false))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();

        // Retrieve the generated study ID from the HTTP response.
        String studyIdString = JsonPath.read(responseBody, "$.id");
        UUID studyId = UUID.fromString(studyIdString);

        // Verify that the study was persisted in PostgreSQL.
        Study savedStudy = studyRepository.findById(studyId)
                .orElseThrow();

        assertEquals(
                researcher.getId(),
                savedStudy.getOwnerId()
        );

        assertEquals(
                "Eye Tracking Study",
                savedStudy.getTitle()
        );

        assertEquals(
                "Study creation integration test",
                savedStudy.getDescription()
        );

        assertEquals(
                StudyStatus.DRAFT,
                savedStudy.getStatus()
        );

        assertFalse(savedStudy.isEyeTrackingEnabled());
        assertFalse(savedStudy.isQuestionnaireEnabled());

        assertNotNull(savedStudy.getCreatedAt());
        assertNotNull(savedStudy.getUpdatedAt());

        // Verify that the selected feed template was initialized.
        var savedFeed = studyFeedRepository.findById(studyId)
                .orElseThrow();

        assertEquals("blank", savedFeed.getTemplateCode());
    }


    /** Verifies that a blank study title is rejected without creating a study. */
    @Test
    void shouldRejectStudyCreationWithBlankTitle() throws Exception {

        long studyCountBefore = studyRepository.count();

        mockMvc.perform(
                        post("/api/studies")
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "   ",
                                  "description": "Invalid study title test",
                                  "templateCode": "blank"
                                }
                                """)
                )
                .andExpect(status().isBadRequest());

        assertEquals(
                studyCountBefore,
                studyRepository.count()
        );
    }

    /** Verifies that a study title exceeding 255 characters is rejected without persistence. */
    @Test
    void shouldRejectStudyCreationWithOverlongTitle() throws Exception {

        long studyCountBefore = studyRepository.count();

        String longTitle = "A".repeat(256);

        String requestBody = """
            {
              "title": "%s",
              "description": "Title length boundary test",
              "templateCode": "blank"
            }
            """.formatted(longTitle);

        mockMvc.perform(
                        post("/api/studies")
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(requestBody)
                )
                .andExpect(status().isBadRequest());

        assertEquals(
                studyCountBefore,
                studyRepository.count()
        );
    }

    /** Verifies that an unknown feed template is rejected without persisting study data. */
    @Test
    void shouldRejectStudyCreationWithUnknownTemplate() throws Exception {

        long studyCountBefore = studyRepository.count();
        long feedCountBefore = studyFeedRepository.count();

        mockMvc.perform(
                        post("/api/studies")
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Invalid Template Study",
                                  "description": "Unknown template test",
                                  "templateCode": "nonexistent-template"
                                }
                                """)
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("FEED_TEMPLATE_NOT_FOUND"));

        assertEquals(
                studyCountBefore,
                studyRepository.count()
        );

        assertEquals(
                feedCountBefore,
                studyFeedRepository.count()
        );
    }

    /** Verifies that a missing template code is rejected without creating a study or feed. */
    @Test
    void shouldRejectStudyCreationWithoutTemplateCode() throws Exception {

        long studyCountBefore = studyRepository.count();
        long feedCountBefore = studyFeedRepository.count();

        mockMvc.perform(
                        post("/api/studies")
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                {
                                  "title": "Missing Template Study",
                                  "description": "Template code validation test"
                                }
                                """)
                )
                .andExpect(status().isBadRequest());

        assertEquals(
                studyCountBefore,
                studyRepository.count()
        );

        assertEquals(
                feedCountBefore,
                studyFeedRepository.count()
        );
    }

}