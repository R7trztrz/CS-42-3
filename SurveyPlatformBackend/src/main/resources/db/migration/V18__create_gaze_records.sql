-- M6 / FR-50: browsing-phase gaze events, delivered in batches.
--
-- (session_id, sequence) is the idempotency key the browser buffer assigns;
-- the sequence is gap-free per page lifetime, so a gap in stored sequences is
-- a batch that never arrived. Overflow loss is carried on every batch.
CREATE TABLE gaze_batches (
    session_id UUID NOT NULL,
    sequence BIGINT NOT NULL,
    batch_id VARCHAR(128) NOT NULL,
    schema_version SMALLINT NOT NULL,
    event_count INTEGER NOT NULL,
    dropped_events BIGINT NOT NULL,
    dropped_events_total BIGINT NOT NULL,
    request_hash CHAR(64) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_gaze_batches PRIMARY KEY (session_id, sequence),
    CONSTRAINT fk_gaze_batches_session
        FOREIGN KEY (session_id) REFERENCES participant_sessions (id) ON DELETE CASCADE,
    CONSTRAINT ck_gaze_batches_schema_version
        CHECK (schema_version = 1),
    CONSTRAINT ck_gaze_batches_sequence
        CHECK (sequence >= 1),
    CONSTRAINT ck_gaze_batches_event_count
        CHECK (event_count >= 1),
    CONSTRAINT ck_gaze_batches_dropped
        CHECK (dropped_events >= 0 AND dropped_events_total >= dropped_events),
    CONSTRAINT ck_gaze_batches_request_hash
        CHECK (request_hash ~ '^[0-9a-f]{64}$')
);

-- One row per prediction. Raw viewport coordinates are always kept; widget
-- attribution is an annotation, never a filter.
CREATE TABLE gaze_events (
    session_id UUID NOT NULL,
    batch_sequence BIGINT NOT NULL,
    event_index INTEGER NOT NULL,
    page_id VARCHAR(128) NOT NULL,
    x DOUBLE PRECISION NOT NULL,
    y DOUBLE PRECISION NOT NULL,
    scroll_x DOUBLE PRECISION NOT NULL,
    scroll_y DOUBLE PRECISION NOT NULL,
    page_x DOUBLE PRECISION NOT NULL,
    page_y DOUBLE PRECISION NOT NULL,
    viewport_width DOUBLE PRECISION NOT NULL,
    viewport_height DOUBLE PRECISION NOT NULL,
    device_pixel_ratio DOUBLE PRECISION NOT NULL,
    widget_id VARCHAR(255),
    attribution VARCHAR(16) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    session_elapsed_ms DOUBLE PRECISION NOT NULL,
    calibration_attempt_id UUID NOT NULL,

    CONSTRAINT pk_gaze_events PRIMARY KEY (session_id, batch_sequence, event_index),
    CONSTRAINT fk_gaze_events_batch
        FOREIGN KEY (session_id, batch_sequence)
            REFERENCES gaze_batches (session_id, sequence) ON DELETE CASCADE,
    CONSTRAINT fk_gaze_events_calibration
        FOREIGN KEY (session_id, calibration_attempt_id)
            REFERENCES calibration_attempts (session_id, attempt_id) ON DELETE CASCADE,
    CONSTRAINT ck_gaze_events_index
        CHECK (event_index >= 0),
    CONSTRAINT ck_gaze_events_attribution
        CHECK (attribution IN ('WIDGET', 'NO_CONTENT', 'UNRESOLVED')),
    CONSTRAINT ck_gaze_events_widget
        CHECK ((attribution = 'WIDGET') = (widget_id IS NOT NULL)),
    CONSTRAINT ck_gaze_events_viewport
        CHECK (viewport_width >= 0 AND viewport_height >= 0 AND device_pixel_ratio > 0)
);

CREATE INDEX idx_gaze_events_session_elapsed
    ON gaze_events (session_id, session_elapsed_ms);

CREATE INDEX idx_gaze_events_session_widget
    ON gaze_events (session_id, widget_id)
    WHERE widget_id IS NOT NULL;

COMMENT ON TABLE gaze_batches IS
    'FR-50 gaze batches keyed by the browser-assigned (session_id, sequence).';
COMMENT ON COLUMN gaze_batches.dropped_events_total IS
    'Events the browser discarded on buffer overflow since the page started collecting.';
COMMENT ON TABLE gaze_events IS
    'FR-50 gaze predictions in viewport CSS pixels with scroll offsets and widget attribution.';
COMMENT ON COLUMN gaze_events.widget_id IS
    'The data-component-id (Craft node id) under the gaze point, or NULL when none was found.';
