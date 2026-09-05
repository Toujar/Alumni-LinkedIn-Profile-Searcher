-- V1 - Create alumni table
-- Stores LinkedIn alumni profiles retrieved via the PhantomBuster API.
--
-- Deduplication key: profile_url (LinkedIn canonical URL).
-- A partial unique index is used so that rows where profile_url IS NULL
-- do not conflict with each other (PostgreSQL treats NULL != NULL in unique indexes).
--
-- Note: "current_role" is a PostgreSQL reserved keyword so the column is
-- named "job_title" at the database level; mapped to "currentRole" in the entity.

CREATE TABLE alumni (
    id                BIGSERIAL        PRIMARY KEY,
    name              VARCHAR(255)     NOT NULL,
    job_title         VARCHAR(512),
    university        VARCHAR(255)     NOT NULL,
    location          VARCHAR(255),
    linkedin_headline VARCHAR(512),
    passout_year      INTEGER,
    profile_url       VARCHAR(1024),
    created_at        TIMESTAMP        NOT NULL DEFAULT NOW()
);

-- Partial unique index: enforces uniqueness only on non-NULL profile URLs.
CREATE UNIQUE INDEX uq_alumni_profile_url_notnull
    ON alumni (profile_url)
    WHERE profile_url IS NOT NULL;
