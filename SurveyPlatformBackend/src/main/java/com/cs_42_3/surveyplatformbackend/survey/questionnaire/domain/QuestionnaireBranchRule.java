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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

import java.util.Objects;
import java.util.UUID;

/** One validated FR38 answer trigger and its target item. */
@Getter
@Entity
@Table(
        name = "questionnaire_branch_rules",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_branch_rules_source_option",
                        columnNames = {"source_item_id", "source_option_id"}
                ),
                @UniqueConstraint(
                        name = "uk_branch_rules_source_scale_value",
                        columnNames = {"source_item_id", "source_scale_value"}
                )
        }
)
public class QuestionnaireBranchRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "source_item_id", nullable = false)
    private QuestionnaireItem sourceItem;

    @Column(name = "source_option_id")
    private UUID sourceOptionId;

    @Column(name = "source_scale_value")
    private Integer sourceScaleValue;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "target_item_id", nullable = false)
    private QuestionnaireItem targetItem;

    protected QuestionnaireBranchRule() {}

    static QuestionnaireBranchRule create(
            QuestionnaireItem sourceItem,
            UUID sourceOptionId,
            Integer sourceScaleValue,
            QuestionnaireItem targetItem
    ) {
        if ((sourceOptionId == null) == (sourceScaleValue == null)) {
            throw new IllegalArgumentException(
                    "Exactly one of sourceOptionId or sourceScaleValue is required"
            );
        }
        QuestionnaireBranchRule rule = new QuestionnaireBranchRule();
        rule.sourceItem = Objects.requireNonNull(sourceItem, "Source item is required");
        rule.sourceOptionId = sourceOptionId;
        rule.sourceScaleValue = sourceScaleValue;
        rule.targetItem = Objects.requireNonNull(targetItem, "Target item is required");
        return rule;
    }
}
