-- M6 / FR-49 and FR-51 (UC-35, UC-37): eye-tracking calibration recorded against M5 sessions.
-- One row per finished attempt, whatever its outcome, so unusable attempts stay visible.
-- The browser grades the attempt; the server stores the grade as received and derives nothing.
-- Every row belongs to a participant session and is deleted with it (NFR-07).
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
    CONSTRAINT ck_calibration_attempts_schema_version CHECK (schema_version = 1),
    CONSTRAINT ck_calibration_attempts_attempt_number CHECK (attempt_number BETWEEN 1 AND 10),
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
    -- No residual to grade on: the browser reports NOT_MEASURED with basis NONE, and only then.
    CONSTRAINT ck_calibration_attempts_not_measured
        CHECK ((quality = 'NOT_MEASURED') = (quality_basis = 'NONE')),
    CONSTRAINT ck_calibration_attempts_thresholds
        CHECK (good_fraction >= 0 AND fair_fraction > good_fraction AND fair_fraction <= 1),
    CONSTRAINT ck_calibration_attempts_targets
        CHECK (min_samples_per_target BETWEEN 1 AND 5
            AND calibration_targets_required BETWEEN 0 AND 9
            AND validation_targets_required BETWEEN 0 AND 9
            AND calibration_targets_measured BETWEEN 0 AND calibration_targets_required
            AND validation_targets_measured BETWEEN 0 AND validation_targets_required),
    CONSTRAINT ck_calibration_attempts_medians
        CHECK ((residual_median_px IS NULL OR residual_median_px >= 0)
            AND (validation_median_px IS NULL OR validation_median_px >= 0)),
    CONSTRAINT ck_calibration_attempts_viewport
        CHECK (viewport_width > 0 AND viewport_height > 0 AND device_pixel_ratio > 0),
    CONSTRAINT ck_calibration_attempts_times CHECK (finished_at >= started_at),
    CONSTRAINT ck_calibration_attempts_request_hash CHECK (request_hash ~ '^[0-9a-f]{64}$')
);

-- lowQualityReasons as rows. The browser emits each reason at most once per attempt.
CREATE TABLE calibration_attempt_reasons (
    session_id UUID NOT NULL,
    attempt_id UUID NOT NULL,
    reason VARCHAR(24) NOT NULL,

    CONSTRAINT pk_calibration_attempt_reasons PRIMARY KEY (session_id, attempt_id, reason),
    CONSTRAINT fk_calibration_attempt_reasons_attempt
        FOREIGN KEY (session_id, attempt_id)
            REFERENCES calibration_attempts (session_id, attempt_id) ON DELETE CASCADE,
    CONSTRAINT ck_calibration_attempt_reasons_reason
        CHECK (reason IN ('ABANDONED', 'NOT_MEASURED', 'INSUFFICIENT_SAMPLES',
                          'HIGH_RESIDUAL', 'VIEWPORT_CHANGED', 'ENGINE_UNAVAILABLE'))
);

-- pointResiduals as rows: one per measured target, aggregate prediction in viewport CSS pixels.
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
    CONSTRAINT ck_calibration_point_residuals_index CHECK (point_index BETWEEN 0 AND 8),
    CONSTRAINT ck_calibration_point_residuals_residual CHECK (residual_px >= 0),
    CONSTRAINT ck_calibration_point_residuals_samples CHECK (sample_count BETWEEN 1 AND 5)
);

-- The attempt the participant continued into browsing with: the session's eye-tracking record
-- (UC-37). Written in the transaction that calls M5 completeCalibration, which also sets
-- participant_sessions.calibration_completed_at, so the time is not repeated here.
CREATE TABLE calibration_completions (
    session_id UUID NOT NULL,
    attempt_id UUID NOT NULL,

    CONSTRAINT pk_calibration_completions PRIMARY KEY (session_id),
    CONSTRAINT fk_calibration_completions_attempt
        FOREIGN KEY (session_id, attempt_id)
            REFERENCES calibration_attempts (session_id, attempt_id) ON DELETE CASCADE
);

COMMENT ON TABLE calibration_attempts IS
    'FR-49 calibration attempts, one per finished attempt including abandoned and unavailable ones.';
COMMENT ON COLUMN calibration_attempts.attempt_id IS
    'Browser-generated attempt UUID; with session_id it is the idempotency key of the upload.';
COMMENT ON COLUMN calibration_attempts.attempt_number IS
    'Browser count from 1; restarts after a reload unless previousAttempts is passed, so not unique.';
COMMENT ON COLUMN calibration_attempts.tracking_available IS
    'Whether gaze collection may follow this attempt; the quality grade never decides it.';
COMMENT ON COLUMN calibration_attempts.quality IS
    'Grade computed by the browser from the residual medians; stored as received.';
COMMENT ON COLUMN calibration_attempts.request_hash IS
    'Lowercase SHA-256 digest of the received record; a replay with other content is rejected.';
COMMENT ON TABLE calibration_point_residuals IS
    'Measured calibration and validation targets; targets without samples have no row.';
COMMENT ON TABLE calibration_completions IS
    'The attempt each eye-tracked session continued into browsing with; at most one per session.';
