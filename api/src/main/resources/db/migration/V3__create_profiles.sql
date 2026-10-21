-- Profile: one money profile per user (see the ERD in docs/ARCHITECTURE.md).
-- Created automatically on sign-up with country MA / currency MAD; city,
-- household size and net income stay empty until the user completes the profile.
-- The currency is derived from the country, never chosen freely (MVP: MA -> MAD).
CREATE TABLE profiles (
    id               UUID        PRIMARY KEY,
    user_id          UUID        NOT NULL UNIQUE REFERENCES users (id) ON DELETE CASCADE,
    country          CHAR(2)     NOT NULL,
    currency         CHAR(3)     NOT NULL,
    city             VARCHAR(80),
    household_size   INT,
    net_income_minor BIGINT,
    created_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at       TIMESTAMPTZ NOT NULL DEFAULT now()
);
