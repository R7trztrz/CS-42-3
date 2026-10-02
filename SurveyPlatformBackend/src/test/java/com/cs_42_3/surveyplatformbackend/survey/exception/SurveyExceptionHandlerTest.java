package com.cs_42_3.surveyplatformbackend.survey.exception;

import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

/** Unit coverage for constraint-name-based persistence error classification. */
class SurveyExceptionHandlerTest {

    private final SurveyExceptionHandler handler = new SurveyExceptionHandler();
    private final MockHttpServletRequest request = new MockHttpServletRequest(
            "PUT",
            "/api/studies/study-id/questionnaire"
    );

    @Test
    void questionnaireStudyUniqueConstraintMapsToConcurrentCreationConflict() {
        var response = handler.handlePersistenceFailure(
                violation("uq_questionnaires_study"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("CONCURRENT_QUESTIONNAIRE_CREATION");
    }

    @Test
    void questionForeignKeyConstraintMapsToChangedReferenceConflict() {
        var response = handler.handlePersistenceFailure(
                violation("fk_questionnaire_items_question"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("QUESTION_REFERENCE_CHANGED");
    }

    @Test
    void branchTargetForeignKeyMapsToChangedReferenceConflict() {
        var response = handler.handlePersistenceFailure(
                violation("fk_branch_rules_target_item"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody().code()).isEqualTo("QUESTION_REFERENCE_CHANGED");
    }

    @Test
    void unknownIntegrityConstraintIsNotMisreportedAsConcurrentCreation() {
        var response = handler.handlePersistenceFailure(
                violation("unexpected_constraint"),
                request
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
    }

    private DataIntegrityViolationException violation(String constraintName) {
        ConstraintViolationException cause = new ConstraintViolationException(
                "Constraint failed",
                new SQLException("test-only failure"),
                constraintName
        );
        return new DataIntegrityViolationException("Persistence failed", cause);
    }
}
