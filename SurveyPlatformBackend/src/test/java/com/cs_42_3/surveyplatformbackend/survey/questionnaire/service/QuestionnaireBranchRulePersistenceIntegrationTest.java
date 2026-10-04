package com.cs_42_3.surveyplatformbackend.survey.questionnaire.service;

import com.cs_42_3.surveyplatformbackend.TestcontainersConfiguration;
import com.cs_42_3.surveyplatformbackend.security.CurrentResearcher;
import com.cs_42_3.surveyplatformbackend.study.domain.Study;
import com.cs_42_3.surveyplatformbackend.study.repository.StudyRepository;
import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.repository.QuestionRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** PostgreSQL regression coverage for adding a rule that targets a new sibling item. */
@SpringBootTest(properties = {
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@Testcontainers
class QuestionnaireBranchRulePersistenceIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private StudyRepository studyRepository;
    @Autowired
    private QuestionRepository questionRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private CurrentResearcher currentResearcher;

    private UUID researcherId;
    private UUID studyId;
    private UUID sourceQuestionId;
    private UUID sourceOptionId;
    private UUID targetQuestionId;

    @BeforeEach
    void setUp() {
        researcherId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                        INSERT INTO researchers (id, email, password_hash, role)
                        VALUES (?, ?, 'test-only-password-hash', 'RESEARCHER')
                        """,
                researcherId,
                researcherId + "@survey-test.invalid"
        );
        studyId = studyRepository.saveAndFlush(
                new Study(researcherId, "Branch target persistence", null)
        ).getId();

        Question source = Question.create(
                researcherId,
                QuestionType.SINGLE_CHOICE,
                "Continue to the new item?",
                true,
                null,
                null,
                null,
                null
        );
        source.replaceOptions(java.util.List.of("Yes", "No"));
        source = questionRepository.saveAndFlush(source);
        sourceQuestionId = source.getId();
        sourceOptionId = source.getOptions().get(0).getId();

        targetQuestionId = questionRepository.saveAndFlush(Question.create(
                researcherId,
                QuestionType.TEXT,
                "New branch target",
                false,
                null,
                null,
                null,
                null
        )).getId();
        when(currentResearcher.getId()).thenReturn(researcherId);
    }

    @Test
    void updateCanPersistRuleFromExistingItemToNewItemAndRoundTripIt() throws Exception {
        String firstSave = mockMvc.perform(put("/api/studies/{studyId}/questionnaire", studyId)
                        .with(researcherJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedVersion": null,
                                  "items": [{
                                    "itemId": null,
                                    "questionId": "%s",
                                    "branchRules": []
                                  }]
                                }
                                """.formatted(sourceQuestionId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String sourceItemId = JsonPath.read(firstSave, "$.items[0].itemId");
        Number firstVersion = JsonPath.read(firstSave, "$.version");

        String secondSave = mockMvc.perform(put("/api/studies/{studyId}/questionnaire", studyId)
                        .with(researcherJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedVersion": %d,
                                  "items": [
                                    {
                                      "itemId": "%s",
                                      "questionId": "%s",
                                      "branchRules": [{
                                        "sourceOptionId": "%s",
                                        "sourceScaleValue": null,
                                        "targetPosition": 1
                                      }]
                                    },
                                    {
                                      "itemId": null,
                                      "questionId": "%s",
                                      "branchRules": []
                                    }
                                  ]
                                }
                                """.formatted(
                                firstVersion.longValue(),
                                sourceItemId,
                                sourceQuestionId,
                                sourceOptionId,
                                targetQuestionId
                        )))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].itemId").value(sourceItemId))
                .andExpect(jsonPath("$.items[1].itemId").isNotEmpty())
                .andExpect(jsonPath("$.items[0].branchRules[0].targetPosition").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String targetItemId = JsonPath.read(secondSave, "$.items[1].itemId");
        Number secondVersion = JsonPath.read(secondSave, "$.version");
        assertThat(secondVersion.longValue()).isGreaterThan(firstVersion.longValue());

        mockMvc.perform(get("/api/studies/{studyId}/questionnaire", studyId)
                        .with(researcherJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(secondVersion.longValue()))
                .andExpect(jsonPath("$.items[0].itemId").value(sourceItemId))
                .andExpect(jsonPath("$.items[1].itemId").value(targetItemId))
                .andExpect(jsonPath("$.items[0].branchRules[0].targetItemId")
                        .value(targetItemId));

        mockMvc.perform(put("/api/studies/{studyId}/questionnaire", studyId)
                        .with(researcherJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion": %d, "items": []}
                                """.formatted(firstVersion.longValue())))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUESTIONNAIRE_VERSION_CONFLICT"));
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor researcherJwt() {
        return jwt().jwt(token -> token.subject(researcherId.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_RESEARCHER"));
    }
}
