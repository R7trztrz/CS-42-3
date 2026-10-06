-- Add deterministic SINGLE_CHOICE and SCALE branching for FR38 after the V12 questionnaire schema.
CREATE TABLE questionnaire_branch_rules (
    id UUID NOT NULL,
    source_item_id UUID NOT NULL,
    source_option_id UUID,
    source_scale_value INTEGER,
    target_item_id UUID NOT NULL,

    CONSTRAINT pk_questionnaire_branch_rules PRIMARY KEY (id),
    CONSTRAINT fk_branch_rules_source_item
        FOREIGN KEY (source_item_id) REFERENCES questionnaire_items (id) ON DELETE CASCADE,
    CONSTRAINT fk_branch_rules_target_item
        FOREIGN KEY (target_item_id) REFERENCES questionnaire_items (id) ON DELETE RESTRICT,
    CONSTRAINT fk_branch_rules_source_option
        FOREIGN KEY (source_option_id) REFERENCES question_options (id) ON DELETE RESTRICT,
    CONSTRAINT ck_branch_rules_single_trigger
        CHECK ((source_option_id IS NULL) <> (source_scale_value IS NULL)),
    CONSTRAINT ck_branch_rules_no_self_loop
        CHECK (source_item_id <> target_item_id),
    CONSTRAINT uk_branch_rules_source_option
        UNIQUE (source_item_id, source_option_id),
    CONSTRAINT uk_branch_rules_source_scale_value
        UNIQUE (source_item_id, source_scale_value)
);

CREATE INDEX idx_branch_rules_target_item
    ON questionnaire_branch_rules (target_item_id);

CREATE INDEX idx_branch_rules_source_option
    ON questionnaire_branch_rules (source_option_id);

COMMENT ON TABLE questionnaire_branch_rules IS
    'Deterministic answer-triggered transitions between questionnaire items.';
COMMENT ON COLUMN questionnaire_branch_rules.source_option_id IS
    'SINGLE_CHOICE trigger. RESTRICT prevents option edits from silently deleting rules.';
