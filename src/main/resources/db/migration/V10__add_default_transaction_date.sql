-- V10: Add DEFAULT CURRENT_DATE to transaction_date column.
-- Ensures database-level default for transaction date when not explicitly provided.
ALTER TABLE transactions
    ALTER COLUMN transaction_date SET DEFAULT CURRENT_DATE;