package com.cs_42_3.surveyplatformbackend.survey.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * A single option belonging to a SINGLE_CHOICE or MULTI_CHOICE question.
 *
 * Deliberately does NOT carry a "next question" / jump-target field here — UC-25
 * (branch configuration) is a survey-level concern (an option's jump target only makes
 * sense in the context of one specific survey's question ordering), so that mapping
 * belongs in the survey-layer entity that references this question, not in the
 * question-bank layer itself. Keeping it out of this table is what lets one bank question
 * be reused across multiple surveys with different jump targets in each.
 */
@Entity
@Table(name = "question_options")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "option_text", nullable = false, columnDefinition = "TEXT")
    private String optionText;

    @Column(name = "option_order", nullable = false)
    private Integer optionOrder;
}
