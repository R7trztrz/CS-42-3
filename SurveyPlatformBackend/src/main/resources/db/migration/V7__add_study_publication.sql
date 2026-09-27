-- Existing studies retain their current lifecycle state; only new publication assigns these values.
ALTER TABLE studies
    ADD COLUMN participation_token VARCHAR(43),
    ADD COLUMN published_at TIMESTAMPTZ,
    ADD CONSTRAINT uq_studies_participation_token UNIQUE (participation_token),
    ADD CONSTRAINT ck_studies_publication_pair
        CHECK ((participation_token IS NULL) = (published_at IS NULL));

COMMENT ON COLUMN studies.participation_token IS 'Random URL-safe entry token; not a researcher JWT.';
COMMENT ON COLUMN studies.published_at IS 'Timestamp of the irreversible publication transition.';
