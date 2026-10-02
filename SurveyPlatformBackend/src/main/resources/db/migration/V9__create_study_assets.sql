-- Image bytes live in persistent storage; metadata and ownership live in PostgreSQL.
CREATE TABLE study_assets (
    id UUID PRIMARY KEY,
    study_id UUID NOT NULL REFERENCES studies(id) ON DELETE RESTRICT,
    storage_key VARCHAR(64) NOT NULL UNIQUE,
    original_filename VARCHAR(255) NOT NULL,
    content_type VARCHAR(32) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0 AND size_bytes <= 5242880),
    width INTEGER NOT NULL CHECK (width > 0),
    height INTEGER NOT NULL CHECK (height > 0),
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_asset_content_type CHECK (content_type IN ('image/jpeg', 'image/png', 'image/webp'))
);
CREATE INDEX idx_study_assets_study_id ON study_assets(study_id);
COMMENT ON TABLE study_assets IS 'Immutable study-owned image metadata; feed widgets reference asset UUIDs.';
-- Study deletion must explicitly coordinate asset rows and stored files; cascading rows alone would leak files.
