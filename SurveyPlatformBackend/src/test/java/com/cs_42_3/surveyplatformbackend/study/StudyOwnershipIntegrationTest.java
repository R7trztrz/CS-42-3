package com.cs_42_3.surveyplatformbackend.study;

import com.cs_42_3.surveyplatformbackend.TestcontainersConfiguration;
import com.cs_42_3.surveyplatformbackend.researcher.domain.Researcher;
import com.cs_42_3.surveyplatformbackend.researcher.repository.ResearcherRepository;
import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.context.annotation.Import;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import java.time.Instant;
import java.util.UUID;

@Testcontainers
@SpringBootTest(properties = {
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@Transactional
class StudyOwnershipIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ResearcherRepository researcherRepository;

    @Autowired
    private StudyRepository studyRepository;

    @Autowired
    private JwtEncoder jwtEncoder;

    private Researcher researcherA;
    private Researcher researcherB;

    private Study studyA;
    private Study studyB;

    private String researcherAToken;

    @BeforeEach
    void setUp() {

        researcherA = researcherRepository.save(
                new Researcher(
                        "ownership-a@example.com",
                        "test-password-hash"
                )
        );

        researcherB = researcherRepository.save(
                new Researcher(
                        "ownership-b@example.com",
                        "test-password-hash"
                )
        );

        studyA = studyRepository.saveAndFlush(
                new Study(
                        researcherA.getId(),
                        "Researcher A Study",
                        "Study owned by researcher A"
                )
        );

        studyB = studyRepository.saveAndFlush(
                new Study(
                        researcherB.getId(),
                        "Researcher B Study",
                        "Study owned by researcher B"
                )
        );

        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(researcherA.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plusSeconds(3600))
                .claim("role", "RESEARCHER")
                .build();

        researcherAToken = jwtEncoder
                .encode(JwtEncoderParameters.from(claims))
                .getTokenValue();
    }

    /** Verifies that a researcher cannot read another researcher's study. */
    @Test
    void shouldNotReadAnotherResearchersStudy() throws Exception {

        mockMvc.perform(
                        get("/api/studies/{studyId}", studyB.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherAToken
                                )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("STUDY_NOT_FOUND"))
                .andExpect(jsonPath("$.error")
                        .value("Study not found."));
    }

    /** Verifies that a researcher cannot modify another researcher's study. */
    @Test
    void shouldNotModifyAnotherResearchersStudy() throws Exception {

        mockMvc.perform(
                        patch("/api/studies/{studyId}", studyB.getId())
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherAToken
                                )
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "version": 0,
                                      "title": "Hacked title"
                                    }
                                    """)
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("STUDY_NOT_FOUND"))
                .andExpect(jsonPath("$.error")
                        .value("Study not found."));
    }

    /** Verifies that another user's study is indistinguishable from a missing study. */
    @Test
    void shouldHideStudyExistenceFromNonOwner() throws Exception {

        UUID missingStudyId =
                UUID.fromString("11111111-1111-1111-1111-111111111111");

        mockMvc.perform(
                        get("/api/studies/{studyId}", missingStudyId)
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherAToken
                                )
                )
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code")
                        .value("STUDY_NOT_FOUND"))
                .andExpect(jsonPath("$.error")
                        .value("Study not found."));
    }

    /** Verifies that a researcher only sees their own studies in the list. */
    @Test
    void shouldOnlyListOwnedStudies() throws Exception {

        mockMvc.perform(
                        get("/api/studies")
                                .header(
                                        "Authorization",
                                        "Bearer " + researcherAToken
                                )
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[*].id")
                        .value(org.hamcrest.Matchers.hasItem(
                                studyA.getId().toString()
                        )))
                .andExpect(jsonPath("$.content[*].id")
                        .value(org.hamcrest.Matchers.not(
                                org.hamcrest.Matchers.hasItem(
                                        studyB.getId().toString()
                                )
                        )));
    }
}
