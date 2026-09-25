package com.cs_42_3.surveyplatformbackend.survey.questionnaire.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

/**
 * One stable, ordered question reference within a questionnaire draft.
 */
@Getter
@Entity
@Table(name = "questionnaire_items")
public class QuestionnaireItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "questionnaire_id", nullable = false)
    private Questionnaire questionnaire;

    @Column(name = "question_id")
    private UUID questionId;

    @Column(name = "position", nullable = false)
    private int position;

    protected QuestionnaireItem() {}

    static QuestionnaireItem create(
            Questionnaire questionnaire,
            UUID questionId,
            int position
    ) {
        QuestionnaireItem item = new QuestionnaireItem();
        item.questionnaire = Objects.requireNonNull(questionnaire, "Questionnaire is required");
        item.questionId = Objects.requireNonNull(questionId, "Question ID is required");
        item.position = position;
        return item;
    }

    void attachTo(Questionnaire questionnaire) {
        this.questionnaire = Objects.requireNonNull(questionnaire, "Questionnaire is required");
    }

    void detach() {
        questionnaire = null;
    }

    boolean replaceQuestion(UUID newQuestionId) {
        Objects.requireNonNull(newQuestionId, "Question ID is required");
        if (Objects.equals(questionId, newQuestionId)) {
            return false;
        }
        questionId = newQuestionId;
        return true;
    }

    boolean moveTo(int newPosition) {
        if (position == newPosition) {
            return false;
        }
        position = newPosition;
        return true;
    }
}
