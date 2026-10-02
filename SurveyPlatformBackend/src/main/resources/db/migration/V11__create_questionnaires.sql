-- Create researcher-owned questionnaire drafts for FR36, FR37, and the linear FR39 scope after V8.
CREATE TABLE questionnaires (
    id UUID NOT NULL,
    study_id UUID NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lock_version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_questionnaires PRIMARY KEY (id),
    CONSTRAINT fk_questionnaires_study
        FOREIGN KEY (study_id) REFERENCES studies (id) ON DELETE CASCADE,
    CONSTRAINT uq_questionnaires_study UNIQUE (study_id),
    CONSTRAINT ck_questionnaires_lock_version_nonnegative CHECK (lock_version >= 0)
);

CREATE TABLE questionnaire_items (
    id UUID NOT NULL,
    questionnaire_id UUID NOT NULL,
    question_id UUID,
    position INTEGER NOT NULL,

    CONSTRAINT pk_questionnaire_items PRIMARY KEY (id),
    CONSTRAINT fk_questionnaire_items_questionnaire
        FOREIGN KEY (questionnaire_id) REFERENCES questionnaires (id) ON DELETE CASCADE,
    CONSTRAINT fk_questionnaire_items_question
        FOREIGN KEY (question_id) REFERENCES questions (id) ON DELETE SET NULL,
    CONSTRAINT uq_questionnaire_items_position
        UNIQUE (questionnaire_id, position) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT uq_questionnaire_items_question
        UNIQUE (questionnaire_id, question_id) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT ck_questionnaire_items_position_nonnegative CHECK (position >= 0)
);

CREATE INDEX idx_questionnaire_items_question
    ON questionnaire_items (question_id);

COMMENT ON TABLE questionnaires IS
    'Research-level questionnaire drafts. Each study has at most one questionnaire.';
COMMENT ON COLUMN questionnaires.lock_version IS
    'Optimistic locking version managed by JPA.';
COMMENT ON TABLE questionnaire_items IS
    'Ordered question references belonging to questionnaire drafts.';
COMMENT ON COLUMN questionnaire_items.question_id IS
    'Nullable because deleting a question bank entry leaves a detectable missing reference.';
