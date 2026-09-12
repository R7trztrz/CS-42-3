CREATE TABLE researchers (
                             id UUID NOT NULL,
                             email VARCHAR(255) NOT NULL,
                             password_hash VARCHAR(255) NOT NULL,
                             role VARCHAR(20) NOT NULL DEFAULT 'RESEARCHER',
                             created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

                             CONSTRAINT pk_researchers PRIMARY KEY (id),
                             CONSTRAINT uq_researchers_email UNIQUE (email),
                             CONSTRAINT ck_researchers_email_not_blank
                                 CHECK (email ~ '[^[:space:]]'),
    CONSTRAINT ck_researchers_role
        CHECK (role IN ('RESEARCHER'))
);

COMMENT ON TABLE researchers IS 'Researcher accounts for the SurveyPlatform.';
COMMENT ON COLUMN researchers.password_hash IS
    'BCrypt password hash; plaintext passwords must never be stored.';
COMMENT ON COLUMN researchers.role IS
    'Application role used for researcher authorization.';