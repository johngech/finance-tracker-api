CREATE TABLE users (
    id            BIGSERIAL       PRIMARY KEY,
    first_name    VARCHAR(255)    NOT NULL,
    last_name     VARCHAR(255)    NOT NULL,
    email         VARCHAR(255)    UNIQUE NOT NULL,
    password_hash VARCHAR(255)    NOT NULL,
    created_at    TIMESTAMPTZ     NOT NULL,
    updated_at    TIMESTAMPTZ     NOT NULL
);

CREATE INDEX idx_users_email ON users (email);
