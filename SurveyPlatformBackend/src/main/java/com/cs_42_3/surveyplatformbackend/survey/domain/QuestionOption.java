package com.cs_42_3.surveyplatformbackend.survey.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

/**
 * Ordered answer option owned by a choice-based {@link Question}.
 */
@Getter
@Entity
@Table(
        name = "question_options",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_question_options_question_order",
                columnNames = {"question_id", "option_order"}
        )
)
public class QuestionOption {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @Column(name = "option_text", nullable = false, columnDefinition = "TEXT")
    private String optionText;

    @Column(name = "option_order", nullable = false)
    private int optionOrder;

    protected QuestionOption() {}

    private QuestionOption(Question question, String optionText, int optionOrder) {
        this.question = Objects.requireNonNull(question, "Question is required");
        this.optionText = Objects.requireNonNull(optionText, "Option text is required");
        this.optionOrder = optionOrder;
    }

    static QuestionOption create(Question question, String optionText, int optionOrder) {
        return new QuestionOption(question, optionText, optionOrder);
    }
}
