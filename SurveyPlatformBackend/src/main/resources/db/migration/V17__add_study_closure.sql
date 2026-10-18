-- Preserve legacy CLOSED rows without inventing historical closure times.
ALTER TABLE studies
    ADD COLUMN closed_at TIMESTAMPTZ;

ALTER TABLE studies
    ADD CONSTRAINT ck_studies_closure_metadata
        CHECK (closed_at IS NULL OR status = 'CLOSED');

COMMENT ON COLUMN studies.closed_at IS
    'Time collection was permanently closed; NULL before closure or for legacy rows without a recorded time.';
