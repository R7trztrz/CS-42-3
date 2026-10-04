package com.cs_42_3.surveyplatformbackend;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
@Import(TestcontainersConfiguration.class)
@ActiveProfiles("test")
@AutoConfigureMockMvc
@Testcontainers
class SurveyPlatformBackendApplicationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void openApiPublishesParticipantAuthenticationAndStableErrorContracts() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.components.securitySchemes.participantSessionToken.type")
                        .value("apiKey"))
                .andExpect(jsonPath("$.components.securitySchemes.participantSessionToken.in")
                        .value("header"))
                .andExpect(jsonPath("$.components.securitySchemes.participantSessionToken.name")
                        .value("X-Participant-Session-Token"))
                .andExpect(jsonPath("$.paths['/api/participation/{studyToken}/sessions'].post.security")
                        .doesNotExist())
                .andExpect(jsonPath("$.paths['/api/participant-session'].get.security[0].participantSessionToken")
                        .isArray())
                .andExpect(jsonPath("$.paths['/api/participant-session'].get.responses['401'].description")
                        .value(containsString("PARTICIPANT_SESSION_UNAUTHORIZED")))
                .andExpect(jsonPath("$.paths['/api/participant-session/consent'].put.responses['409'].description")
                        .value(containsString("PARTICIPANT_SESSION_STATE_INVALID")))
                .andExpect(jsonPath("$.paths['/api/participant-session/browsing-completion'].post.responses['409'].description")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/participant-session/questionnaire/current'].get.responses['401'].description")
                        .exists())
                .andExpect(jsonPath("$.paths['/api/participant-session/questionnaire/answers/{itemId}'].put.responses['400'].description")
                        .value(containsString("PARTICIPANT_ANSWER_INVALID")))
                .andExpect(jsonPath("$.paths['/api/participant-session/questionnaire/submission'].post.responses['409'].description")
                        .value(containsString("PARTICIPANT_QUESTIONNAIRE_NOT_READY")))
                .andExpect(jsonPath("$.paths['/api/participant-session/abandonment'].post.responses['401'].description")
                        .exists());
    }

}
