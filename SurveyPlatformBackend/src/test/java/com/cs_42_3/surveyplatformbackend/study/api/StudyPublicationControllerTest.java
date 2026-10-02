package com.cs_42_3.surveyplatformbackend.study.api;

import com.cs_42_3.surveyplatformbackend.config.SecurityConfig;
import com.cs_42_3.surveyplatformbackend.security.CurrentResearcher;
import com.cs_42_3.surveyplatformbackend.study.exception.FeedNotReadyException;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyNotFoundException;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyNotPublishableException;
import com.cs_42_3.surveyplatformbackend.study.exception.StudyVersionConflictException;
import com.cs_42_3.surveyplatformbackend.study.service.ParticipationLinks;
import com.cs_42_3.surveyplatformbackend.study.service.StudyPublicationService;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.SurveyErrorDetail;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.exception.QuestionnairePublicationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Verifies the publication endpoint's stable success-independent error envelope. */
@WebMvcTest(StudyPublicationController.class)
@Import({SecurityConfig.class, StudyPublicationExceptionHandler.class})
@TestPropertySource(properties = {
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class StudyPublicationControllerTest {

    private static final UUID OWNER_ID =
            UUID.fromString("59dc10db-563a-4a6a-baa1-c583f3737900");
    private static final UUID STUDY_ID =
            UUID.fromString("38a09a5c-14cd-42df-a980-5bd0c117f783");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudyPublicationService publicationService;

    @MockitoBean
    private CurrentResearcher currentResearcher;

    @MockitoBean
    private ParticipationLinks participationLinks;

    @BeforeEach
    void setUp() {
        when(currentResearcher.getId()).thenReturn(OWNER_ID);
    }

    @Test
    void studyNotPublishableUsesStableSurveyErrorEnvelope() throws Exception {
        when(publicationService.publish(OWNER_ID, STUDY_ID, 0L))
                .thenThrow(new StudyNotPublishableException());

        performPublish("{\"version\":0}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STUDY_NOT_PUBLISHABLE"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.timestamp").isString())
                .andExpect(jsonPath("$.path").value(publishPath()))
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void feedNotReadyUsesStableSurveyErrorEnvelope() throws Exception {
        when(publicationService.publish(OWNER_ID, STUDY_ID, 0L))
                .thenThrow(new FeedNotReadyException());

        performPublish("{\"version\":0}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("FEED_NOT_READY"))
                .andExpect(jsonPath("$.path").value(publishPath()))
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void staleVersionUsesStableSurveyErrorEnvelope() throws Exception {
        when(publicationService.publish(OWNER_ID, STUDY_ID, 0L))
                .thenThrow(new StudyVersionConflictException());

        performPublish("{\"version\":0}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("STUDY_VERSION_CONFLICT"))
                .andExpect(jsonPath("$.path").value(publishPath()))
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void missingStudyUsesStableSurveyErrorEnvelope() throws Exception {
        when(publicationService.publish(OWNER_ID, STUDY_ID, 0L))
                .thenThrow(new StudyNotFoundException());

        performPublish("{\"version\":0}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STUDY_NOT_FOUND"))
                .andExpect(jsonPath("$.path").value(publishPath()))
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void questionnaireValidationPreservesStructuredDetails() throws Exception {
        SurveyErrorDetail detail = new SurveyErrorDetail(
                "items[0].questionId",
                0,
                null,
                null,
                "MISSING_QUESTION",
                "Question is missing"
        );
        when(publicationService.publish(OWNER_ID, STUDY_ID, 0L))
                .thenThrow(new QuestionnairePublicationException(
                        "Questionnaire cannot be published",
                        List.of(detail)
                ));

        performPublish("{\"version\":0}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code")
                        .value("QUESTIONNAIRE_PUBLICATION_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("items[0].questionId"))
                .andExpect(jsonPath("$.details[0].code").value("MISSING_QUESTION"));
    }

    @Test
    void invalidVersionUsesStableSurveyErrorEnvelopeBeforeCallingService() throws Exception {
        performPublish("{\"version\":-1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.path").value(publishPath()))
                .andExpect(jsonPath("$.details").isEmpty());

        verifyNoInteractions(publicationService);
    }

    @Test
    void malformedBodyUsesStableSurveyErrorEnvelopeBeforeCallingService() throws Exception {
        performPublish("{")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.path").value(publishPath()))
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.error").doesNotExist());

        verifyNoInteractions(publicationService);
    }

    @Test
    void malformedStudyIdUsesStableSurveyErrorEnvelopeBeforeCallingService() throws Exception {
        String path = "/api/studies/not-a-uuid/publish";

        mockMvc.perform(post(path)
                        .with(researcherJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"version\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.path").value(path))
                .andExpect(jsonPath("$.details").isEmpty())
                .andExpect(jsonPath("$.error").doesNotExist());

        verifyNoInteractions(publicationService);
    }

    private org.springframework.test.web.servlet.ResultActions performPublish(String body)
            throws Exception {
        return mockMvc.perform(post(publishPath())
                .with(researcherJwt())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private String publishPath() {
        return "/api/studies/" + STUDY_ID + "/publish";
    }

    private RequestPostProcessor researcherJwt() {
        return jwt()
                .jwt(token -> token.subject(OWNER_ID.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_RESEARCHER"));
    }
}
