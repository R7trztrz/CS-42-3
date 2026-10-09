-- M6 / FR-50 (UC-36): browsing-phase gaze events, uploaded in batches.
-- (session_id, sequence) is the idempotency key the browser assigns. Sequences start at 1 with
-- no gaps, so a gap in the stored sequences is a batch that never arrived.
-- Gaze events have a single, fixed shape, so they use typed columns rather than a JSONB payload.
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
    CONSTRAINT ck_gaze_batches_schema_version CHECK (schema_version = 1),
    CONSTRAINT ck_gaze_batches_sequence CHECK (sequence >= 1),
    CONSTRAINT ck_gaze_batches_event_count CHECK (event_count >= 1),
    CONSTRAINT ck_gaze_batches_dropped
        CHECK (dropped_events >= 0 AND dropped_events_total >= dropped_events),
    CONSTRAINT ck_gaze_batches_request_hash CHECK (request_hash ~ '^[0-9a-f]{64}$')
);

-- One row per gaze prediction. Raw viewport coordinates are always kept; widget attribution is
-- an annotation, never a filter. Values derivable elsewhere are not stored: pageX/pageY
-- (x + scroll) and the calibration quality, which comes from the referenced attempt.
CREATE TABLE gaze_events (
    session_id UUID NOT NULL,
    batch_sequence BIGINT NOT NULL,
    event_index INTEGER NOT NULL,
    page_id VARCHAR(128) NOT NULL,
    x DOUBLE PRECISION NOT NULL,
    y DOUBLE PRECISION NOT NULL,
    scroll_x DOUBLE PRECISION NOT NULL,
    scroll_y DOUBLE PRECISION NOT NULL,
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
    CONSTRAINT ck_gaze_events_index CHECK (event_index >= 0),
    CONSTRAINT ck_gaze_events_attribution
        CHECK (attribution IN ('WIDGET', 'NO_CONTENT', 'UNRESOLVED')),
    CONSTRAINT ck_gaze_events_widget CHECK ((attribution = 'WIDGET') = (widget_id IS NOT NULL)),
    CONSTRAINT ck_gaze_events_viewport
        CHECK (viewport_width >= 0 AND viewport_height >= 0 AND device_pixel_ratio > 0)
);

-- The per-session eye-tracking record of UC-37 / FR-51, for UC-13 and the M7 sessions file.
-- Derived on read from the tables above, so nothing in it can disagree with them.
CREATE VIEW eye_tracking_session_quality AS
SELECT
    s.id AS session_id,
    s.study_id,
    st.eye_tracking_enabled,
    (SELECT count(*) FROM calibration_attempts ca WHERE ca.session_id = s.id)
        AS calibration_attempt_count,
    c.attempt_id AS completed_attempt_id,
    s.calibration_completed_at,
    a.tracking_available,
    a.unavailable_reason,
    a.quality AS calibration_quality,
    a.quality_basis AS calibration_quality_basis,
    (SELECT string_agg(r.reason, ';' ORDER BY r.reason)
     FROM calibration_attempt_reasons r
     WHERE r.session_id = c.session_id AND r.attempt_id = c.attempt_id)
        AS calibration_low_quality_reasons,
    g.batches_received AS gaze_batches_received,
    g.last_sequence - g.batches_received AS gaze_batches_missing,
    g.events_received AS gaze_events_received,
    g.dropped_events_total AS gaze_events_dropped
FROM participant_sessions s
JOIN studies st ON st.id = s.study_id
LEFT JOIN calibration_completions c ON c.session_id = s.id
LEFT JOIN calibration_attempts a ON a.session_id = c.session_id AND a.attempt_id = c.attempt_id
CROSS JOIN LATERAL (
    SELECT count(*) AS batches_received,
           coalesce(max(b.sequence), 0) AS last_sequence,
           coalesce(sum(b.event_count), 0) AS events_received,
           coalesce(max(b.dropped_events_total), 0) AS dropped_events_total
    FROM gaze_batches b
    WHERE b.session_id = s.id
) g;

COMMENT ON TABLE gaze_batches IS
    'FR-50 gaze batches keyed by the browser-assigned (session_id, sequence).';
COMMENT ON COLUMN gaze_batches.dropped_events_total IS
    'Events the browser discarded on buffer overflow since collection started; never uploaded.';
COMMENT ON TABLE gaze_events IS
    'FR-50 gaze predictions in viewport CSS pixels with scroll offsets and widget attribution.';
COMMENT ON COLUMN gaze_events.widget_id IS
    'data-component-id (Craft node id) of the element under the gaze point or its nearest ancestor.';
COMMENT ON COLUMN gaze_events.session_elapsed_ms IS
    'Milliseconds since the session clock origin, possibly negative; the ordering key.';
COMMENT ON VIEW eye_tracking_session_quality IS
    'One row per participant session: calibration outcome and gaze completeness, for filtering.';
