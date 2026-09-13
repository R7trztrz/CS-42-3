package com.cs_42_3.surveyplatformbackend.survey.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * A question in a researcher's question bank (account-level resource, UC-21).
 * Referenced — not copied — by a survey's enabled-question list (UC-24) until the survey
 * is published, at which point the survey layer is expected to snapshot the question content
 * (see M4 module notes: "lock on publish"). This entity itself has no notion of which
 * survey/study it's used in.
 *
 * Type-specific fields (scaleMin/scaleMax/labels) are nullable and only populated when
 * type == SCALE, following the plain relational-columns approach chosen for this module
 * over a single JSON blob, since the type set is small and fixed (3 types) for v1.
 */
@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Owning researcher's id. Filtering by this field is what makes the question bank
     * account-level and enforces the UC-04 ownership constraint at the query level.
     *
     * NOTE: sourced from the authenticated principal (see QuestionService.currentResearcherId()).
     * The exact JWT claim used to populate this must be confirmed against M1's token structure
     * before merging — see module README.
     */
    @Column(name = "researcher_id", nullable = false)
    private Long researcherId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private QuestionType type;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    // ---- Scale-only fields (null for every other type) ----

    @Column(name = "scale_min")
    private Integer scaleMin;

    @Column(name = "scale_max")
    private Integer scaleMax;

    @Column(name = "scale_min_label")
    private String scaleMinLabel;

    @Column(name = "scale_max_label")
    private String scaleMaxLabel;

    // ---- Options (single/multi-choice only) ----

    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("optionOrder ASC")
    @Builder.Default
    private List<QuestionOption> options = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /** Convenience method used by the service layer when rebuilding the option list on edit. */
    public void replaceOptions(List<QuestionOption> newOptions) {
        this.options.clear();
        if (newOptions != null) {
            newOptions.forEach(opt -> opt.setQuestion(this));
            this.options.addAll(newOptions);
        }
    }
}
