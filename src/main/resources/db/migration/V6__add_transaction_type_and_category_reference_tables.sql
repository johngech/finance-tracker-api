CREATE TABLE transaction_types (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(20) NOT NULL UNIQUE
);

CREATE TABLE transaction_categories (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE
);

INSERT INTO transaction_types (name) VALUES ('INCOME'), ('EXPENSE');

INSERT INTO transaction_categories (name)
SELECT DISTINCT category FROM transactions WHERE category IS NOT NULL;

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_type
    FOREIGN KEY (type) REFERENCES transaction_types(name);

ALTER TABLE transactions
    ADD CONSTRAINT fk_transactions_category
    FOREIGN KEY (category) REFERENCES transaction_categories(name);
