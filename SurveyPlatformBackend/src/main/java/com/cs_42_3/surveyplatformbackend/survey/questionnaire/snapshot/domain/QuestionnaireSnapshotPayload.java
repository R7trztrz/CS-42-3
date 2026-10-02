package com.cs_42_3.surveyplatformbackend.survey.questionnaire.snapshot.domain;

import com.cs_42_3.surveyplatformbackend.survey.domain.QuestionType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Complete questionnaire content captured at publication time. */
public record QuestionnaireSnapshotPayload(
        UUID questionnaireId,
        UUID studyId,
        long version,
        Instant sourceUpdatedAt,
        List<Item> items
) {
    public QuestionnaireSnapshotPayload {
        items = items == null ? List.of() : List.copyOf(items);
    }

    /** One stable published item, independent of its source question-bank row. */
    public record Item(
            UUID itemId,
            UUID sourceQuestionId,
            int position,
            QuestionType type,
            String questionText,
            boolean required,
            List<Option> options,
            Integer scaleMin,
            Integer scaleMax,
            String scaleMinLabel,
            String scaleMaxLabel,
            Instant questionCreatedAt,
            Instant questionUpdatedAt,
            UUID defaultNextItemId,
            List<BranchRule> branchRules
    ) {
        public Item {
            options = options == null ? List.of() : List.copyOf(options);
            branchRules = branchRules == null ? List.of() : List.copyOf(branchRules);
        }
    }

    /** One stable published choice option. */
    public record Option(UUID optionId, String optionText, int optionOrder) {}

    /** One deterministic published conditional transition. */
    public record BranchRule(
            UUID ruleId,
            UUID sourceOptionId,
            Integer sourceScaleValue,
            UUID targetItemId
    ) {}
}
