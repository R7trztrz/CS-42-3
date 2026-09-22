package com.cs_42_3.surveyplatformbackend.questionnaire.domain;

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
 * One researcher-configured jump rule: the item and answer that triggers it, and the
 * enabled item it sends the participant to (FR-38).
 * <p>
 * Exactly one of {@link #getSourceOptionId()} or {@link #getSourceScaleValue()} is set,
 * matching the source item's question type. This keeps "which answer was given" a
 * function of a single trigger value, which is what makes runtime jump resolution
 * deterministic (NFR-15); the service layer enforces the type match and a database
 * check constraint mirrors the single-trigger invariant.
 */
@Getter
@Entity
@Table(name = "questionnaire_branch_rules")
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

    private QuestionnaireBranchRule(
            QuestionnaireItem sourceItem,
            UUID sourceOptionId,
            Integer sourceScaleValue,
            QuestionnaireItem targetItem
    ) {
        this.sourceItem = Objects.requireNonNull(sourceItem, "Source item is required");
        this.targetItem = Objects.requireNonNull(targetItem, "Target item is required");
        if ((sourceOptionId == null) == (sourceScaleValue == null)) {
            throw new IllegalArgumentException(
                    "Exactly one of sourceOptionId or sourceScaleValue must be set"
            );
        }
        this.sourceOptionId = sourceOptionId;
        this.sourceScaleValue = sourceScaleValue;
    }

    static QuestionnaireBranchRule create(
            QuestionnaireItem sourceItem,
            UUID sourceOptionId,
            Integer sourceScaleValue,
            QuestionnaireItem targetItem
    ) {
        return new QuestionnaireBranchRule(sourceItem, sourceOptionId, sourceScaleValue, targetItem);
    }
}
