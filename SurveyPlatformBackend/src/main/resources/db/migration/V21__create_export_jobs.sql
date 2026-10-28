-- Export files are private runtime artifacts; cleanup survives aggregate deletion.
CREATE TABLE export_jobs (
    id UUID PRIMARY KEY,
    study_id UUID NOT NULL REFERENCES studies(id) ON DELETE CASCADE,
    session_id UUID NOT NULL REFERENCES participant_sessions(id) ON DELETE CASCADE,
    data_types VARCHAR(100) NOT NULL,
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING','RUNNING','COMPLETED','FAILED','EXPIRED')),
    created_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    snapshot_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    expires_at TIMESTAMPTZ,
    error_code VARCHAR(64),
    error_message VARCHAR(512),
    CONSTRAINT ck_export_types CHECK (data_types = 'QUESTIONNAIRE'),
    CONSTRAINT ck_export_completion CHECK (status <> 'COMPLETED' OR
        (snapshot_at IS NOT NULL AND completed_at IS NOT NULL AND expires_at > completed_at))
);
CREATE INDEX idx_export_jobs_queue ON export_jobs(status, created_at, id);
CREATE INDEX idx_export_jobs_study ON export_jobs(study_id, id);
CREATE INDEX idx_export_jobs_session ON export_jobs(session_id, id);
CREATE INDEX idx_export_jobs_expiry ON export_jobs(expires_at) WHERE status = 'COMPLETED';

CREATE TABLE export_file_cleanup_tasks (
    export_id UUID PRIMARY KEY,
    created_at TIMESTAMPTZ NOT NULL,
    next_attempt_at TIMESTAMPTZ NOT NULL,
    attempts INTEGER NOT NULL DEFAULT 0 CHECK (attempts >= 0)
);
CREATE INDEX idx_export_cleanup_due ON export_file_cleanup_tasks(next_attempt_at, export_id);
COMMENT ON TABLE export_file_cleanup_tasks IS
    'Independent durable cleanup of both ZIP and temporary files, including deleted studies.';
