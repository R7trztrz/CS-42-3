package com.cs_42_3.surveyplatformbackend.survey.exception;

import com.cs_42_3.surveyplatformbackend.survey.api.QuestionController;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.SurveyErrorDetail;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.SurveyErrorResponse;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.QuestionnaireController;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.SaveQuestionnaireItemRequest;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.api.dto.SaveQuestionnaireRequest;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.ConcurrentQuestionnaireCreationException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionReferenceChangedException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireLockedException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireNotFoundException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireValidationException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.QuestionnaireVersionConflictException;
import com.cs_42_3.surveyplatformbackend.survey.questionnaire.exception.StudyNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.persistence.RollbackException;
import jakarta.servlet.http.HttpServletRequest;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Converts M4 question and questionnaire failures into stable API errors. */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(assignableTypes = {
        QuestionController.class,
        QuestionnaireController.class
})
public class SurveyExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(SurveyExceptionHandler.class);
    private static final Pattern ITEM_FIELD = Pattern.compile("items\\[(\\d+)](?:\\.(.+))?");

    @ExceptionHandler(QuestionNotFoundException.class)
    public ResponseEntity<SurveyErrorResponse> handleQuestionNotFound(
            QuestionNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, "QUESTION_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidQuestionDataException.class)
    public ResponseEntity<SurveyErrorResponse> handleInvalidQuestionData(
            InvalidQuestionDataException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.BAD_REQUEST, "QUESTION_VALIDATION_ERROR", exception.getMessage(), request);
    }

    @ExceptionHandler(InvalidResearcherIdentityException.class)
    public ResponseEntity<SurveyErrorResponse> handleInvalidResearcherIdentity(
            InvalidResearcherIdentityException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.UNAUTHORIZED,
                "INVALID_RESEARCHER_IDENTITY",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(StudyNotFoundException.class)
    public ResponseEntity<SurveyErrorResponse> handleStudyNotFound(
            StudyNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(HttpStatus.NOT_FOUND, "STUDY_NOT_FOUND", exception.getMessage(), request);
    }

    @ExceptionHandler(QuestionnaireNotFoundException.class)
    public ResponseEntity<SurveyErrorResponse> handleQuestionnaireNotFound(
            QuestionnaireNotFoundException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.NOT_FOUND,
                "QUESTIONNAIRE_NOT_FOUND",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(QuestionnaireValidationException.class)
    public ResponseEntity<SurveyErrorResponse> handleQuestionnaireValidation(
            QuestionnaireValidationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "QUESTIONNAIRE_VALIDATION_ERROR",
                exception.getMessage(),
                request,
                exception.getDetails()
        );
    }

    @ExceptionHandler(QuestionnaireLockedException.class)
    public ResponseEntity<SurveyErrorResponse> handleQuestionnaireLocked(
            QuestionnaireLockedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "QUESTIONNAIRE_LOCKED",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(QuestionnaireVersionConflictException.class)
    public ResponseEntity<SurveyErrorResponse> handleQuestionnaireVersionConflict(
            QuestionnaireVersionConflictException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "QUESTIONNAIRE_VERSION_CONFLICT",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(ConcurrentQuestionnaireCreationException.class)
    public ResponseEntity<SurveyErrorResponse> handleConcurrentCreation(
            ConcurrentQuestionnaireCreationException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "CONCURRENT_QUESTIONNAIRE_CREATION",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler(QuestionReferenceChangedException.class)
    public ResponseEntity<SurveyErrorResponse> handleQuestionReferenceChanged(
            QuestionReferenceChangedException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "QUESTION_REFERENCE_CHANGED",
                exception.getMessage(),
                request
        );
    }

    @ExceptionHandler({
            ObjectOptimisticLockingFailureException.class,
            OptimisticLockException.class
    })
    public ResponseEntity<SurveyErrorResponse> handleOptimisticLockingFailure(
            RuntimeException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.CONFLICT,
                "QUESTIONNAIRE_VERSION_CONFLICT",
                "Questionnaire was changed concurrently; reload it and retry",
                request
        );
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<SurveyErrorResponse> handleMethodArgumentNotValid(
            MethodArgumentNotValidException exception,
            HttpServletRequest request
    ) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .findFirst()
                .orElse("Request validation failed");
        boolean hasItemError = exception.getBindingResult().getFieldErrors().stream()
                .anyMatch(error -> error.getField().startsWith("items"));
        if (hasItemError
                && exception.getBindingResult().getTarget() instanceof SaveQuestionnaireRequest target) {
            List<SurveyErrorDetail> details = exception.getBindingResult().getFieldErrors().stream()
                    .map(error -> toValidationDetail(error, target))
                    .toList();
            return buildResponse(
                    HttpStatus.BAD_REQUEST,
                    "QUESTIONNAIRE_VALIDATION_ERROR",
                    message,
                    request,
                    details
            );
        }
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", message, request);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<SurveyErrorResponse> handleHttpMessageNotReadable(
            HttpMessageNotReadableException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_REQUEST",
                "Request body is malformed or contains an unsupported value",
                request
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<SurveyErrorResponse> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException exception,
            HttpServletRequest request
    ) {
        return buildResponse(
                HttpStatus.BAD_REQUEST,
                "MALFORMED_REQUEST",
                "Path or query parameter has an invalid value",
                request
        );
    }

    @ExceptionHandler({
            DataIntegrityViolationException.class,
            TransactionSystemException.class,
            RollbackException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<SurveyErrorResponse> handlePersistenceFailure(
            Exception exception,
            HttpServletRequest request
    ) {
        if (hasCause(exception, OptimisticLockException.class)) {
            return buildResponse(
                    HttpStatus.CONFLICT,
                    "QUESTIONNAIRE_VERSION_CONFLICT",
                    "Questionnaire was changed concurrently; reload it and retry",
                    request
            );
        }
        String constraintName = findConstraintName(exception);
        if ("uq_questionnaires_study".equalsIgnoreCase(constraintName)) {
            return buildResponse(
                    HttpStatus.CONFLICT,
                    "CONCURRENT_QUESTIONNAIRE_CREATION",
                    "The questionnaire was created concurrently; reload it and retry",
                    request
            );
        }
        if ("fk_questionnaire_items_question".equalsIgnoreCase(constraintName)) {
            return buildResponse(
                    HttpStatus.CONFLICT,
                    "QUESTION_REFERENCE_CHANGED",
                    "A referenced question changed while the questionnaire was being saved",
                    request
            );
        }

        LOGGER.error("Unrecognized questionnaire persistence failure (constraint: {})", constraintName, exception);
        return buildResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "INTERNAL_ERROR",
                "The request could not be completed",
                request
        );
    }

    private SurveyErrorDetail toValidationDetail(
            FieldError error,
            SaveQuestionnaireRequest target
    ) {
        Matcher matcher = ITEM_FIELD.matcher(error.getField());
        if (!matcher.matches()) {
            return new SurveyErrorDetail(
                    error.getField(),
                    null,
                    null,
                    "FIELD_INVALID",
                    error.getDefaultMessage()
            );
        }
        int index = Integer.parseInt(matcher.group(1));
        String nestedField = matcher.group(2);
        UUID itemId = null;
        if (target.items() != null && index < target.items().size()) {
            SaveQuestionnaireItemRequest item = target.items().get(index);
            itemId = item == null ? null : item.itemId();
        }
        String code = nestedField == null ? "ITEM_REQUIRED" : "QUESTION_ID_REQUIRED";
        return new SurveyErrorDetail(error.getField(), index, itemId, code, error.getDefaultMessage());
    }

    private String findConstraintName(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolation
                    && constraintViolation.getConstraintName() != null) {
                return constraintViolation.getConstraintName();
            }
            current = current.getCause();
        }
        return null;
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private ResponseEntity<SurveyErrorResponse> buildResponse(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request
    ) {
        return buildResponse(status, code, message, request, List.of());
    }

    private ResponseEntity<SurveyErrorResponse> buildResponse(
            HttpStatus status,
            String code,
            String message,
            HttpServletRequest request,
            List<SurveyErrorDetail> details
    ) {
        SurveyErrorResponse response = new SurveyErrorResponse(
                code,
                message,
                Instant.now(),
                request.getRequestURI(),
                details
        );
        return ResponseEntity.status(status).body(response);
    }
}
