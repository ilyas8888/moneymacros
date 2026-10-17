-- Identity: one account per user (see the ERD in docs/ARCHITECTURE.md).
-- Emails are stored trimmed and lower-cased by the application; the unique
-- constraint then guarantees one account per address.
CREATE TABLE users (
    id            UUID         PRIMARY KEY,
    email         VARCHAR(320) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);
