-- Published questionnaires are immutable, self-contained documents.
-- Live draft rules deliberately keep option UUIDs without an option-table foreign key:
-- deleting or replacing a question-bank option must not affect an existing publication.
ALTER TABLE questionnaire_branch_rules
    DROP CONSTRAINT fk_branch_rules_source_option;

COMMENT ON COLUMN questionnaire_branch_rules.source_option_id IS
    'SINGLE_CHOICE trigger UUID. Draft integrity is validated by the application; publications use immutable snapshots.';

CREATE TABLE questionnaire_publication_snapshots (
    id UUID NOT NULL,
    study_id UUID NOT NULL,
    source_questionnaire_id UUID NOT NULL,
    questionnaire_version BIGINT NOT NULL,
    published_at TIMESTAMPTZ NOT NULL,
    content JSONB NOT NULL,

    CONSTRAINT pk_questionnaire_publication_snapshots PRIMARY KEY (id),
    CONSTRAINT fk_questionnaire_publication_snapshots_study
        FOREIGN KEY (study_id) REFERENCES studies (id) ON DELETE RESTRICT,
    CONSTRAINT uq_questionnaire_publication_snapshots_study UNIQUE (study_id),
    CONSTRAINT ck_questionnaire_publication_snapshots_version
        CHECK (questionnaire_version >= 0),
    CONSTRAINT ck_questionnaire_publication_snapshots_content
        CHECK (jsonb_typeof(content) = 'object')
);

CREATE INDEX idx_questionnaire_publication_snapshots_published_at
    ON questionnaire_publication_snapshots (published_at DESC, id);

COMMENT ON TABLE questionnaire_publication_snapshots IS
    'One immutable, self-contained questionnaire publication per study.';
COMMENT ON COLUMN questionnaire_publication_snapshots.source_questionnaire_id IS
    'Source-tracing UUID only; intentionally has no foreign key to the mutable draft.';
COMMENT ON COLUMN questionnaire_publication_snapshots.content IS
    'Complete question, option, ordering, default-next, and branch-rule document captured at publication.';
