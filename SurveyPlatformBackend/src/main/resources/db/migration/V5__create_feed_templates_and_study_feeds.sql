-- Template codes are stable identifiers shared with the frontend.
-- SQL NULL means content has not been supplied, including for the blank template.
CREATE TABLE feed_templates (
    code VARCHAR(32) PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    theme VARCHAR(32),
    content JSONB,
    schema_version INTEGER,
    CONSTRAINT ck_feed_template_schema_version CHECK (schema_version IS NULL OR schema_version > 0)
);

INSERT INTO feed_templates (code, name, theme) VALUES
    ('blank', 'Blank canvas', NULL),
    ('facebook', 'Facebook', 'facebook'),
    ('instagram', 'Instagram', 'instagram'),
    ('tiktok', 'TikTok', 'tiktok'),
    ('x', 'X', 'x'),
    ('threads', 'Threads', 'threads'),
    ('bluesky', 'Bluesky', 'bluesky'),
    ('truth-social', 'Truth Social', 'truth-social');

CREATE TABLE study_feeds (
    study_id UUID PRIMARY KEY REFERENCES studies(id) ON DELETE CASCADE,
    template_code VARCHAR(32) NOT NULL REFERENCES feed_templates(code) ON DELETE RESTRICT,
    theme VARCHAR(32),
    content JSONB,
    schema_version INTEGER,
    lock_version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_study_feed_schema_version CHECK (schema_version IS NULL OR schema_version > 0),
    CONSTRAINT ck_study_feed_lock_version CHECK (lock_version >= 0)
);

COMMENT ON TABLE feed_templates IS 'System-managed template catalog; no researcher write API.';
COMMENT ON COLUMN feed_templates.content IS 'Initial JSON document; NULL until frontend templates are supplied.';
COMMENT ON TABLE study_feeds IS 'Independent per-study feed initialized atomically during study creation.';
COMMENT ON COLUMN study_feeds.content IS 'Independent copy of template content; NULL is unconfigured, not publishable.';
-- Existing studies intentionally remain without a feed: their template choice is unknown.
