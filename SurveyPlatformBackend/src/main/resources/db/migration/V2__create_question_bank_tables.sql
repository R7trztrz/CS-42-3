-- ============================================================================
-- IMPORTANT: rename this file before merging.
-- "V9999" is a placeholder. Check src/main/resources/db/migration/ on main for
-- the highest existing version number (e.g. if the latest is V5__..., this
-- becomes V6__create_question_bank_tables.sql). Flyway will fail on checksum/
-- ordering conflicts if two people merge migrations with the same version number,
-- so confirm the current head of db/migration on main right before merging.
-- ============================================================================

CREATE TABLE questions (
    id              BIGSERIAL PRIMARY KEY,
    researcher_id   BIGINT       NOT NULL,
    type            VARCHAR(20)  NOT NULL,
    question_text   TEXT         NOT NULL,
    scale_min       INTEGER,
    scale_max       INTEGER,
    scale_min_label VARCHAR(255),
    scale_max_label VARCHAR(255),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Every UC-21 list/search query filters by researcher_id first — this index is
-- what keeps that query fast as the bank grows.
CREATE INDEX idx_questions_researcher_id ON questions (researcher_id);

CREATE TABLE question_options (
    id            BIGSERIAL PRIMARY KEY,
    question_id   BIGINT       NOT NULL REFERENCES questions (id) ON DELETE CASCADE,
    option_text   TEXT         NOT NULL,
    option_order  INTEGER      NOT NULL
);

CREATE INDEX idx_question_options_question_id ON question_options (question_id);
