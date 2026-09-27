package com.cs_42_3.surveyplatformbackend.common.exception;

import java.util.UUID;

import com.cs_42_3.surveyplatformbackend.security.CurrentResearcher;
import com.cs_42_3.surveyplatformbackend.study.api.StudyController;
import com.cs_42_3.surveyplatformbackend.study.service.StudyService;

import org.junit.jupiter.api.Test;

import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.CannotCreateTransactionException;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

@WebMvcTest(StudyController.class)
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerWebMvcTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StudyService studyService;

    @MockitoBean
    private CurrentResearcher currentResearcher;

    /** Verifies that malformed JSON returns 400 REQUEST_BODY_INVALID. */
    @Test
    void shouldReturnRequestBodyInvalidForMalformedJson() throws Exception {

        mockMvc.perform(
                        post("/api/studies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("REQUEST_BODY_INVALID"))
                .andExpect(jsonPath("$.error")
                        .value("Request body is invalid."));
    }

    /** Verifies that an unsupported content type returns 415 MEDIA_TYPE_NOT_SUPPORTED. */
    @Test
    void shouldReturnMediaTypeNotSupportedForUnsupportedContentType() throws Exception {

        mockMvc.perform(
                        post("/api/studies")
                                .contentType(MediaType.TEXT_PLAIN)
                                .content("hello")
                )
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code")
                        .value("MEDIA_TYPE_NOT_SUPPORTED"))
                .andExpect(jsonPath("$.error")
                        .value("Content type is not supported."));
    }

    /** Verifies that an unsupported HTTP method returns 405 METHOD_NOT_SUPPORTED. */
    @Test
    void shouldReturnMethodNotSupportedForUnsupportedHttpMethod() throws Exception {

        mockMvc.perform(
                        delete("/api/studies")
                )
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.code")
                        .value("METHOD_NOT_SUPPORTED"))
                .andExpect(jsonPath("$.error")
                        .value("HTTP method is not supported for this endpoint."));
    }

    /** Verifies that an invalid path parameter type returns 400 ARGUMENT_TYPE_MISMATCH. */
    @Test
    void shouldReturnArgumentTypeMismatchForInvalidUuid() throws Exception {

        mockMvc.perform(
                        get("/api/studies/not-a-valid-uuid")
                )
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code")
                        .value("ARGUMENT_TYPE_MISMATCH"))
                .andExpect(jsonPath("$.error")
                        .value("Request parameter has an invalid type."));
    }

    /** Verifies that database integrity errors return a safe 500 response. */
    @Test
    void shouldReturnInternalServerErrorForDataIntegrityViolation() throws Exception {

        UUID researcherId = UUID.randomUUID();

        when(currentResearcher.getId())
                .thenReturn(researcherId);

        when(studyService.createStudy(
                researcherId,
                "Test Study",
                "Test description",
                "DEFAULT"
        )).thenThrow(
                new DataIntegrityViolationException(
                        "secret database constraint information"
                )
        );

        mockMvc.perform(
                        post("/api/studies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "title": "Test Study",
                                      "description": "Test description",
                                      "templateCode": "DEFAULT"
                                    }
                                    """)
                )
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code")
                        .value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.error")
                        .value("An internal server error occurred."));
    }

    /** Verifies that database connection failures return a safe 500 response. */
    @Test
    void shouldReturnInternalServerErrorForDatabaseUnavailable() throws Exception {

        UUID researcherId = UUID.randomUUID();

        when(currentResearcher.getId())
                .thenReturn(researcherId);

        when(studyService.createStudy(
                researcherId,
                "Test Study",
                "Test description",
                "DEFAULT"
        )).thenThrow(
                new DataAccessResourceFailureException(
                        "secret database connection information"
                )
        );

        mockMvc.perform(
                        post("/api/studies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "title": "Test Study",
                                      "description": "Test description",
                                      "templateCode": "DEFAULT"
                                    }
                                    """)
                )
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code")
                        .value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.error")
                        .value("An internal server error occurred."));
    }

    /** Verifies that invalid database resource usage returns a safe 500 response. */
    @Test
    void shouldReturnInternalServerErrorForInvalidDatabaseResourceUsage() throws Exception {

        UUID researcherId = UUID.randomUUID();

        when(currentResearcher.getId())
                .thenReturn(researcherId);

        when(studyService.createStudy(
                researcherId,
                "Test Study",
                "Test description",
                "DEFAULT"
        )).thenThrow(
                new InvalidDataAccessResourceUsageException(
                        "secret SQL or schema information"
                )
        );

        mockMvc.perform(
                        post("/api/studies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "title": "Test Study",
                                      "description": "Test description",
                                      "templateCode": "DEFAULT"
                                    }
                                    """)
                )
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code")
                        .value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.error")
                        .value("An internal server error occurred."));
    }

    /** Verifies that transaction creation failures return a safe 500 response. */
    @Test
    void shouldReturnInternalServerErrorForTransactionCreationFailure() throws Exception {

        UUID researcherId = UUID.randomUUID();

        when(currentResearcher.getId())
                .thenReturn(researcherId);

        when(studyService.createStudy(
                researcherId,
                "Test Study",
                "Test description",
                "DEFAULT"
        )).thenThrow(
                new CannotCreateTransactionException(
                        "secret transaction or database connection information"
                )
        );

        mockMvc.perform(
                        post("/api/studies")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("""
                                    {
                                      "title": "Test Study",
                                      "description": "Test description",
                                      "templateCode": "DEFAULT"
                                    }
                                    """)
                )
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code")
                        .value("INTERNAL_SERVER_ERROR"))
                .andExpect(jsonPath("$.error")
                        .value("An internal server error occurred."));
    }
}
