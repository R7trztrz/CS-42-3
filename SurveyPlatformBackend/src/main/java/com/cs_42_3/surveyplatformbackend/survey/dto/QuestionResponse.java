package com.cs_42_3.surveyplatformbackend.survey.dto;

import com.cs_42_3.surveyplatformbackend.survey.entity.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionResponse {
    private Long id;
    private QuestionType type;
    private String questionText;
    private List<QuestionOptionResponse> options;
    private Integer scaleMin;
    private Integer scaleMax;
    private String scaleMinLabel;
    private String scaleMaxLabel;
    private Instant createdAt;
    private Instant updatedAt;
}
