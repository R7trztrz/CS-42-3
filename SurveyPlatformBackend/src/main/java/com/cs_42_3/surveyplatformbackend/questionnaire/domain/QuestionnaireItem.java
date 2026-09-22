package com.cs_42_3.surveyplatformbackend.questionnaire.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * One enabled question-bank reference at a fixed position within a {@link Questionnaire}.
 * <p>
 * References the live {@code questions} row (V3) by ID only; the domain layer does not
 * depend on the survey module, so the service layer resolves and attaches the live
 * question content when building an API response.
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

    @Column(name = "question_id", nullable = false, updatable = false)
    private UUID questionId;

    @Column(name = "item_order", nullable = false)
    private int itemOrder;

    @OneToMany(mappedBy = "sourceItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<QuestionnaireBranchRule> branchRules = new ArrayList<>();

    protected QuestionnaireItem() {}

    private QuestionnaireItem(Questionnaire questionnaire, UUID questionId, int itemOrder) {
        this.questionnaire = Objects.requireNonNull(questionnaire, "Questionnaire is required");
        this.questionId = Objects.requireNonNull(questionId, "Question ID is required");
        this.itemOrder = itemOrder;
    }

    static QuestionnaireItem create(Questionnaire questionnaire, UUID questionId, int itemOrder) {
        return new QuestionnaireItem(questionnaire, questionId, itemOrder);
    }

    void addBranchRule(QuestionnaireBranchRule branchRule) {
        branchRules.add(Objects.requireNonNull(branchRule, "Branch rule is required"));
    }

    /**
     * Returns an immutable snapshot of this item's branch rules.
     *
     * @return branch rules in no particular order
     */
    public List<QuestionnaireBranchRule> getBranchRules() {
        return List.copyOf(branchRules);
    }
}
