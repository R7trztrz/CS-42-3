CREATE TABLE participant_questionnaire_steps (
    id UUID NOT NULL,
    session_id UUID NOT NULL,
    item_id UUID NOT NULL,
    sequence_number INTEGER NOT NULL,
    idempotency_key UUID NOT NULL,
    request_hash CHAR(64) NOT NULL,
    next_item_id UUID,
    submitted_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_participant_questionnaire_steps PRIMARY KEY (id),
    CONSTRAINT fk_participant_questionnaire_steps_session
        FOREIGN KEY (session_id) REFERENCES participant_sessions (id) ON DELETE CASCADE,
    CONSTRAINT uq_participant_questionnaire_steps_session_item
        UNIQUE (session_id, item_id),
    CONSTRAINT uq_participant_questionnaire_steps_session_sequence
        UNIQUE (session_id, sequence_number),
    CONSTRAINT uq_participant_questionnaire_steps_session_idempotency
        UNIQUE (session_id, idempotency_key),
    CONSTRAINT ck_participant_questionnaire_steps_sequence
        CHECK (sequence_number > 0),
    CONSTRAINT ck_participant_questionnaire_steps_request_hash
        CHECK (request_hash ~ '^[0-9a-f]{64}$')
);

CREATE INDEX idx_participant_questionnaire_steps_session_submitted
    ON participant_questionnaire_steps (session_id, submitted_at, id);

CREATE TABLE participant_answers (
    id UUID NOT NULL,
    step_id UUID NOT NULL,
    question_type VARCHAR(24) NOT NULL,
    answer_payload JSONB NOT NULL,
    answered_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_participant_answers PRIMARY KEY (id),
    CONSTRAINT fk_participant_answers_step
        FOREIGN KEY (step_id) REFERENCES participant_questionnaire_steps (id) ON DELETE CASCADE,
    CONSTRAINT uq_participant_answers_step UNIQUE (step_id),
    CONSTRAINT ck_participant_answers_question_type
        CHECK (question_type IN ('SINGLE_CHOICE', 'MULTI_CHOICE', 'SCALE', 'TEXT')),
    CONSTRAINT ck_participant_answers_payload
        CHECK (jsonb_typeof(answer_payload) = 'object')
);

COMMENT ON TABLE participant_questionnaire_steps IS
    'The immutable path actually traversed through a published questionnaire.';
COMMENT ON COLUMN participant_questionnaire_steps.next_item_id IS
    'The server-selected next item from the first successful request; NULL means END.';
COMMENT ON COLUMN participant_questionnaire_steps.request_hash IS
    'Lowercase SHA-256 digest of item identity plus the canonical answer request.';
COMMENT ON TABLE participant_answers IS
    'Answers that actually exist; optional unanswered steps deliberately have no row.';
