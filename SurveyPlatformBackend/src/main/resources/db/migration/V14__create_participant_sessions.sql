-- Freeze the consent document version together with every published study.
ALTER TABLE studies
    ADD COLUMN consent_document_version VARCHAR(64);

UPDATE studies
SET consent_document_version = 'platform-default-v1'
WHERE status IN ('COLLECTING', 'CLOSED')
  AND participation_token IS NOT NULL
  AND published_at IS NOT NULL;

ALTER TABLE studies
    ADD CONSTRAINT ck_studies_publication_metadata
        CHECK (
            (status = 'DRAFT'
                AND participation_token IS NULL
                AND published_at IS NULL
                AND consent_document_version IS NULL)
            OR
            (status IN ('COLLECTING', 'CLOSED')
                AND participation_token IS NOT NULL
                AND published_at IS NOT NULL
                AND consent_document_version IS NOT NULL)
            OR
            -- V7 deliberately retained pre-publication lifecycle rows. Keep those
            -- schema-valid legacy rows inaccessible rather than inventing tokens.
            (status IN ('COLLECTING', 'CLOSED')
                AND participation_token IS NULL
                AND published_at IS NULL
                AND consent_document_version IS NULL)
        );

CREATE TABLE participant_sessions (
    id UUID NOT NULL,
    study_id UUID NOT NULL,
    anonymous_participant_id UUID NOT NULL,
    session_token_hash CHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL,
    phase VARCHAR(24) NOT NULL,
    abandonment_reason VARCHAR(32),
    current_question_item_id UUID,
    questionnaire_ready_at TIMESTAMPTZ,
    consented_at TIMESTAMPTZ,
    calibration_completed_at TIMESTAMPTZ,
    browsing_completed_at TIMESTAMPTZ,
    entered_at TIMESTAMPTZ NOT NULL,
    last_activity_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    abandoned_at TIMESTAMPTZ,
    device_info JSONB,
    lock_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_participant_sessions PRIMARY KEY (id),
    CONSTRAINT fk_participant_sessions_study
        FOREIGN KEY (study_id) REFERENCES studies (id) ON DELETE CASCADE,
    CONSTRAINT uq_participant_sessions_token_hash UNIQUE (session_token_hash),
    CONSTRAINT uq_participant_sessions_study_participant
        UNIQUE (study_id, anonymous_participant_id),
    CONSTRAINT ck_participant_sessions_token_hash
        CHECK (session_token_hash ~ '^[0-9a-f]{64}$'),
    CONSTRAINT ck_participant_sessions_status
        CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'ABANDONED')),
    CONSTRAINT ck_participant_sessions_phase
        CHECK (phase IN ('CONSENT', 'CALIBRATION', 'BROWSING', 'QUESTIONNAIRE', 'FINISHED')),
    CONSTRAINT ck_participant_sessions_abandonment_reason
        CHECK (abandonment_reason IS NULL OR abandonment_reason IN (
            'CONSENT_DECLINED', 'PARTICIPANT_EXIT', 'INACTIVITY_TIMEOUT', 'STUDY_CLOSED'
        )),
    CONSTRAINT ck_participant_sessions_device_info
        CHECK (device_info IS NULL OR jsonb_typeof(device_info) = 'object'),
    CONSTRAINT ck_participant_sessions_lock_version
        CHECK (lock_version >= 0),
    CONSTRAINT ck_participant_sessions_lifecycle
        CHECK (
            (status = 'IN_PROGRESS'
                AND phase <> 'FINISHED'
                AND completed_at IS NULL
                AND abandoned_at IS NULL
                AND abandonment_reason IS NULL)
            OR
            (status = 'COMPLETED'
                AND phase = 'FINISHED'
                AND completed_at IS NOT NULL
                AND abandoned_at IS NULL
                AND abandonment_reason IS NULL)
            OR
            (status = 'ABANDONED'
                AND phase = 'FINISHED'
                AND completed_at IS NULL
                AND abandoned_at IS NOT NULL
                AND abandonment_reason IS NOT NULL)
        ),
    CONSTRAINT ck_participant_sessions_consent_declined
        CHECK (
            abandonment_reason <> 'CONSENT_DECLINED'
            OR (device_info IS NULL AND consented_at IS NULL)
        ),
    CONSTRAINT ck_participant_sessions_current_question
        CHECK (
            current_question_item_id IS NULL
            OR (status = 'IN_PROGRESS' AND phase = 'QUESTIONNAIRE')
        ),
    CONSTRAINT ck_participant_sessions_questionnaire_ready
        CHECK (
            questionnaire_ready_at IS NULL
            OR (status = 'IN_PROGRESS' AND phase = 'QUESTIONNAIRE'
                AND current_question_item_id IS NULL)
        )
);

CREATE INDEX idx_participant_sessions_study_status_entered
    ON participant_sessions (study_id, status, entered_at DESC, id);

CREATE INDEX idx_participant_sessions_status_activity
    ON participant_sessions (status, last_activity_at, id);

COMMENT ON TABLE participant_sessions IS
    'Anonymous M5 participation sessions; raw session tokens are never persisted.';
COMMENT ON COLUMN participant_sessions.session_token_hash IS
    'Lowercase SHA-256 hex digest of a 32-byte opaque session token.';
COMMENT ON COLUMN participant_sessions.device_info IS
    'Size-limited, allow-listed, non-identifying device information supplied at entry.';
