-- Create the eye-tracking calibration records required by FR-49.
-- One row per calibration attempt within a participant session; attempts are write-once,
-- so no optimistic locking column is defined.
-- Quality is persisted as raw residuals only. No graded label (good/fair/poor) is stored,
-- because M6 records raw data and leaves derived measures to the researcher (see FR-49).
-- Add the session foreign key in a later migration once the session schema is agreed.
CREATE TABLE calibration_records (
    id UUID NOT NULL,
    session_id UUID NOT NULL,
    attempt_number INT NOT NULL,
    outcome VARCHAR(20) NOT NULL,
    unavailable_reason VARCHAR(32),
    residual_median_px DOUBLE PRECISION,
    validation_median_px DOUBLE PRECISION,
    viewport_width INT NOT NULL,
    viewport_height INT NOT NULL,
    device_pixel_ratio DOUBLE PRECISION NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT pk_calibration_records PRIMARY KEY (id),
    CONSTRAINT uq_calibration_records_session_attempt UNIQUE (session_id, attempt_number),
    CONSTRAINT ck_calibration_records_attempt_positive CHECK (attempt_number >= 1),
    CONSTRAINT ck_calibration_records_outcome
        CHECK (outcome IN ('COMPLETED', 'ABANDONED', 'UNAVAILABLE')),
    CONSTRAINT ck_calibration_records_reason
        CHECK (unavailable_reason IS NULL OR unavailable_reason IN
               ('NO_CAMERA_API', 'NO_CAMERA_DEVICE', 'PERMISSION_DENIED', 'INIT_FAILED')),
    CONSTRAINT ck_calibration_records_reason_matches_outcome
        CHECK ((outcome = 'UNAVAILABLE') = (unavailable_reason IS NOT NULL)),
    CONSTRAINT ck_calibration_records_completed_has_residual
        CHECK (outcome <> 'COMPLETED' OR residual_median_px IS NOT NULL),
    CONSTRAINT ck_calibration_records_unavailable_has_no_residual
        CHECK (outcome <> 'UNAVAILABLE'
               OR (residual_median_px IS NULL AND validation_median_px IS NULL)),
    CONSTRAINT ck_calibration_records_residual_nonnegative
        CHECK ((residual_median_px IS NULL OR residual_median_px >= 0)
               AND (validation_median_px IS NULL OR validation_median_px >= 0)),
    CONSTRAINT ck_calibration_records_viewport_positive
        CHECK (viewport_width > 0 AND viewport_height > 0),
    CONSTRAINT ck_calibration_records_dpr_positive CHECK (device_pixel_ratio > 0),
    CONSTRAINT ck_calibration_records_time_order CHECK (finished_at >= started_at)
);

-- Support looking up a session's attempts in order, and the FR-51 session quality rollup.
CREATE INDEX idx_calibration_records_session ON calibration_records (session_id, attempt_number);

COMMENT ON TABLE calibration_records IS 'Eye-tracking calibration attempts; one row per attempt of a participant session.';
COMMENT ON COLUMN calibration_records.session_id IS 'Participant session identifier; session foreign key pending the M5 schema.';
COMMENT ON COLUMN calibration_records.outcome IS 'COMPLETED: all targets fixated. ABANDONED: started but not finished. UNAVAILABLE: camera unusable, never started.';
COMMENT ON COLUMN calibration_records.residual_median_px IS 'Median in-sample residual over calibration targets; measured on the points used to train the model and therefore optimistic.';
COMMENT ON COLUMN calibration_records.validation_median_px IS 'Median out-of-sample residual over validation targets; null when validation targets are not presented.';
COMMENT ON COLUMN calibration_records.device_pixel_ratio IS 'Device pixel ratio at calibration time; needed to reinterpret CSS-pixel residuals.';

-- Per-target residuals. Stored relationally rather than as a JSON payload because M7 exports
-- these rows to CSV and researchers analyse them per target position.
CREATE TABLE calibration_point_residuals (
    id UUID NOT NULL,
    calibration_record_id UUID NOT NULL,
    point_kind VARCHAR(16) NOT NULL,
    point_index INT NOT NULL,
    target_x DOUBLE PRECISION NOT NULL,
    target_y DOUBLE PRECISION NOT NULL,
    predicted_x DOUBLE PRECISION NOT NULL,
    predicted_y DOUBLE PRECISION NOT NULL,
    residual_px DOUBLE PRECISION NOT NULL,
    sample_count INT NOT NULL,

    CONSTRAINT pk_calibration_point_residuals PRIMARY KEY (id),
    CONSTRAINT fk_calibration_point_residuals_record
        FOREIGN KEY (calibration_record_id) REFERENCES calibration_records (id) ON DELETE CASCADE,
    CONSTRAINT uq_calibration_point_residuals_slot
        UNIQUE (calibration_record_id, point_kind, point_index),
    CONSTRAINT ck_calibration_point_residuals_kind
        CHECK (point_kind IN ('CALIBRATION', 'VALIDATION')),
    CONSTRAINT ck_calibration_point_residuals_index CHECK (point_index >= 0),
    CONSTRAINT ck_calibration_point_residuals_residual CHECK (residual_px >= 0),
    CONSTRAINT ck_calibration_point_residuals_samples CHECK (sample_count >= 1)
);

CREATE INDEX idx_calibration_point_residuals_record
    ON calibration_point_residuals (calibration_record_id, point_kind, point_index);

COMMENT ON TABLE calibration_point_residuals IS 'Per-target residual between the fixation target and the gaze position the model predicted; the raw quality data the client asked to retain.';
COMMENT ON COLUMN calibration_point_residuals.point_kind IS 'CALIBRATION: target used to train the model. VALIDATION: target withheld from training.';
COMMENT ON COLUMN calibration_point_residuals.residual_px IS 'Euclidean distance in CSS pixels between the target and the trimmed-mean predicted position.';
