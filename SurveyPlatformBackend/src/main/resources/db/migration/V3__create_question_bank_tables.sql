-- Create the reusable question bank required by FR32-FR35.
-- UUID identifiers are assigned by the application, matching V1 and V2.
CREATE TABLE questions (
    id UUID NOT NULL,
    researcher_id UUID NOT NULL,
    type VARCHAR(20) NOT NULL,
    question_text TEXT NOT NULL,
    is_required BOOLEAN NOT NULL DEFAULT FALSE,
    scale_min INTEGER,
    scale_max INTEGER,
    scale_min_label VARCHAR(255),
    scale_max_label VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_questions PRIMARY KEY (id),
    CONSTRAINT fk_questions_researcher
        FOREIGN KEY (researcher_id) REFERENCES researchers (id),
    CONSTRAINT ck_questions_type
        CHECK (type IN ('SINGLE_CHOICE', 'MULTI_CHOICE', 'SCALE', 'TEXT')),
    CONSTRAINT ck_questions_text_not_blank
        CHECK (question_text ~ '[^[:space:]]'),
    CONSTRAINT ck_questions_type_specific_fields CHECK (
        (
            type = 'SCALE'
            AND scale_min IS NOT NULL
            AND scale_max IS NOT NULL
            AND scale_min < scale_max
        )
        OR
        (
            type <> 'SCALE'
            AND scale_min IS NULL
            AND scale_max IS NULL
            AND scale_min_label IS NULL
            AND scale_max_label IS NULL
        )
    )
);

-- Support owner-scoped lists ordered by the most recently updated question.
CREATE INDEX idx_questions_owner_updated
    ON questions (researcher_id, updated_at DESC, id DESC);

CREATE TABLE question_options (
    id UUID NOT NULL,
    question_id UUID NOT NULL,
    option_text TEXT NOT NULL,
    option_order INTEGER NOT NULL,

    CONSTRAINT pk_question_options PRIMARY KEY (id),
    CONSTRAINT fk_question_options_question
        FOREIGN KEY (question_id) REFERENCES questions (id) ON DELETE CASCADE,
    -- Hibernate replaces option rows within one flush, so validate final ordering
    -- after the old rows have also been removed.
    CONSTRAINT uk_question_options_question_order
        UNIQUE (question_id, option_order) DEFERRABLE INITIALLY DEFERRED,
    CONSTRAINT ck_question_options_text_not_blank
        CHECK (option_text ~ '[^[:space:]]'),
    CONSTRAINT ck_question_options_order_nonnegative
        CHECK (option_order >= 0)
);

COMMENT ON TABLE questions IS 'Reusable survey questions owned by researchers.';
COMMENT ON COLUMN questions.is_required IS 'Whether a response is required when the question is used in a survey.';
COMMENT ON TABLE question_options IS 'Ordered options for reusable choice questions.';
