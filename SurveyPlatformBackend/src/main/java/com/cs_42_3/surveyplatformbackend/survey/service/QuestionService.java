package com.cs_42_3.surveyplatformbackend.survey.service;

import com.cs_42_3.surveyplatformbackend.survey.api.dto.CreateQuestionRequest;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.QuestionResponse;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.QuestionPageResponse;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.UpdateQuestionRequest;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;

import java.util.UUID;

/**
 * Defines researcher-owned question-bank operations for FR32-FR35.
 */
public interface QuestionService {

    QuestionPageResponse listQuestions(
            QuestionType type,
            String search,
            int page,
            int size
    );

    QuestionResponse getQuestion(UUID questionId);

    QuestionResponse createQuestion(CreateQuestionRequest request);

    QuestionResponse updateQuestion(UUID questionId, UpdateQuestionRequest request);

    void deleteQuestion(UUID questionId);
}
