package com.cs_42_3.surveyplatformbackend.survey.api;

import com.cs_42_3.surveyplatformbackend.config.SecurityConfig;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.QuestionOptionResponse;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.QuestionResponse;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.QuestionSummaryResponse;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;
import com.cs_42_3.surveyplatformbackend.survey.exception.InvalidQuestionDataException;
import com.cs_42_3.surveyplatformbackend.survey.exception.InvalidResearcherIdentityException;
import com.cs_42_3.surveyplatformbackend.survey.exception.QuestionNotFoundException;
import com.cs_42_3.surveyplatformbackend.survey.exception.SurveyExceptionHandler;
import com.cs_42_3.surveyplatformbackend.survey.service.QuestionService;

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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * MockMvc tests for the FR32-FR35 question-bank HTTP contract.
 */
@WebMvcTest(QuestionController.class)
@Import({SecurityConfig.class, SurveyExceptionHandler.class})
@TestPropertySource(properties = {
        "security.jwt.secret=MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY="
})
class QuestionControllerTest {

    private static final UUID RESEARCHER_ID =
            UUID.fromString("217bb280-5560-4c76-9bd0-64534d7f78d5");
    private static final UUID QUESTION_ID =
            UUID.fromString("b7b9e646-3a4a-4256-8b9e-d8b75671f402");
    private static final UUID OLDER_QUESTION_ID =
            UUID.fromString("4e21e446-3471-4f83-bccf-12ee211f266a");
    private static final UUID OPTION_ID =
            UUID.fromString("739d73b1-f1d2-45e7-9a5c-b4cc425917f8");
    private static final Instant UPDATED_AT = Instant.parse("2026-09-14T06:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private QuestionService questionService;

    @Test
    void listQuestionsReturnsSummaryResponseAndPassesFilters() throws Exception {
        when(questionService.listQuestions(QuestionType.TEXT, "feedback"))
                .thenReturn(List.of(
                        new QuestionSummaryResponse(
                                QUESTION_ID,
                                QuestionType.TEXT,
                                "Share feedback",
                                UPDATED_AT
                        ),
                        new QuestionSummaryResponse(
                                OLDER_QUESTION_ID,
                                QuestionType.TEXT,
                                "Older feedback",
                                UPDATED_AT.minusSeconds(60)
                        )
                ));

        mockMvc.perform(get("/api/questions")
                        .with(researcherJwt())
                        .param("type", "TEXT")
                        .param("keyword", "feedback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(QUESTION_ID.toString()))
                .andExpect(jsonPath("$[0].type").value("TEXT"))
                .andExpect(jsonPath("$[0].questionText").value("Share feedback"))
                .andExpect(jsonPath("$[0].updatedAt").value(UPDATED_AT.toString()))
                .andExpect(jsonPath("$[0].options").doesNotExist())
                .andExpect(jsonPath("$[1].id").value(OLDER_QUESTION_ID.toString()));

        verify(questionService).listQuestions(QuestionType.TEXT, "feedback");
    }

    @Test
    void getQuestionReturnsCompleteQuestionSettings() throws Exception {
        when(questionService.getQuestion(QUESTION_ID)).thenReturn(questionResponse());

        mockMvc.perform(get("/api/questions/{questionId}", QUESTION_ID)
                        .with(researcherJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(QUESTION_ID.toString()))
                .andExpect(jsonPath("$.required").value(true))
                .andExpect(jsonPath("$.options[0].id").value(OPTION_ID.toString()))
                .andExpect(jsonPath("$.options[0].optionOrder").value(0));
    }

    @Test
    void createQuestionReturnsCreatedResponseAndLocation() throws Exception {
        when(questionService.createQuestion(any())).thenReturn(questionResponse());

        mockMvc.perform(post("/api/questions")
                        .with(researcherJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "SINGLE_CHOICE",
                                  "questionText": "Choose one",
                                  "required": true,
                                  "options": [
                                    {"optionText": "Yes"},
                                    {"optionText": "No"}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location",
                        "http://localhost/api/questions/" + QUESTION_ID
                ))
                .andExpect(jsonPath("$.id").value(QUESTION_ID.toString()));
    }

    @Test
    void updateQuestionReturnsUpdatedResponse() throws Exception {
        when(questionService.updateQuestion(any(UUID.class), any())).thenReturn(questionResponse());

        mockMvc.perform(put("/api/questions/{questionId}", QUESTION_ID)
                        .with(researcherJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "SINGLE_CHOICE",
                                  "questionText": "Choose one",
                                  "required": true,
                                  "options": [
                                    {"optionText": "Yes"},
                                    {"optionText": "No"}
                                  ]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(QUESTION_ID.toString()));
    }

    @Test
    void deleteQuestionReturnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/questions/{questionId}", QUESTION_ID)
                        .with(researcherJwt()))
                .andExpect(status().isNoContent());

        verify(questionService).deleteQuestion(QUESTION_ID);
    }

    @Test
    void listQuestionsWithoutAuthenticationReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/questions"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(questionService);
    }

    @Test
    void listQuestionsWithoutResearcherRoleReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/questions")
                        .with(jwt()
                                .jwt(token -> token.subject(RESEARCHER_ID.toString()))
                                .authorities(new SimpleGrantedAuthority("ROLE_PARTICIPANT"))))
                .andExpect(status().isForbidden());

        verifyNoInteractions(questionService);
    }

    @Test
    void createQuestionWithBlankTextReturnsUnifiedBadRequest() throws Exception {
        mockMvc.perform(post("/api/questions")
                        .with(researcherJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "TEXT",
                                  "questionText": "   ",
                                  "required": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value(
                        "questionText: Question text is required"
                ))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.path").value("/api/questions"));

        verifyNoInteractions(questionService);
    }

    @Test
    void createQuestionWithUnsupportedTypeReturnsUnifiedBadRequest() throws Exception {
        mockMvc.perform(post("/api/questions")
                        .with(researcherJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "RANKING",
                                  "questionText": "Rank the options",
                                  "required": false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.path").value("/api/questions"));

        verifyNoInteractions(questionService);
    }

    @Test
    void createQuestionWithMalformedJsonReturnsUnifiedBadRequest() throws Exception {
        mockMvc.perform(post("/api/questions")
                        .with(researcherJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "TEXT",
                                  "questionText":
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.path").value("/api/questions"));

        verifyNoInteractions(questionService);
    }

    @Test
    void createQuestionWithInvalidQuestionDataReturnsUnifiedBadRequest() throws Exception {
        when(questionService.createQuestion(any()))
                .thenThrow(new InvalidQuestionDataException("At least two options are required"));

        mockMvc.perform(post("/api/questions")
                        .with(researcherJwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "type": "SINGLE_CHOICE",
                                  "questionText": "Choose one",
                                  "required": false,
                                  "options": [
                                    {"optionText": "Only option"}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("QUESTION_VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").value("At least two options are required"))
                .andExpect(jsonPath("$.path").value("/api/questions"));
    }

    @Test
    void invalidResearcherIdentityReturnsUnifiedUnauthorized() throws Exception {
        when(questionService.listQuestions(null, null))
                .thenThrow(new InvalidResearcherIdentityException(
                        "Authenticated researcher subject is not a UUID"
                ));

        mockMvc.perform(get("/api/questions")
                        .with(researcherJwt()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_RESEARCHER_IDENTITY"))
                .andExpect(jsonPath("$.path").value("/api/questions"));
    }

    @Test
    void getQuestionForMissingOrUnownedResourceReturnsUnifiedNotFound() throws Exception {
        when(questionService.getQuestion(QUESTION_ID))
                .thenThrow(new QuestionNotFoundException(QUESTION_ID));

        mockMvc.perform(get("/api/questions/{questionId}", QUESTION_ID)
                        .with(researcherJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("QUESTION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Question not found: " + QUESTION_ID))
                .andExpect(jsonPath("$.path").value("/api/questions/" + QUESTION_ID));
    }

    @Test
    void getQuestionWithMalformedUuidReturnsUnifiedBadRequest() throws Exception {
        mockMvc.perform(get("/api/questions/not-a-uuid")
                        .with(researcherJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
                .andExpect(jsonPath("$.path").value("/api/questions/not-a-uuid"));

        verifyNoInteractions(questionService);
    }

    private RequestPostProcessor researcherJwt() {
        return jwt()
                .jwt(token -> token.subject(RESEARCHER_ID.toString()))
                .authorities(new SimpleGrantedAuthority("ROLE_RESEARCHER"));
    }

    private QuestionResponse questionResponse() {
        return new QuestionResponse(
                QUESTION_ID,
                QuestionType.SINGLE_CHOICE,
                "Choose one",
                true,
                List.of(new QuestionOptionResponse(OPTION_ID, "Yes", 0)),
                null,
                null,
                null,
                null,
                UPDATED_AT.minusSeconds(60),
                UPDATED_AT
        );
    }
}
