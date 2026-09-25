package com.cs_42_3.surveyplatformbackend.survey.questionnaire.domain;

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
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Mutable questionnaire draft aggregate belonging to exactly one study.
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
    @OrderBy("position ASC")
    private List<QuestionnaireItem> items = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    private Instant updatedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion;

    protected Questionnaire() {}

    private Questionnaire(UUID studyId) {
        this.studyId = Objects.requireNonNull(studyId, "Study ID is required");
    }

    public static Questionnaire create(UUID studyId) {
        return new Questionnaire(studyId);
    }

    public List<QuestionnaireItem> getItems() {
        return List.copyOf(items);
    }

    public QuestionnaireItem addItem(UUID questionId, int position) {
        QuestionnaireItem item = QuestionnaireItem.create(this, questionId, position);
        items.add(item);
        items.sort(Comparator.comparingInt(QuestionnaireItem::getPosition));
        return item;
    }

    public boolean removeItem(UUID itemId) {
        Iterator<QuestionnaireItem> iterator = items.iterator();
        while (iterator.hasNext()) {
            QuestionnaireItem item = iterator.next();
            if (Objects.equals(item.getId(), itemId)) {
                iterator.remove();
                item.detach();
                return true;
            }
        }
        return false;
    }

    public boolean replaceItemQuestion(UUID itemId, UUID questionId) {
        return findItem(itemId).replaceQuestion(questionId);
    }

    public boolean reorderItems(Map<UUID, Integer> positionsByItemId) {
        boolean changed = false;
        for (QuestionnaireItem item : items) {
            Integer position = positionsByItemId.get(item.getId());
            if (position != null) {
                changed |= item.moveTo(position);
            }
        }
        items.sort(Comparator.comparingInt(QuestionnaireItem::getPosition));
        return changed;
    }

    /**
     * Differentially synchronizes the aggregate while retaining every reusable item UUID.
     * A null item ID represents a genuinely new item.
     */
    public boolean synchronizeItems(List<ItemPlacement> requestedItems) {
        Objects.requireNonNull(requestedItems, "Requested items are required");

        Map<UUID, QuestionnaireItem> existingById = new HashMap<>();
        for (QuestionnaireItem item : items) {
            if (item.getId() != null) {
                existingById.put(item.getId(), item);
            }
        }

        Set<QuestionnaireItem> retained = new HashSet<>();
        boolean changed = false;
        for (int position = 0; position < requestedItems.size(); position++) {
            ItemPlacement placement = requestedItems.get(position);
            QuestionnaireItem item = placement.itemId() == null
                    ? null
                    : existingById.get(placement.itemId());
            if (placement.itemId() != null && item == null) {
                throw new IllegalArgumentException(
                        "Questionnaire item does not belong to this aggregate: " + placement.itemId()
                );
            }
            if (item == null) {
                item = QuestionnaireItem.create(this, placement.questionId(), position);
                items.add(item);
                changed = true;
            } else {
                item.attachTo(this);
                changed |= item.replaceQuestion(placement.questionId());
                changed |= item.moveTo(position);
            }
            retained.add(item);
        }

        Iterator<QuestionnaireItem> iterator = items.iterator();
        while (iterator.hasNext()) {
            QuestionnaireItem item = iterator.next();
            if (!retained.contains(item)) {
                iterator.remove();
                item.detach();
                changed = true;
            }
        }
        items.sort(Comparator.comparingInt(QuestionnaireItem::getPosition));
        return changed;
    }

    public boolean clearBranchRules() {
        boolean changed = false;
        for (QuestionnaireItem item : items) {
            changed |= item.clearBranchRules();
        }
        return changed;
    }

    /** Removes outgoing rules from every item that references the supplied question. */
    public boolean clearOutgoingBranchRules(UUID questionId) {
        Objects.requireNonNull(questionId, "Question ID is required");
        boolean changed = false;
        for (QuestionnaireItem item : items) {
            if (Objects.equals(item.getQuestionId(), questionId)) {
                changed |= item.clearBranchRules();
            }
        }
        return changed;
    }

    /** Replaces all rules after items have been synchronized into their final order. */
    public boolean replaceBranchRules(List<List<BranchRulePlacement>> rulesBySourcePosition) {
        Objects.requireNonNull(rulesBySourcePosition, "Branch-rule plans are required");
        if (rulesBySourcePosition.size() != items.size()) {
            throw new IllegalArgumentException("Branch-rule plans must match questionnaire items");
        }
        boolean changed = false;
        for (int sourcePosition = 0; sourcePosition < items.size(); sourcePosition++) {
            List<QuestionnaireItem.BranchRulePlacement> placements = rulesBySourcePosition
                    .get(sourcePosition).stream()
                    .map(plan -> new QuestionnaireItem.BranchRulePlacement(
                            plan.sourceOptionId(),
                            plan.sourceScaleValue(),
                            items.get(plan.targetPosition())
                    ))
                    .toList();
            changed |= items.get(sourcePosition).replaceBranchRules(placements);
        }
        return changed;
    }

    public void markModified() {
        updatedAt = Instant.now();
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

    private QuestionnaireItem findItem(UUID itemId) {
        return items.stream()
                .filter(item -> Objects.equals(item.getId(), itemId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Questionnaire item not found: " + itemId));
    }

    public record ItemPlacement(UUID itemId, UUID questionId) {
        public ItemPlacement {
            Objects.requireNonNull(questionId, "Question ID is required");
        }
    }


    public record BranchRulePlacement(
            UUID sourceOptionId,
            Integer sourceScaleValue,
            int targetPosition
    ) {}
}
