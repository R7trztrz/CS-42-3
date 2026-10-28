-- Preserve every existing task as a session export before making session_id optional.
ALTER TABLE export_jobs ADD COLUMN scope VARCHAR(16) NOT NULL DEFAULT 'SESSION';
ALTER TABLE export_jobs ALTER COLUMN session_id DROP NOT NULL;
ALTER TABLE export_jobs ADD CONSTRAINT ck_export_scope CHECK (
    (scope = 'SESSION' AND session_id IS NOT NULL) OR
    (scope = 'STUDY' AND session_id IS NULL)
);
ALTER TABLE export_jobs ADD COLUMN failed_session_id UUID;
COMMENT ON COLUMN export_jobs.failed_session_id IS
    'Safe validation-failure context; intentionally not a cascade foreign key.';
