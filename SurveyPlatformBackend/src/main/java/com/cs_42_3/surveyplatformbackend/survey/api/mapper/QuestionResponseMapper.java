package com.cs_42_3.surveyplatformbackend.survey.api.mapper;

import com.cs_42_3.surveyplatformbackend.survey.api.dto.QuestionOptionResponse;
import com.cs_42_3.surveyplatformbackend.survey.api.dto.QuestionResponse;
import com.cs_42_3.surveyplatformbackend.survey.domain.Question;
import org.springframework.stereotype.Component;

import java.util.List;

/** Maps a fully loaded question aggregate to its shared API representation. */
@Component
public class QuestionResponseMapper {

    public QuestionResponse toResponse(Question question) {
        List<QuestionOptionResponse> options = question.getOptions().stream()
                .map(option -> new QuestionOptionResponse(
                        option.getId(),
                        option.getOptionText(),
                        option.getOptionOrder()
                ))
                .toList();

        return new QuestionResponse(
                question.getId(),
                question.getType(),
                question.getQuestionText(),
                question.isRequired(),
                options,
                question.getScaleMin(),
                question.getScaleMax(),
                question.getScaleMinLabel(),
                question.getScaleMaxLabel(),
                question.getCreatedAt(),
                question.getUpdatedAt()
        );
    }
}
