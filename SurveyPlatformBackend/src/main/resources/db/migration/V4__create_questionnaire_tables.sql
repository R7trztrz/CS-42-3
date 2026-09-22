-- Create the questionnaire structure required by FR36-FR39.
-- A questionnaire is the researcher-configured ordering of question-bank items for one
-- study, plus the option-to-item jump table that drives branching (FR-38). Draft items
-- reference live rows in questions / question_options (V3); FR-14/FR-15 will freeze an
-- immutable snapshot once study publishing is implemented, so this migration only covers
-- the editable draft.
CREATE TABLE questionnaires (
    id UUID NOT NULL,
    study_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lock_version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_questionnaires PRIMARY KEY (id),
    CONSTRAINT uk_questionnaires_study UNIQUE (study_id),
    CONSTRAINT fk_questionnaires_study
        FOREIGN KEY (study_id) REFERENCES studies (id) ON DELETE CASCADE,
    CONSTRAINT ck_questionnaires_lock_version_nonnegative CHECK (lock_version >= 0)
);

CREATE TABLE questionnaire_items (
    id UUID NOT NULL,
    questionnaire_id UUID NOT NULL,
    question_id UUID NOT NULL,
    item_order INTEGER NOT NULL,

    CONSTRAINT pk_questionnaire_items PRIMARY KEY (id),
    CONSTRAINT fk_questionnaire_items_questionnaire
        FOREIGN KEY (questionnaire_id) REFERENCES questionnaires (id) ON DELETE CASCADE,
    -- Restricted, not cascaded: a bank question in active use must be removed from every
    -- questionnaire (FR-37) before it can be edited or deleted (FR-34/FR-35), protecting NFR-14.
    CONSTRAINT fk_questionnaire_items_question
        FOREIGN KEY (question_id) REFERENCES questions (id) ON DELETE RESTRICT,
    -- Hibernate replaces item rows within one flush (see Questionnaire#replaceItems),
    -- so validate final ordering after the old rows have also been removed.
    CONSTRAINT uk_questionnaire_items_questionnaire_order
        UNIQUE (questionnaire_id, item_order) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT ck_questionnaire_items_order_nonnegative CHECK (item_order >= 0)
);

CREATE INDEX idx_questionnaire_items_question
    ON questionnaire_items (question_id);

CREATE TABLE questionnaire_branch_rules (
    id UUID NOT NULL,
    source_item_id UUID NOT NULL,
    -- Exactly one of the two trigger columns is populated per row, matching the
    -- source item's question type (SINGLE_CHOICE -> option, SCALE -> value).
    source_option_id UUID,
    source_scale_value INTEGER,
    target_item_id UUID NOT NULL,

    CONSTRAINT pk_questionnaire_branch_rules PRIMARY KEY (id),
    CONSTRAINT fk_branch_rules_source_item
        FOREIGN KEY (source_item_id) REFERENCES questionnaire_items (id) ON DELETE CASCADE,
    -- Restricted: removing an item that another rule jumps to must be an explicit,
    -- validated edit (FR-38), never a silent side effect of an unrelated change.
    CONSTRAINT fk_branch_rules_target_item
        FOREIGN KEY (target_item_id) REFERENCES questionnaire_items (id) ON DELETE RESTRICT,
    CONSTRAINT fk_branch_rules_source_option
        FOREIGN KEY (source_option_id) REFERENCES question_options (id) ON DELETE CASCADE,
    CONSTRAINT ck_branch_rules_single_trigger
        CHECK ((source_option_id IS NULL) <> (source_scale_value IS NULL)),
    CONSTRAINT ck_branch_rules_no_self_loop
        CHECK (source_item_id <> target_item_id),
    -- One rule per selected option / scale value keeps the "answer -> next item" lookup
    -- a function, which is what makes runtime jump resolution deterministic (NFR-15).
    CONSTRAINT uk_branch_rules_source_option
        UNIQUE (source_item_id, source_option_id),
    CONSTRAINT uk_branch_rules_source_scale_value
        UNIQUE (source_item_id, source_scale_value)
);

CREATE INDEX idx_branch_rules_target_item
    ON questionnaire_branch_rules (target_item_id);

COMMENT ON TABLE questionnaires IS 'One researcher-configured questionnaire draft per study.';
COMMENT ON TABLE questionnaire_items IS 'Ordered, enabled question-bank references for one questionnaire (FR-36/FR-37).';
COMMENT ON COLUMN questionnaire_items.item_order IS 'Zero-based display order and default jump target when no branch rule matches.';
COMMENT ON TABLE questionnaire_branch_rules IS 'Per-answer jump targets configured by the researcher (FR-38).';
COMMENT ON COLUMN questionnaire_branch_rules.source_option_id IS 'Selected option that triggers this rule; set only for SINGLE_CHOICE source items.';
COMMENT ON COLUMN questionnaire_branch_rules.source_scale_value IS 'Selected scale value that triggers this rule; set only for SCALE source items.';
