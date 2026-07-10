CREATE TABLE account_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
);

INSERT INTO account_types (name) VALUES ('CHECKING'), ('SAVINGS'), ('INVESTMENT');

CREATE TABLE accounts (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(20) NOT NULL,
    type VARCHAR(20) NOT NULL,
    balance DECIMAL(19, 2) NOT NULL DEFAULT 0.00 CHECK (balance >= 0),
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_accounts_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_accounts_type FOREIGN KEY (type) REFERENCES account_types(name),
    CONSTRAINT uq_accounts_user_name UNIQUE (user_id, name)
);

CREATE INDEX idx_accounts_user_id ON accounts(user_id);
