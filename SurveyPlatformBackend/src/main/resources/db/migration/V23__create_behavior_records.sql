-- FR-47/48: session-scoped, retry-safe uploads of raw behavior records.
CREATE TABLE behavior_batches (
    session_id UUID NOT NULL REFERENCES participant_sessions(id) ON DELETE CASCADE,
    batch_id UUID NOT NULL,
    stream_id UUID NOT NULL,
    sequence BIGINT NOT NULL CHECK (sequence BETWEEN 1 AND 9007199254740991),
    schema_version SMALLINT NOT NULL CHECK (schema_version = 1),
    event_count INTEGER NOT NULL CHECK (event_count BETWEEN 1 AND 10000),
    request_hash CHAR(64) NOT NULL CHECK (request_hash ~ '^[0-9a-f]{64}$'),
    received_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT pk_behavior_batches PRIMARY KEY (session_id, batch_id),
    CONSTRAINT uq_behavior_stream_sequence UNIQUE (session_id, stream_id, sequence)
);

CREATE TABLE behavior_events (
    session_id UUID NOT NULL,
    batch_id UUID NOT NULL,
    event_index INTEGER NOT NULL CHECK (event_index >= 0),
    event_type VARCHAR(24) NOT NULL CHECK (event_type IN ('click','keydown','mousemove','scroll','visibilitychange')),
    occurred_at TIMESTAMPTZ NOT NULL,
    session_elapsed_ms DOUBLE PRECISION NOT NULL CHECK
        (session_elapsed_ms NOT IN ('NaN'::double precision, 'Infinity'::double precision, '-Infinity'::double precision)),
    page_id VARCHAR(128) NOT NULL CHECK (btrim(page_id) <> ''),
    component_id VARCHAR(255),
    component_type VARCHAR(128),
    payload JSONB NOT NULL CHECK (jsonb_typeof(payload) = 'object'),
    CONSTRAINT pk_behavior_events PRIMARY KEY (session_id, batch_id, event_index),
    CONSTRAINT fk_behavior_events_batch FOREIGN KEY (session_id, batch_id)
        REFERENCES behavior_batches(session_id, batch_id) ON DELETE CASCADE
);
CREATE INDEX idx_behavior_events_session_elapsed
    ON behavior_events(session_id, session_elapsed_ms, batch_id, event_index);

COMMENT ON TABLE behavior_batches IS
    'Immutable batches identified by session and batch UUID; each page collection instance has its own sequence.';
COMMENT ON COLUMN behavior_events.payload IS
    'Allow-listed event-specific fields only; input values, key characters and arbitrary DOM content are not accepted.';
COMMENT ON COLUMN behavior_events.session_elapsed_ms IS
    'Client-reported shared session clock value; negative values and equal timestamps are permitted.';
