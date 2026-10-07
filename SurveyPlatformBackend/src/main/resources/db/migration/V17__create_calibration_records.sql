-- M6 / FR-49: calibration attempts recorded against M5 participant sessions.
--
-- One row per finished attempt, whatever its outcome, so researchers can see
-- abandoned and camera-unavailable attempts as well as usable ones. The
-- server stores the residuals the browser measured and derives no score.
CREATE TABLE calibration_attempts (
    session_id UUID NOT NULL,
    attempt_id UUID NOT NULL,
    schema_version SMALLINT NOT NULL,
    algorithm_version VARCHAR(32) NOT NULL,
    attempt_number SMALLINT NOT NULL,
    outcome VARCHAR(16) NOT NULL,
    tracking_available BOOLEAN NOT NULL,
    unavailable_reason VARCHAR(24),
    quality VARCHAR(16) NOT NULL,
    quality_basis VARCHAR(16) NOT NULL,
    low_quality_reasons JSONB NOT NULL,
    good_fraction DOUBLE PRECISION NOT NULL,
    fair_fraction DOUBLE PRECISION NOT NULL,
    min_samples_per_target SMALLINT NOT NULL,
    calibration_targets_measured SMALLINT NOT NULL,
    calibration_targets_required SMALLINT NOT NULL,
    validation_targets_measured SMALLINT NOT NULL,
    validation_targets_required SMALLINT NOT NULL,
    residual_median_px DOUBLE PRECISION,
    validation_median_px DOUBLE PRECISION,
    viewport_width DOUBLE PRECISION NOT NULL,
    viewport_height DOUBLE PRECISION NOT NULL,
    device_pixel_ratio DOUBLE PRECISION NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ NOT NULL,
    request_hash CHAR(64) NOT NULL,
    received_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_calibration_attempts PRIMARY KEY (session_id, attempt_id),
    CONSTRAINT fk_calibration_attempts_session
        FOREIGN KEY (session_id) REFERENCES participant_sessions (id) ON DELETE CASCADE,
    CONSTRAINT ck_calibration_attempts_schema_version
        CHECK (schema_version = 1),
    CONSTRAINT ck_calibration_attempts_attempt_number
        CHECK (attempt_number BETWEEN 1 AND 10),
    CONSTRAINT ck_calibration_attempts_outcome
        CHECK (outcome IN ('COMPLETED', 'ABANDONED', 'UNAVAILABLE')),
    CONSTRAINT ck_calibration_attempts_unavailable_reason
        CHECK (unavailable_reason IS NULL OR unavailable_reason IN (
            'NO_CAMERA_API', 'NO_CAMERA_DEVICE', 'PERMISSION_DENIED',
            'INIT_FAILED', 'INIT_CANCELLED', 'INIT_TIMEOUT'
        )),
    CONSTRAINT ck_calibration_attempts_tracking_available
        CHECK (NOT tracking_available OR unavailable_reason IS NULL),
    CONSTRAINT ck_calibration_attempts_unavailable_outcome
        CHECK (outcome <> 'UNAVAILABLE'
            OR (NOT tracking_available AND unavailable_reason IS NOT NULL)),
    CONSTRAINT ck_calibration_attempts_quality
        CHECK (quality IN ('GOOD', 'FAIR', 'POOR', 'NOT_MEASURED')),
    CONSTRAINT ck_calibration_attempts_quality_basis
        CHECK (quality_basis IN ('CALIBRATION', 'VALIDATION', 'NONE')),
    CONSTRAINT ck_calibration_attempts_low_quality_reasons
        CHECK (jsonb_typeof(low_quality_reasons) = 'array'),
    CONSTRAINT ck_calibration_attempts_thresholds
        CHECK (good_fraction >= 0 AND fair_fraction > good_fraction AND fair_fraction <= 1),
    CONSTRAINT ck_calibration_attempts_targets
        CHECK (min_samples_per_target >= 1
            AND calibration_targets_measured BETWEEN 0 AND calibration_targets_required
            AND validation_targets_measured BETWEEN 0 AND validation_targets_required
            AND calibration_targets_required BETWEEN 0 AND 9
            AND validation_targets_required BETWEEN 0 AND 9),
    CONSTRAINT ck_calibration_attempts_residuals
        CHECK ((residual_median_px IS NULL OR residual_median_px >= 0)
            AND (validation_median_px IS NULL OR validation_median_px >= 0)),
    CONSTRAINT ck_calibration_attempts_viewport
        CHECK (viewport_width > 0 AND viewport_height > 0 AND device_pixel_ratio > 0),
    CONSTRAINT ck_calibration_attempts_times
        CHECK (finished_at >= started_at),
    CONSTRAINT ck_calibration_attempts_request_hash
        CHECK (request_hash ~ '^[0-9a-f]{64}$')
);

CREATE INDEX idx_calibration_attempts_session_received
    ON calibration_attempts (session_id, received_at, attempt_id);

-- One row per measured target: the aggregate predicted centre and its
-- Euclidean residual in viewport CSS pixels.
CREATE TABLE calibration_point_residuals (
    session_id UUID NOT NULL,
    attempt_id UUID NOT NULL,
    point_kind VARCHAR(16) NOT NULL,
    point_index SMALLINT NOT NULL,
    target_x DOUBLE PRECISION NOT NULL,
    target_y DOUBLE PRECISION NOT NULL,
    predicted_x DOUBLE PRECISION NOT NULL,
    predicted_y DOUBLE PRECISION NOT NULL,
    residual_px DOUBLE PRECISION NOT NULL,
    sample_count SMALLINT NOT NULL,

    CONSTRAINT pk_calibration_point_residuals
        PRIMARY KEY (session_id, attempt_id, point_kind, point_index),
    CONSTRAINT fk_calibration_point_residuals_attempt
        FOREIGN KEY (session_id, attempt_id)
            REFERENCES calibration_attempts (session_id, attempt_id) ON DELETE CASCADE,
    CONSTRAINT ck_calibration_point_residuals_kind
        CHECK (point_kind IN ('CALIBRATION', 'VALIDATION')),
    CONSTRAINT ck_calibration_point_residuals_index
        CHECK (point_index BETWEEN 0 AND 8),
    CONSTRAINT ck_calibration_point_residuals_residual
        CHECK (residual_px >= 0),
    CONSTRAINT ck_calibration_point_residuals_samples
        CHECK (sample_count >= 1)
);

-- The attempt a participant continued with. It is written in the same
-- transaction that moves the M5 session from CALIBRATION to BROWSING.
CREATE TABLE calibration_completions (
    session_id UUID NOT NULL,
    attempt_id UUID NOT NULL,
    completed_at TIMESTAMPTZ NOT NULL,

    CONSTRAINT pk_calibration_completions PRIMARY KEY (session_id),
    CONSTRAINT fk_calibration_completions_attempt
        FOREIGN KEY (session_id, attempt_id)
            REFERENCES calibration_attempts (session_id, attempt_id) ON DELETE CASCADE
);

COMMENT ON TABLE calibration_attempts IS
    'FR-49 calibration attempts, one per finished attempt including abandoned and unavailable ones.';
COMMENT ON COLUMN calibration_attempts.request_hash IS
    'Lowercase SHA-256 digest of the canonical attempt record; a replay with other content is rejected.';
COMMENT ON TABLE calibration_completions IS
    'The calibration attempt each eye-tracked session continued into browsing with.';
