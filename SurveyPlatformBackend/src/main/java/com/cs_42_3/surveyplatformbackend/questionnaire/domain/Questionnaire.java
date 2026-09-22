package com.cs_42_3.surveyplatformbackend.questionnaire.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Researcher-configured questionnaire owned by one study.
 * <p>
 * Aggregates the ordered, enabled question-bank references (FR-36, FR-37) and their
 * per-answer branch rules (FR-38); saved as one unit (FR-39). Items reference the live
 * question bank while the questionnaire is a draft; publishing a study will freeze an
 * immutable snapshot once FR-14/FR-15 exist, which is out of this aggregate's scope.
 */
@Getter
@Entity
@Table(name = "questionnaires")
public class Questionnaire {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "study_id", nullable = false, updatable = false)
    private UUID studyId;

    @OneToMany(mappedBy = "questionnaire", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("itemOrder ASC")
    private List<QuestionnaireItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    private Instant updatedAt;

    // Managed by JPA; guards concurrent editor saves, not a published content version.
    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion;

    protected Questionnaire() {}

    private Questionnaire(UUID studyId) {
        this.studyId = Objects.requireNonNull(studyId, "Study ID is required");
    }

    /**
     * Creates an empty questionnaire for a study before it is persisted.
     *
     * @param studyId the owning study
     * @return a new questionnaire with no enabled items
     */
    public static Questionnaire create(UUID studyId) {
        return new Questionnaire(studyId);
    }

    /**
     * Replaces every enabled item, its order, and its branch rules as one unit.
     * <p>
     * Plans are supplied in their new display order; each
     * {@link QuestionnaireBranchRulePlan#targetPosition()} refers to the index within
     * {@code plans} of that rule's jump target.
     *
     * @param plans enabled items in order, each carrying its own branch rules
     */
    public void replaceItems(List<QuestionnaireItemPlan> plans) {
        Objects.requireNonNull(plans, "Item plan list is required");
        items.clear();

        List<QuestionnaireItem> created = new ArrayList<>(plans.size());
        for (int index = 0; index < plans.size(); index++) {
            QuestionnaireItem item = QuestionnaireItem.create(this, plans.get(index).questionId(), index);
            items.add(item);
            created.add(item);
        }

        for (int index = 0; index < plans.size(); index++) {
            QuestionnaireItem sourceItem = created.get(index);
            for (QuestionnaireBranchRulePlan rulePlan : plans.get(index).branchRules()) {
                QuestionnaireItem targetItem = created.get(rulePlan.targetPosition());
                sourceItem.addBranchRule(QuestionnaireBranchRule.create(
                        sourceItem,
                        rulePlan.sourceOptionId(),
                        rulePlan.sourceScaleValue(),
                        targetItem
                ));
            }
        }

        touchUpdatedAt();
    }

    /**
     * Returns an immutable snapshot of the ordered items.
     *
     * @return items ordered from zero upward
     */
    public List<QuestionnaireItem> getItems() {
        return List.copyOf(items);
    }

    @PrePersist
    private void initializeTimestamps() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    private void refreshUpdatedAt() {
        updatedAt = Instant.now();
    }

    private void touchUpdatedAt() {
        updatedAt = Instant.now();
    }
}
