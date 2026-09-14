package com.cs_42_3.surveyplatformbackend.survey.service;

import com.cs_42_3.surveyplatformbackend.survey.api.dto.CreateQuestionRequest;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.QuestionResponse;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.QuestionSummaryResponse;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.UpdateQuestionRequest;
import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;

import java.util.List;
import java.util.UUID;

/**
 * Defines researcher-owned question-bank operations for FR32-FR35.
 */
public interface QuestionService {

    List<QuestionSummaryResponse> listQuestions(QuestionType type, String keyword);

    QuestionResponse getQuestion(UUID questionId);

    QuestionResponse createQuestion(CreateQuestionRequest request);

    QuestionResponse updateQuestion(UUID questionId, UpdateQuestionRequest request);

    void deleteQuestion(UUID questionId);
}
