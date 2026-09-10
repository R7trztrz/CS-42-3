-- Create the study container required by FR-11.
-- UUID identifiers are assigned by the application.
-- Add the owner foreign key in a later migration once the account schema is agreed.
CREATE TABLE studies (
    id UUID NOT NULL,
    owner_id UUID NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lock_version BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT pk_studies PRIMARY KEY (id),
    CONSTRAINT ck_studies_title_not_blank CHECK (title ~ '[^[:space:]]'),
    CONSTRAINT ck_studies_status CHECK (status IN ('DRAFT', 'COLLECTING', 'CLOSED')),
    CONSTRAINT ck_studies_lock_version_nonnegative CHECK (lock_version >= 0)
);

-- Support listing a researcher's studies in creation order.
CREATE INDEX idx_studies_owner_created_at ON studies (owner_id, created_at DESC, id);

COMMENT ON TABLE studies IS 'Study containers owned by researchers.';
COMMENT ON COLUMN studies.owner_id IS 'Researcher identifier obtained from the authenticated principal; account foreign key pending.';
COMMENT ON COLUMN studies.status IS 'Study lifecycle state; allowed transitions are enforced by the application.';
COMMENT ON COLUMN studies.updated_at IS 'Last modification time maintained by the application on every update.';
COMMENT ON COLUMN studies.lock_version IS 'Optimistic locking version managed by JPA, not a published content version.';
