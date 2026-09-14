package com.cs_42_3.surveyplatformbackend.survey.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Reusable survey question owned by one researcher.
 * <p>
 * Maintains type-specific fields and the lifecycle of ordered choice options.
 */
@Getter
@Entity
@Table(name = "questions")
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "researcher_id", nullable = false, updatable = false)
    private UUID researcherId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private QuestionType type;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    @Column(name = "is_required", nullable = false)
    private boolean required;

    @Column(name = "scale_min")
    private Integer scaleMin;

    @Column(name = "scale_max")
    private Integer scaleMax;

    @Column(name = "scale_min_label", length = 255)
    private String scaleMinLabel;

    @Column(name = "scale_max_label", length = 255)
    private String scaleMaxLabel;

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("optionOrder ASC")
    private List<QuestionOption> options = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "timestamptz")
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false, columnDefinition = "timestamptz")
    private Instant updatedAt;

    protected Question() {}

    private Question(
            UUID researcherId,
            QuestionType type,
            String questionText,
            boolean required,
            Integer scaleMin,
            Integer scaleMax,
            String scaleMinLabel,
            String scaleMaxLabel
    ) {
        this.researcherId = Objects.requireNonNull(researcherId, "Researcher ID is required");
        applyFields(type, questionText, required, scaleMin, scaleMax, scaleMinLabel, scaleMaxLabel);
    }

    /**
     * Creates a reusable survey question before it is persisted.
     *
     * @param researcherId owner of the question
     * @param type response format
     * @param questionText text shown to participants
     * @param required whether a response is mandatory
     * @param scaleMin optional lower scale bound
     * @param scaleMax optional upper scale bound
     * @param scaleMinLabel optional lower-bound label
     * @param scaleMaxLabel optional upper-bound label
     * @return a new question owned by the supplied researcher
     */
    public static Question create(
            UUID researcherId,
            QuestionType type,
            String questionText,
            boolean required,
            Integer scaleMin,
            Integer scaleMax,
            String scaleMinLabel,
            String scaleMaxLabel
    ) {
        return new Question(
                researcherId,
                type,
                questionText,
                required,
                scaleMin,
                scaleMax,
                scaleMinLabel,
                scaleMaxLabel
        );
    }

    /**
     * Replaces the editable fields with already validated and normalized values.
     *
     * @param type response format
     * @param questionText text shown to participants
     * @param required whether a response is mandatory
     * @param scaleMin optional lower scale bound
     * @param scaleMax optional upper scale bound
     * @param scaleMinLabel optional lower-bound label
     * @param scaleMaxLabel optional upper-bound label
     */
    public void update(
            QuestionType type,
            String questionText,
            boolean required,
            Integer scaleMin,
            Integer scaleMax,
            String scaleMinLabel,
            String scaleMaxLabel
    ) {
        applyFields(type, questionText, required, scaleMin, scaleMax, scaleMinLabel, scaleMaxLabel);
        touchUpdatedAt();
    }

    /**
     * Replaces all choice options while preserving their supplied order.
     *
     * @param optionTexts normalized option text in display order
     */
    public void replaceOptions(List<String> optionTexts) {
        Objects.requireNonNull(optionTexts, "Option list is required");
        options.clear();
        for (int index = 0; index < optionTexts.size(); index++) {
            options.add(QuestionOption.create(this, optionTexts.get(index), index));
        }
        touchUpdatedAt();
    }

    /**
     * Returns an immutable snapshot of the ordered options.
     *
     * @return options ordered from zero upward
     */
    public List<QuestionOption> getOptions() {
        return List.copyOf(options);
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

    private void applyFields(
            QuestionType type,
            String questionText,
            boolean required,
            Integer scaleMin,
            Integer scaleMax,
            String scaleMinLabel,
            String scaleMaxLabel
    ) {
        this.type = Objects.requireNonNull(type, "Question type is required");
        this.questionText = Objects.requireNonNull(questionText, "Question text is required");
        this.required = required;
        this.scaleMin = scaleMin;
        this.scaleMax = scaleMax;
        this.scaleMinLabel = scaleMinLabel;
        this.scaleMaxLabel = scaleMaxLabel;
    }

    private void touchUpdatedAt() {
        updatedAt = Instant.now();
    }
}
