-- Cleanup tasks deliberately have no foreign key to studies or assets.
-- They must survive deletion of their source records and application restarts.
CREATE TABLE asset_file_cleanup_tasks (
    id UUID PRIMARY KEY,
    storage_key VARCHAR(64) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0 CHECK (attempt_count >= 0),
    next_attempt_at TIMESTAMPTZ NOT NULL,
    last_error VARCHAR(300)
);

CREATE INDEX idx_asset_file_cleanup_tasks_due
    ON asset_file_cleanup_tasks (next_attempt_at, id);
