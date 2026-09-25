-- Require every study owner to reference an existing researcher.
-- Existing orphaned owner IDs must be resolved before this migration can succeed.
-- Restrict account deletion while studies still reference the researcher.
ALTER TABLE studies
    ADD CONSTRAINT fk_studies_owner
    FOREIGN KEY (owner_id)
    REFERENCES researchers (id)
    ON DELETE RESTRICT;

COMMENT ON COLUMN studies.owner_id IS
    'Researcher identifier obtained from the authenticated principal; references researchers.id.';
