package com.cs_42_3.surveyplatformbackend.survey.questionnaire.api;

import com.cs_42_3.surveyplatformbackend.config.SecurityConfig;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.SurveyErrorDetail;
import com.cs_42_3.surveyplatformbackend.survey.exception.SurveyExceptionHandler;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.QuestionnaireResponse;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.QuestionnaireSaveResult;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireNotFoundException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireLockedException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireValidationException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireVersionConflictException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.service.QuestionnaireService;
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

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP contract coverage for study questionnaire reads and whole-draft saves. */
@WebMvcTest(QuestionnaireController.class)
@Import({SecurityConfig.class, SurveyExceptionHandler.class})
@TestPropertySource(properties = {
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class QuestionnaireControllerTest {

    private static final UUID RESEARCHER_ID = UUID.fromString("a3a88bfc-06f4-4075-b72d-1531be1d234c");
    private static final UUID STUDY_ID = UUID.fromString("32a26b54-e053-40e7-9359-a95176da5158");
    private static final UUID QUESTIONNAIRE_ID = UUID.fromString("35514c7f-8328-4921-bf75-0c2d81068122");
    private static final UUID ITEM_ID = UUID.fromString("ea28f333-d766-426f-a20c-b0974ba655a4");
    private static final UUID QUESTION_ID = UUID.fromString("d1c3e81f-5d90-4742-8524-dee739e197e0");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuestionnaireService questionnaireService;

    @Test
    void getReturnsQuestionnaire() throws Exception {
        when(questionnaireService.getQuestionnaire(STUDY_ID)).thenReturn(response());

        mockMvc.perform(get("/api/studies/{studyId}/questionnaire", STUDY_ID)
                        .with(researcherJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(QUESTIONNAIRE_ID.toString()))
                .andExpect(jsonPath("$.studyId").value(STUDY_ID.toString()));
    }

    @Test
    void firstSaveReturnsCreatedAndResourceLocation() throws Exception {
        when(questionnaireService.saveQuestionnaire(eq(STUDY_ID), any()))
                .thenReturn(new QuestionnaireSaveResult(response(), true));

        mockMvc.perform(put("/api/studies/{studyId}/questionnaire", STUDY_ID)
                        .with(researcherJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedVersion": null,
                                  "items": [
                                    {"itemId": null, "questionId": "%s"}
                                  ]
                                }
                                """.formatted(QUESTION_ID)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "http://localhost/api/studies/" + STUDY_ID + "/questionnaire"
                ))
                .andExpect(jsonPath("$.id").value(QUESTIONNAIRE_ID.toString()))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void updateOrSafeReplayReturnsOkWithoutLocation() throws Exception {
        when(questionnaireService.saveQuestionnaire(eq(STUDY_ID), any()))
                .thenReturn(new QuestionnaireSaveResult(response(), false));

        mockMvc.perform(put("/api/studies/{studyId}/questionnaire", STUDY_ID)
                        .with(researcherJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion": 0, "items": []}
                                """))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"));
    }

    @Test
    void nullArrayElementReturnsIndexedStableDetailInsteadOfDeserializerFailure() throws Exception {
        mockMvc.perform(put("/api/studies/{studyId}/questionnaire", STUDY_ID)
                        .with(researcherJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion": 0, "items": [null]}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("QUESTIONNAIRE_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("items[0]"))
                .andExpect(jsonPath("$.details[0].index").value(0))
                .andExpect(jsonPath("$.details[0].code").value("ITEM_REQUIRED"));
        verifyNoInteractions(questionnaireService);
    }

    @Test
    void nullItemsCollectionIsRejectedButEmptyCollectionReachesService() throws Exception {
        mockMvc.perform(put("/api/studies/{studyId}/questionnaire", STUDY_ID)
                        .with(researcherJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion": 0, "items": null}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("QUESTIONNAIRE_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.details[0].field").value("items"));
    }

    @Test
    void businessValidationReturnsItemLocationAndItemId() throws Exception {
        SurveyErrorDetail detail = new SurveyErrorDetail(
                "items[0].questionId",
                0,
                ITEM_ID,
                "INVALID_QUESTION_REFERENCE",
                "Question does not exist or is not owned by the current researcher"
        );
        when(questionnaireService.saveQuestionnaire(eq(STUDY_ID), any()))
                .thenThrow(new QuestionnaireValidationException("Invalid question", List.of(detail)));

        mockMvc.perform(put("/api/studies/{studyId}/questionnaire", STUDY_ID)
                        .with(researcherJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "expectedVersion": 0,
                                  "items": [
                                    {"itemId": "%s", "questionId": "%s"}
                                  ]
                                }
                                """.formatted(ITEM_ID, QUESTION_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details[0].itemId").value(ITEM_ID.toString()))
                .andExpect(jsonPath("$.details[0].code").value("INVALID_QUESTION_REFERENCE"));
    }

    @Test
    void getMissingQuestionnaireReturnsStableNotFoundCode() throws Exception {
        when(questionnaireService.getQuestionnaire(STUDY_ID))
                .thenThrow(new QuestionnaireNotFoundException(STUDY_ID));

        mockMvc.perform(get("/api/studies/{studyId}/questionnaire", STUDY_ID)
                        .with(researcherJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUESTIONNAIRE_NOT_FOUND"))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void versionConflictReturnsConflict() throws Exception {
        when(questionnaireService.saveQuestionnaire(eq(STUDY_ID), any()))
                .thenThrow(new QuestionnaireVersionConflictException("Stale version"));

        mockMvc.perform(put("/api/studies/{studyId}/questionnaire", STUDY_ID)
                        .with(researcherJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion": 1, "items": []}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUESTIONNAIRE_VERSION_CONFLICT"));
    }

    @Test
    void lockedStudyReturnsConflict() throws Exception {
        when(questionnaireService.saveQuestionnaire(eq(STUDY_ID), any()))
                .thenThrow(new QuestionnaireLockedException(STUDY_ID));

        mockMvc.perform(put("/api/studies/{studyId}/questionnaire", STUDY_ID)
                        .with(researcherJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"expectedVersion": 0, "items": []}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("QUESTIONNAIRE_LOCKED"));
    }

    @Test
    void malformedStudyIdReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/studies/not-a-uuid/questionnaire")
                        .with(researcherJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"));
    }

    @Test
    void unauthenticatedAccessIsRejectedBeforeService() throws Exception {
        mockMvc.perform(get("/api/studies/{studyId}/questionnaire", STUDY_ID))
                .andExpect(status().isUnauthorized());
        verifyNoInteractions(questionnaireService);
    }

    @Test
    void nonResearcherRoleIsForbidden() throws Exception {
        mockMvc.perform(get("/api/studies/{studyId}/questionnaire", STUDY_ID)
                        .with(jwt().jwt(token -> token.subject(RESEARCHER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_PARTICIPANT"))))
                .andExpect(status().isForbidden());
    }

    private QuestionnaireResponse response() {
        return new QuestionnaireResponse(
                QUESTIONNAIRE_ID,
                STUDY_ID,
                List.of(),
                0L,
                Instant.parse("2026-09-21T10:00:00Z")
        );
    }

    private RequestPostProcessor researcherJwt() {
        return jwt().jwt(token -> token.subject(RESEARCHER_ID.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_RESEARCHER"));
    }
}
